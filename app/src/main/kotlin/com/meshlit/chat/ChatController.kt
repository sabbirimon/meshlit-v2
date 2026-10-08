package com.meshlit.chat

import android.content.Context
import com.meshlit.core.inference.*
import com.meshlit.core.common.MeshlitResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable data class ChatOptions(val onlineProfileId:String?=null,val systemPrompt:String="",val maxTokens:Int=1024,val temperature:Float=0.7f,val historyMessages:Int=10,val routeId:String?=null,val routeScenario:String="general",val webTools:Boolean=false,val phoneTools:Boolean=false,val memoryTools:Boolean=false,val showTokenStats:Boolean=true,val localSearchTools:Boolean=false,val outputBudgetMode:OutputBudgetMode=OutputBudgetMode.MANUAL,val outputTargetSeconds:Int=30,val nodeTools:Boolean=false) {
    val usesLocalTools get()=webTools || phoneTools || memoryTools || localSearchTools || nodeTools
    fun validate(){require(!nodeTools || (!webTools && !phoneTools)){"Choose node tools separately from web/phone tools"};require(outputTargetSeconds in 5..120);require(!usesLocalTools || (onlineProfileId==null && routeId==null)){"Chat tools currently require an on-device model"};require(routeId==null || routeId=="__auto__" || routeId.matches(Regex("[A-Za-z0-9_-]{1,64}")));require(routeScenario.matches(Regex("[a-z0-9_-]{1,40}")));require(routeId==null || (onlineProfileId==null && maxTokens<=1024));require(systemPrompt.length<=4000 && maxTokens in 1..2048 && temperature.isFinite() && temperature in 0f..2f && historyMessages in 0..20)}
}
@Serializable data class ChatMessage(val id:String=UUID.randomUUID().toString(),val role:String,val text:String,val usage:ChatTokenUsage?=null)
@Serializable data class ChatConversation(val id:String=UUID.randomUUID().toString(),val title:String="New chat",
    val messages:List<ChatMessage> = emptyList(),val updatedAt:Long=System.currentTimeMillis(),val options:ChatOptions=ChatOptions(),val usageNote:String?=null)
data class ChatState(val conversations:List<ChatConversation> = emptyList(),val selectedId:String?=null,
    val running:Boolean=false,val error:String?=null,val generationStartedMs:Long?=null,val activeOutputLimit:Int?=null) {
    val current get()=conversations.firstOrNull { it.id==selectedId }
}

