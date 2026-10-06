package com.meshlit.pipeline

import android.content.Context
import android.content.Intent
import android.app.ActivityManager
import android.os.Build
import android.os.PowerManager
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.meshlit.core.common.MeshlitResult
import com.meshlit.core.inference.*
import com.meshlit.core.inference.models.*
import com.meshlit.core.inference.pipeline.*
import com.meshlit.core.trust.EncryptedCredentialStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import java.io.File
import java.math.BigInteger
import java.net.ServerSocket
import java.net.InetAddress
import java.security.*
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal

@Serializable data class PipelinePeer(val host:String,val port:Int,val fingerprint:String,val token:String,val weight:Int=1)
data class PipelineStatus(val worker:Boolean=false,val coordinator:Boolean=false,val starting:Boolean=false,
    val error:String?=null,val log:List<String> = emptyList(),val coordinatorId:String?=null,val memoryBudgetBytes:Long=0,val local:Boolean=false)

/** Explicit opt-in layer offload. Native RPC is private to each app process;
 * only the certificate-pinned, authenticated TLS tunnel listens on the LAN. */
class PipelineHost(private val context:Context,private val inference:InferenceCoordinator,private val scope:CoroutineScope) {
    private val lock=Mutex()
    private val wireJson=Json { ignoreUnknownKeys=true }
    private val credentials by lazy { EncryptedCredentialStore(context,"pipeline-pairing") }
    private val _status=MutableStateFlow(PipelineStatus())
    val status=_status.asStateFlow()
    private var worker:Process?=null
    private var server:Process?=null
    private var workerTunnel:RpcTunnel?=null
    private val bridges=mutableListOf<RpcTunnel>()
    private var keyFile:File?=null
    private var negotiatedPeers:List<PipelinePeer> = emptyList()
    val publicPort=50551
    private fun executable(name:String):File=File(context.applicationInfo.nativeLibraryDir,"libmeshlit_pipeline_$name.so").also {
        require(it.isFile && it.canExecute()) {"Pipeline native runtime is unavailable for this ABI; build and install it first"}
    }
    fun available()=File(context.applicationInfo.nativeLibraryDir,"libmeshlit_pipeline_worker.so").canExecute()
    fun agentControlAllowed()=context.getSharedPreferences("pipeline-policy",0).getBoolean("agent-control",false)
    fun setAgentControlAllowed(allowed:Boolean){context.getSharedPreferences("pipeline-policy",0).edit().putBoolean("agent-control",allowed).apply()}
    fun peers():List<PipelinePeer> = credentials.get("peers")?.let { runCatching{Json.decodeFromString<List<PipelinePeer>>(it)}.getOrNull() }.orEmpty()
    fun savePeers(peers:List<PipelinePeer>) {
        require(peers.size<=8)
        peers.forEach {
            require(it.host.isNotBlank() && it.host.length<=253 && it.host.none(Char::isWhitespace))
            require(it.port in 1024..65535 && it.weight in 1..100)
            require(it.fingerprint.matches(Regex("[a-fA-F0-9]{64}")))
            require(it.token.length in 32..128 && it.token.all { c -> c.code in 33..126 })
        }
        require(peers.map { "${it.host}:${it.port}" }.distinct().size==peers.size) {"Duplicate workers"}
        credentials.put("peers",Json.encodeToString(peers))
    }
    fun pairingToken():String=credentials.get("token") ?: randomToken().also {credentials.put("token",it)}
    private fun randomToken():String=ByteArray(32).also{SecureRandom().nextBytes(it)}.joinToString(""){"%02x".format(it)}
    private fun certificate():Pair<SSLContext,X509Certificate> {
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        val alias="meshlit-pipeline-tls-v1"
        if(!store.containsAlias(alias)) KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC,"AndroidKeyStore").apply {
            initialize(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1")).setDigests(KeyProperties.DIGEST_SHA256)
                .setCertificateSubject(X500Principal("CN=Meshlit layer worker"))
                .setCertificateSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
                .setCertificateNotBefore(Date(System.currentTimeMillis()-86400000))
                .setCertificateNotAfter(Date(System.currentTimeMillis()+5L*365*86400000)).build())
            generateKeyPair()
        }
        val managers=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply{init(store,null)}
        return SSLContext.getInstance("TLS").apply{init(managers.keyManagers,null,null)} to (store.getCertificate(alias) as X509Certificate)
    }
    fun controlTlsContext():SSLContext=certificate().first
    fun fingerprint():String=RpcTunnel.fingerprint(certificate().second)
    private fun port():Int=ServerSocket(0,1,InetAddress.getByName("127.0.0.1")).use {it.localPort}
    private fun running(process:Process):Boolean=try {process.exitValue();false} catch(_:IllegalThreadStateException){true}
    private fun log(line:String) { com.meshlit.observability.AppLoggerFactory.appLogger("pipeline").info("pipeline.native",line.take(1000))
        _status.update { it.copy(log=(it.log+line.take(1000)).takeLast(100)) } }
    private fun launch(name:String,args:List<String>):Process {
        val process=ProcessBuilder(listOf(executable(name).absolutePath)+args).directory(context.filesDir)
            .redirectErrorStream(true).start()
        scope.launch(Dispatchers.IO) { process.inputStream.bufferedReader().useLines { lines -> lines.forEach { log(it) } } }
        return process
    }
    private fun service()=androidx.core.content.ContextCompat.startForegroundService(context,Intent(context,PipelineService::class.java))
    suspend fun startWorker()=lock.withLock { withContext(Dispatchers.IO) {
        if(worker!=null) return@withContext
        _status.update{it.copy(starting=true,error=null)}
        try {
            service()
            val nativePort=port()
            worker=launch("worker",listOf("-H","127.0.0.1","-p","$nativePort","--device","CPU","-t","2"))
            delay(500);check(running(worker!!)){"Native worker exited"}
            workerTunnel=RpcTunnel.server(certificate().first,publicPort,nativePort,pairingToken(),::log,capabilities={Json.encodeToString(offer())},networkAllowed={remote->(context.applicationContext as com.meshlit.MeshlitApplication).meshlitFirewall.decide(remote,null,null,publicPort).allowed})
            _status.update{it.copy(worker=true)}
            val process=worker!!
            scope.launch(Dispatchers.IO) {
                process.waitFor()
                if(worker===process) {workerTunnel?.close();worker=null;_status.update{it.copy(worker=false,error="Native worker stopped")}}
            }
        } catch(cancelled:CancellationException) {stopWorkerInternal();throw cancelled}
        catch(e:Exception) {stopWorkerInternal();_status.update{it.copy(error=e.message)};throw e}
        finally {_status.update{it.copy(starting=false)};stopServiceIfIdle()}
    } }
    private fun stopWorkerInternal(){workerTunnel?.close();workerTunnel=null;val process=worker;worker=null;process?.destroy();_status.update{it.copy(worker=false)}}
    suspend fun stopWorker()=lock.withLock {stopWorkerInternal();stopServiceIfIdle()}
    private fun offer():NodeOffer {
        val memory=ActivityManager.MemoryInfo().also{context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)}
        val id=credentials.get("node-id") ?: java.util.UUID.randomUUID().toString().also{credentials.put("node-id",it)}
        return NodeOffer(id,Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",memory.availMem,context.filesDir.usableSpace,
            ClusterNegotiation.REVISION,workerAllowed=worker!=null,
            thermalStatus=if(Build.VERSION.SDK_INT>=29) context.getSystemService(PowerManager::class.java).currentThermalStatus else 0)
    }
    /** Query only explicitly paired workers. Local model ownership limits current
     * activation to this coordinator; the election contract also supports remote owners. */
    suspend fun negotiate(path:String)=withContext(Dispatchers.IO) {
        val file=File(path);ModelFiles.validateGguf(file)
        val hash=ModelFiles.sha256(file)
        val peerOffers=peers().map { peer -> peer to wireJson.decodeFromString<NodeOffer>(
            RpcTunnel.queryCapabilities(peer.host,peer.port,peer.fingerprint,peer.token)).copy(observedAtMs=System.currentTimeMillis()) }
        val local=offer().copy(coordinatorAllowed=true,coordinatorModelSha256=hash,workerAllowed=false)
        val plan=ClusterNegotiation.plan(peerOffers.map{it.second}+local,hash,file.length())
        negotiatedPeers=plan.workers.mapIndexed { index,node -> peerOffers.first{it.second.nodeId==node.nodeId}.first.copy(weight=plan.weights[index]) }
        _status.update{it.copy(coordinatorId=plan.coordinatorId,memoryBudgetBytes=plan.memoryBudgetBytes,error=null)}
        log("Negotiated ${plan.workers.size} workers; capacity estimate ${plan.memoryBudgetBytes} bytes")
        negotiatedPeers
    }
    suspend fun startPipeline(path:String,contextSize:Int=2048,keyCacheType:String="f16") {
        val paired=negotiate(path)
        require(paired.size>=2){"Pair at least two approved workers"}
        startNative(path,ModelRuntimeOptions(LocalModelBackend.NATIVE_LOCAL,contextSize,keyCacheType),paired)
    }
    suspend fun startLocal(path:String,options:ModelRuntimeOptions) {
        require(options.backend==LocalModelBackend.NATIVE_LOCAL)
        startNative(path,options,emptyList())
    }
    private suspend fun startNative(path:String,options:ModelRuntimeOptions,paired:List<PipelinePeer>) {
        options.validate(GgufMetadata.read(File(path)).maxContext)
        val contextSize=options.contextSize
        ModelFiles.validateGguf(File(path))
        _status.update{it.copy(starting=true,error=null)}
        try {
            val result=inference.loadExternalEngine(path,contextSize) {
                lock.withLock { withContext(Dispatchers.IO) {
                    stopServerInternal();service()
                    try {
                        paired.forEach { bridges+=RpcTunnel.client(it.host,it.port,it.fingerprint,it.token,::log) }
                        val endpoints=bridges.joinToString(","){"127.0.0.1:${it.port}"}
                        val httpPort=port();val token=randomToken()
                        keyFile=File(context.filesDir,"pipeline-api-key").apply{writeText(token);setReadable(false,false);setReadable(true,true)}
                        com.meshlit.power.PowerRepository(context).loadBlockReason()?.let{error(it)}
                        val args=mutableListOf("-m",path,"-c","$contextSize","-t","${com.meshlit.models.AccelerationPreferences(context).effectiveThreads()}","--parallel","1",
                            "--host","127.0.0.1","--port","$httpPort","--api-key-file",keyFile!!.absolutePath,
                            "--cache-type-k",options.keyCacheType,"--cache-type-v","f16",
                            "--no-webui","--no-warmup","--fit","off")
                        if(paired.isEmpty()) args+=listOf("--device","CPU","-ngl","0")
                        else args+=listOf("--rpc",endpoints,"--device",paired.indices.joinToString(","){"RPC$it"},
                            "--split-mode","layer","--tensor-split",paired.joinToString(","){"${it.weight}"},"-ngl","99")
                        server=launch("server",args)
                        val client=OkHttpClient.Builder().callTimeout(2,java.util.concurrent.TimeUnit.SECONDS).build()
                        withTimeout(180000) {
                            while(true) {
                                ensureActive();check(running(server!!)){"Native pipeline server exited; see runtime log"}
                                val ready=runCatching {client.newCall(Request.Builder().url("http://127.0.0.1:$httpPort/health")
                                    .header("Authorization","Bearer $token").build()).execute().use{it.code==200}}.getOrDefault(false)
                                if(ready) break
                                delay(500)
                            }
                        }
                        _status.update{it.copy(coordinator=paired.isNotEmpty(),local=paired.isEmpty())}
                        val selected=RpcPipelineEngine(httpPort,token,path,if(paired.isEmpty()) "llama-native-local" else "llama-rpc-layer") {lock.withLock {stopServerInternal();stopServiceIfIdle()}}
                        val process=server!!
                        scope.launch(Dispatchers.IO) {
                            process.waitFor()
                            if(server===process) {
                                _status.update{it.copy(coordinator=false,local=false,error="Native model process stopped; load again")}
                                inference.reportExternalFailure(selected,"Native pipeline stopped; recovery replay is not yet implemented")
                            }
                        }
                        selected
                    } catch(e:Throwable) {stopServerInternal();stopServiceIfIdle();throw e}
                } }
            }
            if(result is MeshlitResult.Failure) error(result.error.tag)
        } catch(cancelled:CancellationException){throw cancelled}
        catch(e:Exception){_status.update{it.copy(error=e.message)};throw e}
        finally {_status.update{it.copy(starting=false)};stopServiceIfIdle()}
    }
    private fun stopServerInternal(){val process=server;server=null;process?.destroy();bridges.forEach{it.close()};bridges.clear();keyFile?.delete();keyFile=null;_status.update{it.copy(coordinator=false,local=false)}}
    suspend fun stopPipeline(){inference.unloadModel();lock.withLock{stopServerInternal();stopServiceIfIdle()}}
    suspend fun stopAll(){inference.cancel();stopPipeline();stopWorker()}
    fun active()=worker!=null || server!=null || status.value.starting
    private fun stopServiceIfIdle(){if(!active()) context.stopService(Intent(context,PipelineService::class.java))}
}
