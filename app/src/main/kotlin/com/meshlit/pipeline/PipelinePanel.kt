package com.meshlit.pipeline
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlit.di.koinInject
import com.meshlit.models.ModelLibrary
import com.meshlit.ui.theme.ChatTokens as T
import kotlinx.coroutines.*

@Composable fun PipelinePanel() {
    val host=koinInject<PipelineHost>();val library=koinInject<ModelLibrary>()
    val status by host.status.collectAsStateWithLifecycle();val models by library.models.collectAsStateWithLifecycle()
    val context=androidx.compose.ui.platform.LocalContext.current
    var scanned by remember{mutableStateOf<PipelinePeer?>(null)}
    var qr by remember{mutableStateOf<android.graphics.Bitmap?>(null)}
    val scope=rememberCoroutineScope();val clipboard=LocalClipboardManager.current
    var peers by remember{mutableStateOf<List<PipelinePeer>>(emptyList())}
    var add by remember{mutableStateOf(false)};var address by remember{mutableStateOf("")}
    var fingerprint by remember{mutableStateOf("")};var token by remember{mutableStateOf("")}
    var localPairing by remember{mutableStateOf<String?>(null)};var error by remember{mutableStateOf<String?>(null)}
    var delegated by remember{mutableStateOf(host.agentControlAllowed())}
    var selected by remember{mutableStateOf("")}
    LaunchedEffect(host){withContext(Dispatchers.IO){peers=host.peers()}}
    fun action(block:suspend()->Unit){scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}}}
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(T.large),verticalArrangement=Arrangement.spacedBy(T.small)) {
        Text("Layer pipeline",style=MaterialTheme.typography.titleMedium)
        Text("Experimental native layer execution on approved workers. Off by default. Use trusted devices on your private network.")
        Text("The coordinator keeps the complete GGUF on disk and uploads tensors to workers. This is layer offload; large-model and physical-phone verification are still required.",style=MaterialTheme.typography.bodySmall)
        if(!host.available()) Text("Native pipeline runtime is not packaged for this device ABI.")
        Text("Worker: ${if(status.worker) "Listening with TLS on ${host.publicPort}" else "Off"} · Coordinator: ${if(status.coordinator) "Active" else "Off"}")
        Row(horizontalArrangement=Arrangement.spacedBy(T.small)) {
            OutlinedButton(enabled=host.available() && !status.starting,onClick={action{if(status.worker) host.stopWorker() else host.startWorker()}}) {
                Text(if(status.worker) "Stop worker" else "Start worker") }
            TextButton(onClick={action{localPairing=withContext(Dispatchers.IO){
                val address=(context.applicationContext as com.meshlit.MeshlitApplication).localIpAddress
                LayerPairingQr.encode(PipelinePeer(address,host.publicPort,host.fingerprint(),host.pairingToken()))
            }}}){Text("Pair this device")}
        }
        Row {Text("Allow agents to control the paired cluster",Modifier.weight(1f));Switch(delegated,{delegated=it;host.setAgentControlAllowed(it)})}
        Text("${peers.size} approved workers")
        if(status.memoryBudgetBytes>0) Text("Negotiated memory estimate: ${com.meshlit.ui.modern.formatBytes(status.memoryBudgetBytes)}")
        peers.forEachIndexed { index,peer -> Row {
            Text("${peer.host}:${peer.port}",Modifier.weight(1f))
            TextButton(enabled=!status.coordinator && !status.starting,onClick={action{val updated=peers.filterIndexed{i,_->i!=index};withContext(Dispatchers.IO){host.savePeers(updated)};peers=updated}}){Text("Remove")}
        } }
        OutlinedButton(enabled=!status.coordinator && !status.starting,onClick={action{
            when(val scan=com.meshlit.devices.QrScanner.scan(context)){
                is com.meshlit.devices.QrScanner.ScanResult.Success -> scanned=LayerPairingQr.decode(scan.rawValue)
                com.meshlit.devices.QrScanner.ScanResult.Cancelled ->Unit
                else ->error="QR scanner unavailable; use manual pairing"
            }
        }}){Text("Scan worker QR")}
        OutlinedButton(enabled=!status.coordinator && !status.starting,onClick={add=true}){Text("Add approved worker")}
        if(status.local) Text("Native local inference is active; no remote workers are being used.")
        if(status.starting) LinearProgressIndicator(Modifier.fillMaxWidth())
        if(status.coordinator) Button(onClick={action{host.stopPipeline()}}){Text("Stop pipeline")}
        else {
            models.filter{it.installed}.forEach { model ->
                Row {RadioButton(selected==model.path,{selected=model.path});Text(model.name,Modifier.padding(top=T.medium))}
            }
            Button(enabled=host.available() && peers.size>=2 && selected.isNotBlank() && !status.starting,
                onClick={action{host.startPipeline(selected,models.first{it.path==selected}.runtimeOptions.contextSize,models.first{it.path==selected}.runtimeOptions.keyCacheType)}}){Text("Negotiate and load cluster")}
        }
        (error ?: status.error)?.let{Text(it,color=MaterialTheme.colorScheme.error)}
        if(status.log.isNotEmpty()) Text(status.log.takeLast(4).joinToString("\n"),style=MaterialTheme.typography.bodySmall)
    } }
    if(add) AlertDialog(onDismissRequest={add=false;token=""},title={Text("Approve worker")},text={Column {
        Text("Verify the fingerprint and token on the worker device before pairing.")
        OutlinedTextField(address,{address=it},label={Text("Host address (TLS port 50551)")})
        OutlinedTextField(fingerprint,{fingerprint=it},label={Text("Certificate SHA-256")})
        OutlinedTextField(token,{token=it},label={Text("Pairing token")},visualTransformation=PasswordVisualTransformation())
    }},confirmButton={TextButton(onClick={action{val updated=peers+PipelinePeer(address.trim(),50551,fingerprint.trim(),token.trim());withContext(Dispatchers.IO){host.savePeers(updated)};peers=updated;add=false;token=""}}){Text("Approve")}},dismissButton={TextButton(onClick={add=false;token=""}){Text("Cancel")}})
    scanned?.let{peer ->AlertDialog(onDismissRequest={scanned=null},title={Text("Approve scanned worker")},
        text={Text("${peer.host}:${peer.port}\nCertificate SHA-256: ${peer.fingerprint}\nVerify this device before approving.")},
        confirmButton={TextButton(onClick={action{withContext(Dispatchers.IO){com.meshlit.core.inference.pipeline.RpcTunnel.queryCapabilities(peer.host,peer.port,peer.fingerprint,peer.token);host.savePeers(peers+peer)};peers=host.peers();scanned=null}}){Text("Verify and approve")}},
        dismissButton={TextButton(onClick={scanned=null}){Text("Cancel")}})}
    LaunchedEffect(localPairing){qr=localPairing?.let{value ->withContext(Dispatchers.Default){
        val matrix=com.google.zxing.MultiFormatWriter().encode(value,com.google.zxing.BarcodeFormat.QR_CODE,512,512)
        android.graphics.Bitmap.createBitmap(512,512,android.graphics.Bitmap.Config.ARGB_8888).apply{
            val pixels=IntArray(512*512){i ->if(matrix[i%512,i/512]) android.graphics.Color.BLACK else android.graphics.Color.WHITE};setPixels(pixels,0,512,0,0,512,512)
        }
    }}}
    localPairing?.let { pairing -> AlertDialog(onDismissRequest={localPairing=null},title={Text("Pairing details")},text={Column{qr?.let{androidx.compose.foundation.Image(it.asImageBitmap(),"Private worker pairing QR",Modifier.fillMaxWidth())};Text("Contains your private pairing token. Share only with devices you approve.")}},
        confirmButton={TextButton(onClick={clipboard.setText(AnnotatedString(pairing));localPairing=null}){Text("Copy")}},
        dismissButton={TextButton(onClick={localPairing=null}){Text("Close")}}) }
}
