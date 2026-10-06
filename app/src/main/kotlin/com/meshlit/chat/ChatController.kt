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

@Serializable data class ChatOptions(val onlineProfileId:String?=null,val systemPrompt:String="",val maxTokens:Int=1024,val temperature:Float=0.7f,val historyMessages:Int=10,val routeId:String?=null,val routeScenario:String="general") {
    fun validate(){require(routeId==null || routeId=="__auto__" || routeId.matches(Regex("[A-Za-z0-9_-]{1,64}")));require(routeScenario.matches(Regex("[a-z0-9_-]{1,40}")));require(routeId==null || (onlineProfileId==null && maxTokens<=1024));require(systemPrompt.length<=4000 && maxTokens in 1..2048 && temperature.isFinite() && temperature in 0f..2f && historyMessages in 0..20)}
}
@Serializable data class ChatMessage(val id:String=UUID.randomUUID().toString(),val role:String,val text:String)
@Serializable data class ChatConversation(val id:String=UUID.randomUUID().toString(),val title:String="New chat",
    val messages:List<ChatMessage> = emptyList(),val updatedAt:Long=System.currentTimeMillis(),val options:ChatOptions=ChatOptions(),val usageNote:String?=null)
data class ChatState(val conversations:List<ChatConversation> = emptyList(),val selectedId:String?=null,
    val running:Boolean=false,val error:String?=null) {
    val current get()=conversations.firstOrNull { it.id==selectedId }
}

