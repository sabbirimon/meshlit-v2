package com.meshlit.chat

import com.meshlit.core.common.MeshlitResult
import com.meshlit.core.common.control.ManagedFeature
import com.meshlit.core.common.control.OperationGate
import com.meshlit.core.inference.*
import com.meshlit.core.mcp.*

/** Chat grants expose a small tool list; each existing tool retains its own
 * saved delegation, OS permission and operation checks. No generic shell tool. */
class LocalChatTools(private val inference:InferenceCoordinator,private val gate:OperationGate,private val registry:()->McpToolRegistry,private val search:()->com.meshlit.search.AppSearchService) {
    suspend fun run(prompt:String,options:ChatOptions,status:(String)->Unit):LocalToolAnswer=gate.run(ManagedFeature.AUTOMATION,true) {
        options.validate();com.meshlit.BuildProfile.requireChat(options)
        val model=inference.loadedModel() ?: error("Load a local model before enabling tools")
        val names=buildSet {
            if(options.nodeTools) addAll(listOf("vm_status","vm_start","vm_wait","vm_stop","vm_exec","ssh_nodes","ssh_node_request"))
            if(options.memoryTools) add("personal_memory")
            if(options.webTools) addAll(listOf("crawl_url","web_search","search_access"))
            if(options.localSearchTools) add("app_search")
            if(options.phoneTools) addAll(listOf("android_control_status","android_control","android_package_status","android_package_request"))
        }
        val tools=registry().list().filter{it.name in names && it.origin==McpToolSpec.Origin.BuiltIn}
        check(tools.isNotEmpty()){"No selected built-in tools are available"}
        LocalToolLoop(complete={planningPrompt->
            gate.run(ManagedFeature.INFERENCE,true) {
                val result=inference.infer(InferenceRequest(planningPrompt,maxTokens=minOf(options.maxTokens,1024),temperature=0f,
                    expectedModelPath=model.modelPath,onDeviceOnly=true,publishEvents=false,onToken={}))
                when(result){is MeshlitResult.Success->result.value.finalText;is MeshlitResult.Failure->error("Local tool planning failed: ${result.error.tag}")}
            }
        },invoke={request->if(request.name=="crawl_url") search().access.web(true){registry().invoke(request)} else registry().invoke(request)},checkAllowed={gate.requireAllowed(ManagedFeature.AUTOMATION,true)}).run(prompt,tools,status)
    }
}
