package com.meshlit.pipeline
import com.meshlit.core.mcp.*
import com.meshlit.models.ModelLibrary
import kotlinx.serialization.json.*

/** Humans and agents use the same host/repository, with saved delegation scopes. */
class PipelineMcpTools(private val host:PipelineHost,private val library:ModelLibrary) {
    private fun permission():McpToolResult.Error?=if(host.agentControlAllowed()) null else
        McpToolResult.Error(McpToolResult.ErrorCode.PERMISSION_DENIED,"Enable agent cluster control in Network and pairing first")
    private suspend fun model(args:JsonElement):String? {
        library.ready.await()
        val id=(args as? JsonObject)?.get("model_id")?.jsonPrimitive?.contentOrNull
        return library.models.value.firstOrNull{it.id==id && it.installed}?.path
    }
    private val schema=buildJsonObject{put("type","object");put("properties",buildJsonObject{
        put("model_id",buildJsonObject{put("type","string")})});put("required",buildJsonArray{add("model_id")})}
    fun specs():List<McpToolSpec> = listOf(
        McpToolSpec(name="cluster_runtime_status",description="Inspect actual native layer runtime state and saved delegation; does not expose pairing secrets."){
            McpToolResult.Json(buildJsonObject{
                put("worker",host.status.value.worker);put("coordinator",host.status.value.coordinator)
                put("starting",host.status.value.starting);put("native_available",host.available())
                put("agent_control",host.agentControlAllowed());put("paired_workers",host.peers().size)
                put("estimated_memory_bytes",host.status.value.memoryBudgetBytes)
                host.status.value.error?.let{put("error",it)}
                put("remote_failover_implemented",false);put("durable_replication_implemented",false)
            })
        },
        McpToolSpec(name="model_library",description="List known model IDs, installed state and transfer status; no credentials or automatic transfers."){
            library.ready.await();McpToolResult.Json(buildJsonObject{put("models",buildJsonArray{
                library.models.value.forEach{entry -> add(buildJsonObject{put("id",entry.id);put("name",entry.name)
                    put("installed",entry.installed);put("phase",entry.phase);put("bytes",entry.sizeBytes)})}
            })})
        },
        McpToolSpec(name="cluster_layer_plan",description="Query explicitly paired workers and negotiate a memory-weighted layer plan for an installed model. Requires saved agent cluster control.",inputSchema=schema){args ->
            permission()?.let{return@McpToolSpec it}
            val path=model(args) ?: return@McpToolSpec McpToolResult.Error(McpToolResult.ErrorCode.INVALID_ARGS,"Choose an installed model_id")
            val peers=host.negotiate(path)
            McpToolResult.Json(buildJsonObject{put("workers",peers.size);put("memory_estimate_bytes",host.status.value.memoryBudgetBytes)})
        },
        McpToolSpec(name="cluster_layer_start",description="Unload local model and start actual native layer execution using approved workers and a managed installed model. Requires delegated cluster control.",inputSchema=schema){args ->
            permission()?.let{return@McpToolSpec it}
            val path=model(args) ?: return@McpToolSpec McpToolResult.Error(McpToolResult.ErrorCode.INVALID_ARGS,"Choose an installed model_id")
            host.startPipeline(path)
            McpToolResult.Json(buildJsonObject{put("ready",host.status.value.coordinator)})
        },
        McpToolSpec(name="cluster_worker_start",description="Start an authenticated LAN layer worker only with persisted user delegation."){
            permission()?.let{return@McpToolSpec it};host.startWorker()
            McpToolResult.Json(buildJsonObject{put("worker",host.status.value.worker)})
        },
        McpToolSpec(name="cluster_runtime_stop",description="Stop native layer worker and coordinator under saved user delegation."){
            permission()?.let{return@McpToolSpec it};host.stopAll()
            McpToolResult.Json(buildJsonObject{put("active",host.active())})
        }
    )
}
