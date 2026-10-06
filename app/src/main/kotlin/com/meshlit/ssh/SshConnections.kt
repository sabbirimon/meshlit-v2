package com.meshlit.ssh
import android.content.Context
import com.meshlit.core.ssh.*
import com.meshlit.core.trust.EncryptedCredentialStore
import com.meshlit.observability.AppLoggerFactory.appLogger as logger
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class SshConnections(context:Context) {
    private val store=EncryptedCredentialStore(context,"ssh-connections")
    private val json=Json{ignoreUnknownKeys=true};private val _connections=MutableStateFlow(runCatching{store.get("connections")?.let{json.decodeFromString<List<SshConnection>>(it)}}.getOrNull().orEmpty())
    val connections=_connections.asStateFlow()
    @Synchronized fun save(connection:SshConnection,password:String?=null,key:String?=null){connection.validate();require(_connections.value.size<32 || _connections.value.any{it.id==connection.id});password?.let{require(it.length<=4096);store.put("password-${connection.id}",it);store.remove("key-${connection.id}")};key?.let{require(it.length<=65536);store.put("key-${connection.id}",it);store.remove("password-${connection.id}")};val updated=_connections.value.filterNot{it.id==connection.id}+connection;store.put("connections",json.encodeToString(updated));_connections.value=updated}
    @Synchronized fun remove(id:String){val updated=_connections.value.filterNot{it.id==id};store.put("connections",json.encodeToString(updated));store.remove("password-$id");store.remove("key-$id");_connections.value=updated}
    suspend fun execute(id:String,command:String,agent:Boolean=false):SshCommandResult {
        val config=_connections.value.firstOrNull{it.id==id} ?: error("SSH connection not found")
        require(!agent || config.agentAllowed){"Agent SSH access is not enabled for this host"}
        val log=logger("SshConnections");log.info("ssh.command.started","Owner-approved SSH command started",mapOf("connectionId" to id))
        try {val result=SshClient().execute(config,store.get("password-$id"),store.get("key-$id"),command);log.info("ssh.command.finished","SSH command completed",mapOf("connectionId" to id,"exitCode" to result.exitCode.toString()));return result}
        catch(e:kotlinx.coroutines.CancellationException){log.info("ssh.command.cancelled","SSH session cancelled",mapOf("connectionId" to id));throw e}
        catch(e:Exception){log.warn("ssh.command.failed","SSH request failed",mapOf("connectionId" to id,"type" to e.javaClass.simpleName));throw e}
    }
}