class ChatController(private val context:Context,private val coordinator:InferenceCoordinator,private val appScope:CoroutineScope,private val providers:com.meshlit.providers.OnlineProviders,private val router:com.meshlit.routing.ModelRoutes,private val localTools:LocalChatTools,private val memory:PersonalMemory,private val library:com.meshlit.models.ModelLibrary) {
    private val _state=MutableStateFlow(ChatState())
    val state:StateFlow<ChatState> = _state.asStateFlow()
    private val file=File(context.filesDir,"conversations-v1.json")
    private val selectionFile=File(context.filesDir,"chat-selection-v1.txt")
    private val json=Json { ignoreUnknownKeys=true }
    private val persistence=Mutex()
    private var generation:Job?=null
    private val evidence=MutableStateFlow<ClusterOutputEvidence?>(null)
    val clusterEvidence=evidence.asStateFlow()
    fun budgetDecision(options:ChatOptions)=clusterOutputBudget(options,coordinator.engineTag,coordinator.loadedModel(),evidence.value,android.os.SystemClock.elapsedRealtime())
    @Volatile var generationId:String?=null; private set
    val ready=appScope.async(Dispatchers.IO) {
        val items=runCatching { json.decodeFromString<List<ChatConversation>>(file.readText()) }.getOrDefault(emptyList()).take(40)
        _state.value=ChatState(items,restoredChatSelection(items,runCatching{selectionFile.readText()}.getOrNull()))
    }
    private suspend fun save()=persistence.withLock { withContext(Dispatchers.IO) {
        val temp=File(file.parentFile,file.name+".tmp")
        temp.writeText(json.encodeToString(_state.value.conversations.take(40)))
        check(temp.renameTo(file)) { "Cannot save conversation history" }
        val selectionTemp=File(selectionFile.parentFile,selectionFile.name+".tmp")
        selectionTemp.writeText(_state.value.selectedId.orEmpty())
        check(selectionTemp.renameTo(selectionFile)) { "Cannot save conversation selection" }
    } }
    fun newChat() {
        if(!ready.isCompleted || _state.value.running) return
        val item=ChatConversation()
        _state.update { it.copy(conversations=(listOf(item)+it.conversations).take(40),selectedId=item.id,error=null) }
        appScope.launch { runCatching { save() } }
    }
    fun select(id:String) {
        if(ready.isCompleted && !_state.value.running && _state.value.conversations.any{it.id==id}) {
            _state.update { it.copy(selectedId=id,error=null) }
            appScope.launch {runCatching{save()}}
        }
    }
    fun delete(id:String) {
        if(!ready.isCompleted || _state.value.running) return
        _state.update { old -> val remaining=old.conversations.filterNot { it.id==id }
            old.copy(conversations=remaining,selectedId=if(old.selectedId==id) remaining.firstOrNull()?.id else old.selectedId) }
        appScope.launch { runCatching { save() } }
    }
    fun setOptions(options:ChatOptions) {
        options.validate();com.meshlit.BuildProfile.requireChat(options);check(!_state.value.running)
        if(_state.value.current==null) newChat()
        _state.update{old->old.copy(conversations=old.conversations.map{if(it.id==old.selectedId) it.copy(options=options) else it})}
        appScope.launch{runCatching{save()}.onFailure{error->_state.update{it.copy(error="Cannot save chat options: ${error.javaClass.simpleName}")}}}
    }
    fun send(text:String):String? {
        if(!ready.isCompleted || _state.value.running || text.isBlank()) return null
        require(text.length<=12000) { "Message is too long" }
        if(_state.value.current?.options?.onlineProfileId==null && _state.value.current?.options?.routeId==null && coordinator.state.value !is CoordinatorState.Ready) {
            _state.update { it.copy(error="Load a model from Models before sending a message") };return null
        }
        if(_state.value.current==null) newChat()
        val selected=_state.value.selectedId!!
        val user=ChatMessage(role="user",text=text.trim())
        val assistant=ChatMessage(role="assistant",text="")
        val configured=_state.value.current!!.options
        try { configured.validate();com.meshlit.BuildProfile.requireChat(configured) } catch(e:Exception) {
            _state.update { it.copy(error=e.message) };return null
        }
        val decision=budgetDecision(configured)
        val options=configured.copy(maxTokens=decision.limit)
        val history=_state.value.current!!.messages.filter{it.text.isNotBlank()}.takeLast(options.historyMessages)
        _state.update { old -> old.copy(running=true,error=null,generationStartedMs=android.os.SystemClock.elapsedRealtime(),activeOutputLimit=options.maxTokens,conversations=old.conversations.map { c ->
            if(c.id==selected) c.copy(title=if(c.messages.isEmpty()) text.trim().take(56) else c.title,
                messages=(c.messages+user+assistant).takeLast(100),updatedAt=System.currentTimeMillis()) else c }) }
        generationId=assistant.id
        generation=appScope.launch {
            val started=android.os.SystemClock.elapsedRealtime()
            fun recordUsage(usage:ChatTokenUsage) {_state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(messages=c.messages.map{if(it.id==assistant.id) it.copy(usage=usage.copy(budgetReason=decision.explanation)) else it}) else c})}}
            try {
                ready.await();save()
                androidx.core.content.ContextCompat.startForegroundService(context,
                    android.content.Intent(context,ChatInferenceService::class.java).putExtra("generation",assistant.id))
                if(options.routeId!=null) {
                    val prompt=buildString {
                        if(options.systemPrompt.isNotBlank()) append("Instructions: ").append(options.systemPrompt).append("\n\n")
                        history.forEach{append(it.role).append(": ").append(it.text.take(4000)).append('\n')}
                        append("user: ").append(user.text)
                    }
                    val result=router.execute(options.routeId.takeUnless{it=="__auto__"},options.routeScenario,prompt,options.maxTokens)
                    if(!result.success) error("Route ${result.routeId} failed at step ${(result.failedStep ?: 0)+1}: ${result.error}")
                    _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(messages=c.messages.map{if(it.id==assistant.id) it.copy(text=result.text) else it},usageNote="Route ${result.routeId} · ${result.mode} · ${result.steps.size} completed steps") else c})}
                    recordUsage(chatTokenUsage("Routed run · counts not aggregated",options.maxTokens,null,null,null,android.os.SystemClock.elapsedRealtime()-started,null,null))
                } else if(options.onlineProfileId!=null) {
                    val (profile,reply)=providers.generate(options.onlineProfileId, (history+user).map{com.meshlit.core.inference.models.OnlineMessage(it.role,it.text)},options.systemPrompt,options.maxTokens,options.temperature)
                    val cost=reply.estimatedCost(profile)
                    val note="${profile.name} · input ${reply.inputTokens ?: "unknown"}, output ${reply.outputTokens ?: "unknown"} tokens · "+
                        (cost?.let{"estimated ${"%.6f".format(it)} ${profile.currency} (user prices)"} ?: "cost unknown")
                    _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(messages=c.messages.map{if(it.id==assistant.id) it.copy(text=reply.text) else it},usageNote=note) else c})}
                    recordUsage(chatTokenUsage("Provider usage",options.maxTokens,reply.inputTokens,reply.outputTokens,null,android.os.SystemClock.elapsedRealtime()-started,null,null))
                } else {
                memory.captureRequest(user.text)
                val prompt=memory.context(user.text)+options.systemPrompt.takeIf{it.isNotBlank()}?.let{"Instructions: $it\n\n"}.orEmpty()+if(history.isEmpty()) user.text else buildString {
                    append("Continue this conversation. Answer the latest user message.\n")
                    history.forEach { append(it.role).append(": ").append(it.text.take(4000)).append('\n') }
                    append("user: ").append(user.text)
                }
                if(options.usesLocalTools) {
                    fun publish(value:String){_state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(messages=c.messages.map{if(it.id==assistant.id) it.copy(text=value) else it}) else c})}}
                    val answer=localTools.run(prompt,options){publish(it)}
                    publish(answer.text+if(answer.sources.isEmpty()) "" else "\n\nSources:\n"+answer.sources.joinToString("\n"){"- $it"})
                    _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(usageNote="On-device tool loop · ${answer.calls} tool calls · native token totals not aggregated") else c})}
                    recordUsage(chatTokenUsage("Tool loop · counts not aggregated",options.maxTokens,null,null,null,android.os.SystemClock.elapsedRealtime()-started,null,null))
                } else {
                val buffer=StringBuilder()
                val originalInfo=coordinator.loadedModel()
                val originalEngine=coordinator.engineTag
                val originalModel=library.models.value.firstOrNull{it.installed && it.path==originalInfo?.modelPath}
                suspend fun generate()=coordinator.infer(InferenceRequest(prompt=prompt,maxTokens=options.maxTokens,temperature=options.temperature,reuseContext=coordinator.engineTag=="llama-native-local",expectedModelPath=originalInfo?.modelPath,onToken={ token ->
                    buffer.append(token)
                    _state.update { old -> old.copy(conversations=old.conversations.map { c ->
                        if(c.id==selected) c.copy(messages=c.messages.map { if(it.id==assistant.id) it.copy(text=buffer.toString()) else it }) else c }) }
                }))
                var result=generate()
                if(result is MeshlitResult.Failure && result.error is com.meshlit.core.common.MeshlitError.Native && memory.state.value.policy.recovery && originalModel!=null && library.models.value.any{it==originalModel} && coordinator.loadedModel()?.modelPath==originalModel.path) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    library.load(originalModel.id)
                    buffer.clear()
                    result=generate()
                }
                val completed=result
                if(completed is MeshlitResult.Failure) error(completed.error.tag)
                if(completed is MeshlitResult.Success) {
                    val answer=completedChatText(buffer.toString(),completed.value.finalText)
                    _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(messages=c.messages.map{if(it.id==assistant.id) it.copy(text=answer) else it}) else c})}
                }
                if(completed is MeshlitResult.Success) _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(usageNote="Input ${completed.value.promptTokens ?: "unknown"}, output ${completed.value.generatedTokens ?: "unknown"} tokens · cached ${completed.value.cachedPromptTokens ?: "unknown"}") else c})}
                if(completed is MeshlitResult.Success && originalEngine=="llama-rpc-layer" && coordinator.engineTag==originalEngine && originalInfo!=null && coordinator.loadedModel()===originalInfo && (completed.value.generatedTokens ?: 0)>=8) {
                    completed.value.tokensPerSecond?.takeIf{it.isFinite() && it>0}?.let{evidence.value=ClusterOutputEvidence(originalInfo,it.toDouble(),android.os.SystemClock.elapsedRealtime())}
                }
                if(completed is MeshlitResult.Success) recordUsage(chatTokenUsage("Runtime usage",options.maxTokens,completed.value.promptTokens?.toLong(),completed.value.generatedTokens?.toLong(),completed.value.cachedPromptTokens?.toLong(),
                    android.os.SystemClock.elapsedRealtime()-started,completed.value.tokensPerSecond,originalInfo?.contextSize,completed.value.finishReason.tag))
                }
                }
            } catch(cancelled:CancellationException) { throw cancelled }
            catch(error:Exception) { _state.update { it.copy(error=error.message ?: "Generation failed") } }
            finally {
                withContext(NonCancellable) { runCatching { save() } }
                generationId=null
                context.stopService(android.content.Intent(context,ChatInferenceService::class.java))
                _state.update { it.copy(running=false,generationStartedMs=null,activeOutputLimit=null) }
            }
        }
        return assistant.id
    }
    fun stopIfMatching(id:String?) { if(id!=null && id==generationId) stop() }
    fun stop() { generation?.cancel();coordinator.cancel() }
    fun attachmentError(message:String){_state.update{it.copy(error=message)}}
    fun clearError() { _state.update { it.copy(error=null) } }
}

/** Prefer the completed runtime result, including engines which emit no text callbacks. */
internal fun completedChatText(streamed:String,completed:String):String {
    val answer=completed.ifBlank{streamed}
    check(answer.isNotBlank()){ "The model returned an empty answer. Try another prompt or check the selected model; no response was substituted." }
    return answer
}
internal fun restoredChatSelection(items:List<ChatConversation>,saved:String?)=items.firstOrNull{it.id==saved}?.id ?: items.firstOrNull()?.id
