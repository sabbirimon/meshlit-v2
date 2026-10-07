package com.meshlit.security

import android.content.Context
import com.meshlit.core.mcp.security.LabGate
import com.meshlit.core.trust.EncryptedCredentialStore
import com.meshlit.sandbox.RuntimeHost
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

@Serializable data class PackageDelegation(val names:List<String> = emptyList(),val expiresAtMs:Long=0,val environmentId:String="")
/** Automatic agent install/uninstall is limited to owner-listed pip packages in the ready VM.
 * Distro root operations and arbitrary source edits remain human-only.
 */
class LabPackages(context:Context,private val runtime:RuntimeHost) {
    private val store=EncryptedCredentialStore(context,"lab-package-grants")
    private val json=Json {ignoreUnknownKeys=true}
    fun delegation():PackageDelegation=store.get("delegation")?.let {runCatching {json.decodeFromString<PackageDelegation>(it)}.getOrNull()} ?: PackageDelegation()
    @Synchronized fun delegate(names:List<String>) {require(names.size<=16 && names.all {it.matches(Regex("[A-Za-z0-9][A-Za-z0-9_.+-]{0,99}"))});store.putCommitted("delegation",json.encodeToString(PackageDelegation(names,System.currentTimeMillis()+3600000,if(names.isEmpty()) "" else runtime.labIdentity())))}
    suspend fun run(action:String,manager:String,kind:String,source:String,sha256:String,guestRoot:Boolean=false,agent:Boolean=false):JsonObject {
        LabGate.requireReady(runtime.config().mode.name,runtime.vm.state.name,agent,runtime.allowAgentVm())
        require(action in setOf("list","install","uninstall") && manager in setOf("pip","apt","apk","dnf","pacman") && kind in setOf("repo","file","web"))
        require(source.length<=2000 && sha256.length<=64 && source.none {it=='\u0000' || it=='\n' || it=='\r'})
        val grant=delegation()
        if(agent) require(grant.names.isNotEmpty() && grant.environmentId==runtime.labIdentity() && !guestRoot && manager=="pip" && kind=="repo" && grant.expiresAtMs>System.currentTimeMillis() && (action=="list" || source in grant.names)) {"Agent package grant missing or expired"}
        val args=mutableListOf("timeout","-k","5","50","/usr/local/bin/meshlit-packages",action,"--manager",manager,"--kind",kind,"--source",source,"--sha256",sha256)
        if(guestRoot) args.add("--allow-guest-root")
        val result=runtime.executeGuest(args)
        require(!result.timedOut && !result.truncated && result.exitCode==0) {"Package operation failed or outcome uncertain"}
        if(agent) require(delegation()==grant && grant.environmentId==runtime.labIdentity() && grant.expiresAtMs>System.currentTimeMillis()) {"Package delegation revoked; inspect guest state"}
        val report=json.parseToJsonElement(result.stdout).jsonObject
        require(report["status"]?.jsonPrimitive?.content=="completed")
        store.putCommitted("last-result",report.toString());return report
    }
}
