package com.meshlit.ui.modern

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.meshlit.di.koinInject
import com.meshlit.gateway.*
import com.meshlit.core.mcp.gateway.*

@OptIn(ExperimentalMaterial3Api::class,ExperimentalLayoutApi::class)
@Composable fun GatewayScreen(back:()->Unit) {
    val host=koinInject<GatewayHost>();val saved by host.settings.collectAsState();val running by host.running.collectAsState()
    var model by remember {mutableStateOf(saved.modelId)};var port by remember {mutableStateOf(saved.port.toString())}
    var mode by remember {mutableStateOf(saved.policy.mode)};var input by remember {mutableStateOf(saved.policy.inputTerms.joinToString("\n"))};var output by remember {mutableStateOf(saved.policy.outputTerms.joinToString("\n"))}
    DisposableEffect(Unit) {onDispose {host.stop()}}
    var error by remember {mutableStateOf<String?>(null)}
    val clipboard=androidx.compose.ui.platform.LocalClipboardManager.current
    fun save() {host.save(GatewaySettings(port.toInt(),model,GatewayPolicy(mode,input.lines().filter {it.isNotBlank()},output.lines().filter {it.isNotBlank()})))}
    Scaffold(topBar={TopAppBar(title={Text("Agent Gateway")},navigationIcon={TextButton(onClick=back){Text("Back")}})}) {padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {Text(if(running) "Running on loopback · port ${saved.port}" else "Stopped · screen-bound session; no automatic startup")}
            item {Text("Built-in MCP and A2A 0.3 task endpoints, plus buffered OpenAI-compatible text. Select a model ID from Models/Agent management. Incoming calls use saved agent delegation. This is Meshlit's Kotlin implementation; the upstream Rust agentgateway is a separate host runtime.")}
            item {OutlinedTextField(model,{model=it},label={Text("Model ID, or cloud:profile-id")},modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(port,{port=it},label={Text("Loopback port")})}
            item {Text("Content policy");FlowRow {ContentMode.entries.forEach {value->FilterChip(mode==value,{mode=value},label={Text(value.name)})}}}
            item {Text("Filtered/custom apply your literal rules. An empty list performs no content moderation. Minimal skips these optional rules. Provider restrictions and permissions remain enforced.")}
            item {OutlinedTextField(input,{input=it},label={Text("Block input terms · one per line")},modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(output,{output=it},label={Text("Block output terms · one per line")},modifier=Modifier.fillMaxWidth())}
            item {FlowRow {
                Button(onClick={try {save();error=null}catch(_:Exception){error="Invalid port, model or policy"}}){Text("Save and stop")}
                Button(enabled=!com.meshlit.BuildConfig.PLAY_REVIEW,onClick={try {save();host.start();error=null}catch(_:Exception){error="Gateway could not start; check port and build restrictions"}}){Text("Start")}
                OutlinedButton(onClick={host.stop()}){Text("Stop")}
                OutlinedButton(onClick={clipboard.setText(androidx.compose.ui.text.AnnotatedString(host.token()))}){Text("Copy access token")}
                OutlinedButton(onClick={host.rotate()}){Text("Revoke / rotate token")}
            }}
            item {Text("Endpoints: /mcp · /a2a · /.well-known/agent-card.json · /v1/models · /v1/chat/completions. All require Bearer authentication. Browser origins are rejected. Use a verified private tunnel and host TLS gateway for remote access. Copying the token places a secret on your clipboard.")}
            item {Text("Connect an external agentgateway/LiteLLM endpoint through Online providers → Custom compatible. Use its exact HTTPS API base, gateway key and model alias. Provider keys can stay on the host.")}
            error?.let {item {Text(it,color=MaterialTheme.colorScheme.error)}}
        }
    }
}