class ChatController(private val context:Context,private val coordinator:InferenceCoordinator,private val appScope:CoroutineScope,private val providers:com.meshlit.providers.OnlineProviders,private val router:com.meshlit.routing.ModelRoutes) {
    private val _state=MutableStateFlow(ChatState())
    val state:StateFlow<ChatState> = _state.asStateFlow()
    private val file=File(context.filesDir,"conversations-v1.json")
    private val json=Json { ignoreUnknownKeys=true }
    private val persistence=Mutex()
    private var generation:Job?=null
    @Volatile var generationId:String?=null; private set
    val ready=appScope.async(Dispatchers.IO) {
        val items=runCatching { json.decodeFromString<List<ChatConversation>>(file.readText()) }.getOrDefault(emptyList()).take(40)
        _state.value=ChatState(items,items.firstOrNull()?.id)
    }
    private suspend fun save()=persistence.withLock { withContext(Dispatchers.IO) {
        val temp=File(file.parentFile,file.name+".tmp")
        temp.writeText(json.encodeToString(_state.value.conversations.take(40)))
        check(temp.renameTo(file)) { "Cannot save conversation history" }
    } }
    fun newChat() {
        if(!ready.isCompleted || _state.value.running) return
        val item=ChatConversation()
        _state.update { it.copy(conversations=(listOf(item)+it.conversations).take(40),selectedId=item.id,error=null) }
        appScope.launch { runCatching { save() } }
    }
    fun select(id:String) { if(ready.isCompleted && !_state.value.running) _state.update { it.copy(selectedId=id,error=null) } }
    fun delete(id:String) {
        if(!ready.isCompleted || _state.value.running) return
        _state.update { old -> val remaining=old.conversations.filterNot { it.id==id }
            old.copy(conversations=remaining,selectedId=if(old.selectedId==id) remaining.firstOrNull()?.id else old.selectedId) }
        appScope.launch { runCatching { save() } }
    }
    fun setOptions(options:ChatOptions) {
        options.validate();check(!_state.value.running)
        if(_state.value.current==null) newChat()
        _state.update{old->old.copy(conversations=old.conversations.map{if(it.id==old.selectedId) it.copy(options=options) else it})}
        appScope.launch{runCatching{save()}.onFailure{error->_state.update{it.copy(error="Cannot save chat options: ${error.javaClass.simpleName}")}}}
    }
    fun send(text:String) {
        if(!ready.isCompleted || _state.value.running || text.isBlank()) return
        require(text.length<=12000) { "Message is too long" }
        if(_state.value.current?.options?.onlineProfileId==null && _state.value.current?.options?.routeId==null && coordinator.state.value !is CoordinatorState.Ready) {
            _state.update { it.copy(error="Load a model from Models before sending a message") };return
        }
        if(_state.value.current==null) newChat()
        val selected=_state.value.selectedId!!
        val user=ChatMessage(role="user",text=text.trim())
        val assistant=ChatMessage(role="assistant",text="")
        val options=_state.value.current!!.options
        val history=_state.value.current!!.messages.filter{it.text.isNotBlank()}.takeLast(options.historyMessages)
        _state.update { old -> old.copy(running=true,error=null,conversations=old.conversations.map { c ->
            if(c.id==selected) c.copy(title=if(c.messages.isEmpty()) text.trim().take(56) else c.title,
                messages=(c.messages+user+assistant).takeLast(100),updatedAt=System.currentTimeMillis()) else c }) }
        generationId=assistant.id
        generation=appScope.launch {
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
                } else if(options.onlineProfileId!=null) {
                    val (profile,reply)=providers.generate(options.onlineProfileId, (history+user).map{com.meshlit.core.inference.models.OnlineMessage(it.role,it.text)},options.systemPrompt,options.maxTokens,options.temperature)
                    val cost=reply.estimatedCost(profile)
                    val note="${profile.name} · input ${reply.inputTokens ?: "unknown"}, output ${reply.outputTokens ?: "unknown"} tokens · "+
                        (cost?.let{"estimated ${"%.6f".format(it)} ${profile.currency} (user prices)"} ?: "cost unknown")
                    _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(messages=c.messages.map{if(it.id==assistant.id) it.copy(text=reply.text) else it},usageNote=note) else c})}
                } else {
                val prompt=options.systemPrompt.takeIf{it.isNotBlank()}?.let{"Instructions: $it\n\n"}.orEmpty()+if(history.isEmpty()) user.text else buildString {
                    append("Continue this conversation. Answer the latest user message.\n")
                    history.forEach { append(it.role).append(": ").append(it.text.take(4000)).append('\n') }
                    append("user: ").append(user.text)
                }
                val buffer=StringBuilder()
                val result=coordinator.infer(InferenceRequest(prompt=prompt,maxTokens=options.maxTokens,temperature=options.temperature,reuseContext=coordinator.engineTag=="llama-native-local",onToken={ token ->
                    buffer.append(token)
                    _state.update { old -> old.copy(conversations=old.conversations.map { c ->
                        if(c.id==selected) c.copy(messages=c.messages.map { if(it.id==assistant.id) it.copy(text=buffer.toString()) else it }) else c }) }
                }))
                if(result is MeshlitResult.Failure) error(result.error.tag)
                if(result is MeshlitResult.Success) _state.update{old->old.copy(conversations=old.conversations.map{c->if(c.id==selected) c.copy(usageNote="Input ${result.value.promptTokens ?: "unknown"}, output ${result.value.generatedTokens ?: "unknown"} tokens · cached ${result.value.cachedPromptTokens ?: "unknown"}") else c})}
                }
            } catch(cancelled:CancellationException) { throw cancelled }
            catch(error:Exception) { _state.update { it.copy(error=error.message ?: "Generation failed") } }
            finally {
                withContext(NonCancellable) { runCatching { save() } }
                generationId=null
                context.stopService(android.content.Intent(context,ChatInferenceService::class.java))
                _state.update { it.copy(running=false) }
            }
        }
    }
    fun stopIfMatching(id:String?) { if(id!=null && id==generationId) stop() }
    fun stop() { generation?.cancel();coordinator.cancel() }
    fun attachmentError(message:String){_state.update{it.copy(error=message)}}
    fun clearError() { _state.update { it.copy(error=null) } }
}
