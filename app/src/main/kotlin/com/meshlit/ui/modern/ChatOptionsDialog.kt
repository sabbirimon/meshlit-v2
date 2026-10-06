package com.meshlit.ui.modern
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlit.chat.*
import com.meshlit.providers.OnlineProviders
import com.meshlit.di.koinInject

@Composable fun ChatOptionsDialog(state:ChatState,controller:ChatController,onClose:()->Unit) {
    val providers=koinInject<OnlineProviders>();val profiles by providers.profiles.collectAsStateWithLifecycle()
    val router=koinInject<com.meshlit.routing.ModelRoutes>();val routes by router.routes.collectAsStateWithLifecycle()
    var options by remember{mutableStateOf(state.current?.options ?: ChatOptions())}
    var error by remember{mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onClose,title={Text("Conversation options")},text={Column(Modifier.verticalScroll(rememberScrollState())){
        FilterChip(options.onlineProfileId==null && options.routeId==null,{options=options.copy(onlineProfileId=null,routeId=null)},label={Text("Offline · currently loaded local/cluster model")})
        profiles.filter{it.enabled}.forEach{profile->FilterChip(options.onlineProfileId==profile.id && options.routeId==null,{options=options.copy(onlineProfileId=profile.id,routeId=null)},label={Text("Online · ${profile.name} / ${profile.model}")})}
        if(routes.any{it.enabled}) {
            FilterChip(options.routeId=="__auto__",{options=options.copy(routeId="__auto__",onlineProfileId=null,maxTokens=minOf(options.maxTokens,1024))},label={Text("Model router · automatic rules")})
            routes.filter{it.enabled}.forEach{route->FilterChip(options.routeId==route.id,{options=options.copy(routeId=route.id,onlineProfileId=null,maxTokens=minOf(options.maxTokens,1024))},label={Text("Route · ${route.name} / ${route.mode}")})}
            if(options.routeId!=null) OutlinedTextField(options.routeScenario,{options=options.copy(routeScenario=it.lowercase())},label={Text("Route scenario")})
        }
        Text("Configure API profiles in Settings → Online providers. Online sends this conversation to the selected endpoint and may incur charges. Replies are buffered; local replies stream.")
        OutlinedTextField(options.systemPrompt,{options=options.copy(systemPrompt=it)},label={Text("Instructions")},maxLines=4)
        Text("Output token limit: ${options.maxTokens}");Slider(options.maxTokens.toFloat(),{options=options.copy(maxTokens=it.toInt())},valueRange=16f..if(options.routeId==null) 2048f else 1024f)
        Text("Temperature: ${"%.2f".format(options.temperature)}");Slider(options.temperature,{options=options.copy(temperature=it)},valueRange=0f..2f)
        Text("History messages: ${options.historyMessages}");Slider(options.historyMessages.toFloat(),{options=options.copy(historyMessages=it.toInt())},valueRange=0f..20f,steps=19)
        Text("Context capacity and weight quantization are configured per local model in Models. Online temperature is sent only when enabled for that profile.")
        error?.let{Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={try{controller.setOptions(options);onClose()}catch(e:Exception){error=e.message}}){Text("Save")}},dismissButton={TextButton(onClick=onClose){Text("Cancel")}})
}
