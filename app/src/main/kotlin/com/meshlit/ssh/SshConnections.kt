package com.meshlit.ssh
import android.content.Context
import com.meshlit.core.ssh.*
import com.meshlit.core.trust.EncryptedCredentialStore
import com.meshlit.observability.AppLoggerFactory.appLogger as logger
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
class SshConnections(context:Context,private val vault:com.meshlit.cloud.CloudManagement) {
    private val operations = com.meshlit.operations.OperationsControl.get(context).gate
    private val store=EncryptedCredentialStore(context,"ssh-connections")
    private val json=Json{ignoreUnknownKeys=true};private val _connections=MutableStateFlow(runCatching{store.get("connections")?.let{json.decodeFromString<List<SshConnection>>(it)}}.getOrNull().orEmpty())
    val connections=_connections.asStateFlow()
    private val jobs=mutableMapOf<kotlinx.coroutines.Job,String>()
    @Synchronized fun save(connection:SshConnection,password:String?=null,key:String?=null){connection.validate();require(_connections.value.size<32 || _connections.value.any{it.id==connection.id});password?.let{require(it.length<=4096);store.put("password-${connection.id}",it);store.remove("key-${connection.id}")};key?.let{require(it.length<=65536);store.put("key-${connection.id}",it);store.remove("password-${connection.id}")};val updated=_connections.value.filterNot{it.id==connection.id}+connection;jobs.filterValues{it==connection.id}.keys.toList().forEach{it.cancel(kotlinx.coroutines.CancellationException("SSH profile changed"))};if(!connection.agentAllowed || _connections.value.firstOrNull{it.id==connection.id}?.agentNodeActions?.let{old->!connection.agentNodeActions.containsAll(old)}==true) _connections.value=updated;store.putCommitted("connections",json.encodeToString(updated));_connections.value=updated}
    @Synchronized fun remove(id:String){val updated=_connections.value.filterNot{it.id==id};_connections.value=updated;jobs.filterValues{it==id}.keys.toList().forEach{it.cancel(kotlinx.coroutines.CancellationException("SSH host revoked"))};store.putCommitted("connections",json.encodeToString(updated));store.remove("password-$id");store.remove("key-$id");_connections.value=updated}
    suspend fun execute(id:String,command:String,agent:Boolean=false):SshCommandResult = operations.run(com.meshlit.core.common.control.ManagedFeature.SSH,agent) { check(!agent){"Agents must use scoped node requests"};executeManaged(id,command,agent) }
    suspend fun executeNode(id:String,request:NodeSshRequest):SshCommandResult = operations.run(com.meshlit.core.common.control.ManagedFeature.SSH,true) {
        request.validate()
        val config=_connections.value.firstOrNull{it.id==id} ?: error("SSH connection not found")
        check(config.agentAllowed && request.action in config.agentNodeActions && request.action!=NodeSshAction.APP_EXEC) { "This node action is not delegated" }
        executeManaged(id,json.encodeToString(request),true,config)
    }
    private suspend fun executeManaged(id:String,command:String,agent:Boolean=false,expected:SshConnection?=null):SshCommandResult {
        val config=_connections.value.firstOrNull{it.id==id} ?: error("SSH connection not found")
        check(expected==null || expected==config){"SSH profile changed"}
        require(!agent || config.agentAllowed){"Agent SSH access is not enabled for this host"}
        val job=kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job] ?: error("Missing SSH job")
        synchronized(this) { check(_connections.value.firstOrNull{it.id==id}==config){"SSH profile changed"};jobs[job]=id }
        val log=logger("SshConnections");log.info("ssh.command.started","Owner-approved SSH command started",mapOf("connectionId" to id))
        try {
        val env=config.credentialEnvironmentId?.let{vault.serviceCredentials(it,com.meshlit.core.cloudmcp.management.EnvironmentPurpose.SSH,config.host,if(agent) com.meshlit.core.cloudmcp.management.CloudActor.AGENT else com.meshlit.core.cloudmcp.management.CloudActor.HUMAN)}
        try {val result=SshClient().execute(config,if(env==null) store.get("password-$id") else env["SSH_PASSWORD"],if(env==null) store.get("key-$id") else env["SSH_PRIVATE_KEY"],command);log.info("ssh.command.finished","SSH command completed",mapOf("connectionId" to id,"exitCode" to result.exitCode.toString()));return result}
        catch(e:kotlinx.coroutines.CancellationException){log.info("ssh.command.cancelled","SSH session cancelled",mapOf("connectionId" to id));throw e}
        catch(e:Exception){log.warn("ssh.command.failed","SSH request failed",mapOf("connectionId" to id,"type" to e.javaClass.simpleName));throw e}
        } finally { synchronized(this) { jobs.remove(job) } }
    }
}
