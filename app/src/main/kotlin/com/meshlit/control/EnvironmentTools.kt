package com.meshlit.control
import android.content.Context
import com.meshlit.core.mcp.*
import com.meshlit.power.*
import com.meshlit.devices.ExternalDevices
import com.meshlit.providers.OnlineProviders
import com.meshlit.ssh.SshConnections
import kotlinx.serialization.json.*
class EnvironmentTools(private val context:Context,private val backend:AgentBackend,private val providers:OnlineProviders,private val ssh:SshConnections) {
    private fun schema(properties:Map<String,JsonElement>,required:List<String> = emptyList())=buildJsonObject{put("type","object");put("properties",JsonObject(properties));put("required",JsonArray(required.map(::JsonPrimitive)));put("additionalProperties",false)}
    private fun string()=buildJsonObject{put("type","string")}
    fun specs()=listOf(
        McpToolSpec("power_status","Read observed whole-device battery metrics and saved workload policy. Null values mean unavailable; watts are battery-side, not app-only."){
            val power=PowerRepository(context);McpToolResult.Json(buildJsonObject{put("reading",Json.encodeToJsonElement(power.reading()));put("policy",Json.encodeToJsonElement(power.policy()));put("downloadBlockedReason",power.downloadBlockReason()?.let(::JsonPrimitive) ?: JsonNull)})
        },
        McpToolSpec("external_device_status","Read actual USB devices, storage volumes and granted storage trees. Discovery does not supply accelerator drivers."){
            McpToolResult.Json(Json.encodeToJsonElement(ExternalDevices(context).status()))
        },
        McpToolSpec("online_profiles_list","List saved online model profiles without credentials. Agent use needs Models delegation and agentAllowed on the chosen profile."){
            McpToolResult.Json(buildJsonObject{put("profiles",Json.encodeToJsonElement(providers.profiles.value))})
        },
        McpToolSpec("ssh_connections_list","List configured SSH connections without secrets. Connections are not automatically approved compute workers."){
            McpToolResult.Json(buildJsonObject{put("connections",Json.encodeToJsonElement(ssh.connections.value))})
        },
        McpToolSpec("ssh_command_execute","Execute a command on a saved SSH host. Requires SSH delegation and the per-host agent opt-in; mandatory host-key pin, 60s and 64KiB output limits. No automatic retry.",schema(mapOf("connectionId" to string(),"command" to string()),listOf("connectionId","command"))){args->
            require(backend.delegated(AgentBackend.Scope.SSH)){"Enable human-controlled SSH delegation first"}
            val values=args as JsonObject;McpToolResult.Json(Json.encodeToJsonElement(ssh.execute(values["connectionId"]!!.jsonPrimitive.content,values["command"]!!.jsonPrimitive.content,agent=true)))
        }
    )
}
