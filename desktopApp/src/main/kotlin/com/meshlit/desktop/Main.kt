package com.meshlit.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.meshlit.workspace.*
import com.meshlit.workspace.richtext.*
import kotlinx.coroutines.*

private data class Message(val role: String, val text: String, val completed: Boolean = true)
fun main(args: Array<String>) {
    if (args.size == 2 && args[0] == "--render-check") { renderCheck(args[1]); return }
    if (args.firstOrNull() == "--cli") { runCli(args.drop(1).toTypedArray()); return }
    application {
    Window(onCloseRequest = ::exitApplication, title = "Meshlit · Experimental desktop client",
        state = rememberWindowState(width = 1120.dp, height = 800.dp)) { Workspace() }
    }
}
@Composable internal fun Workspace() {
    val preferences = remember { DesktopPreferences() }
    var language by remember { mutableStateOf(preferences.language()) }
    var look by remember { mutableStateOf(preferences.look()) }
    var scale by remember { mutableStateOf(preferences.scale()) }
    fun t(en: String, zh: String) = language.text(en, zh)
    var preferencesError by remember { mutableStateOf(false) }
    fun save() { preferencesError = runCatching { preferences.save(language, look, scale) }.isFailure }
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var prompt by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var colibriHost by remember { mutableStateOf(false) }
    var colibriMode by remember { mutableStateOf(ColibriMode.OFF) }
    var observedAt by remember { mutableStateOf<Long?>(null) }
    var network by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(listOf<String>()) }
    var model by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf(2048f) }
    var settings by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf(false) }
    var usage by remember { mutableStateOf(GenerationUsage()) }
    var job by remember { mutableStateOf<Job?>(null) }
    var client by remember { mutableStateOf<HostClient?>(null) }
    var revision by remember { mutableStateOf(0) }
    fun stop() { revision++; client?.close(); job?.cancel(); client = null; busy = false }
    DisposableEffect(Unit) { onDispose { client?.close(); job?.cancel() } }
    fun colibriReady() = connected && observedAt?.let { System.nanoTime() - it in 0..300_000_000_000L } == true
    fun hostAllowed() = !colibriHost || colibriDecision(colibriMode, network, colibriReady(), false).route == ColibriRoute.HOST
    fun connect() {
        if (!network || colibriHost && colibriMode == ColibriMode.OFF) return
        stop(); failure = false; connected = false; models = emptyList(); model = ""; busy = true
        val currentRevision = revision
        job = scope.launch {
            var active: HostClient? = null
            try {
                val host = HostEndpoint.parse(endpoint); val connectedClient = HostClient(host, token); active = connectedClient; client = connectedClient
                val choices = withContext(Dispatchers.IO) { connectedClient.models() }
                if (revision != currentRevision) throw CancellationException("Request superseded")
                models = choices; model = choices.firstOrNull().orEmpty(); connected = true; observedAt = System.nanoTime()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (revision == currentRevision) failure = true }
            finally { active?.close(); if (revision == currentRevision) { client = null; busy = false } }
        }
    }
    fun send() {
        if (busy || !connected || !network || !hostAllowed() || prompt.isBlank()) return
        val context = messages.filter { it.completed }.map { ChatTurn(it.role, it.text) } + ChatTurn("user", prompt.trim())
        if (context.size > 64 || context.sumOf { it.content.length.toLong() } > 131072) { failure = true; return }
        messages = messages + Message("user", prompt.trim()) + Message("assistant", "", false)
        prompt = ""; usage = GenerationUsage(); failure = false; busy = true
        val currentRevision = revision
        job = scope.launch {
            var active: HostClient? = null
            try {
                val generationClient = HostClient(HostEndpoint.parse(endpoint), token); active = generationClient; client = generationClient
                val result = withContext(Dispatchers.IO) {
                    generationClient.generate(model, context, GenerationBudget(budget.toInt())) { chunk ->
                        runBlocking { withContext(Dispatchers.Main) { if (revision == currentRevision) messages = messages.dropLast(1) + messages.last().copy(text = messages.last().text + chunk) } }
                    }
                }
                if (revision != currentRevision) throw CancellationException("Request superseded")
                messages = messages.dropLast(1) + messages.last().copy(completed = true); usage = result
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (revision == currentRevision) failure = true }
            finally { active?.close(); if (revision == currentRevision) { client = null; busy = false } }
        }
    }
    val colors = if (look == WorkspaceLook.PAPER) lightColors(primary = Color(look.accent), background = Color(look.background),
        surface = Color(look.surface), onBackground = Color(look.foreground), onSurface = Color(look.foreground))
        else darkColors(primary = Color(look.accent), background = Color(look.background), surface = Color(look.surface),
            onBackground = Color(look.foreground), onSurface = Color(look.foreground), onPrimary = Color(0xFF101113))
    MaterialTheme(colors = colors, typography = Typography(defaultFontFamily = FontFamily.SansSerif,
        body1 = TextStyle(fontSize = (16 * scale).sp, lineHeight = (25 * scale).sp))) {
        Surface(Modifier.fillMaxSize(), color = colors.background) {
            BoxWithConstraints {
                val wide = maxWidth > 860.dp
                Row(Modifier.fillMaxSize()) {
                    if (wide) Column(Modifier.width(248.dp).fillMaxHeight().background(colors.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Meshlit", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
                        Text(t("Experimental client", "实验性客户端"), color = colors.onSurface.copy(alpha = .6f))
                        OutlinedButton({ stop(); messages = emptyList(); usage = GenerationUsage() }, Modifier.fillMaxWidth()) { Text(t("＋ New chat", "＋ 新对话")) }
                        Text(t("Session history stays in memory. Closing the window clears it.", "对话仅保存在内存中，关闭窗口后清除。"), style = MaterialTheme.typography.caption)
                        Spacer(Modifier.weight(1f))
                        Text(t("Android · Desktop · HarmonyOS NEXT", "Android · 桌面 · HarmonyOS NEXT"), style = MaterialTheme.typography.caption)
                        OutlinedButton({ settings = true }, Modifier.fillMaxWidth()) { Text(t("Settings & connection", "设置与连接")) }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(model.ifBlank { "Meshlit" }, fontWeight = FontWeight.Medium)
                                Text(if (colibriHost) "Colibri · ${colibriMode.name} · host client" else t("Host client · no bundled local engine", "主机客户端 · 未内置本地推理引擎"), style = MaterialTheme.typography.caption, color = colors.onSurface.copy(alpha = .65f))
                            }
                            TextButton({ stop(); messages = emptyList(); usage = GenerationUsage() }) { Text(t("New chat", "新对话")) }
                            TextButton({ settings = true }) { Text(t("Settings", "设置")) }
                        }
                        if (messages.isNotEmpty()) OutlinedTextField(query, { query = it.take(256) }, label = { Text(t("Search this chat", "搜索当前对话")) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), singleLine = true)
                        if (failure) Text(t("Request failed. Check the host, trusted TLS, client token and context size. A partial reply is excluded from the next request.", "请求失败。请检查主机、可信 TLS、客户端令牌和上下文长度。未完成的回复不会发送至下一次请求。"), Modifier.padding(24.dp), color = colors.error)
                        if (messages.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✦", fontSize = 48.sp, color = colors.primary)
                            Spacer(Modifier.height(20.dp)); Text(t("What should we work on?", "我们开始做什么？"), style = MaterialTheme.typography.h4)
                            Spacer(Modifier.height(12.dp)); Text(t("Choose a trusted Meshlit or OpenAI-compatible host in Settings.", "请在设置中选择可信的 Meshlit 或 OpenAI 兼容主机。"), color = colors.onSurface.copy(alpha = .65f))
                            Spacer(Modifier.height(20.dp))
                            HeroMenu(::t, onChat = { stop(); messages = emptyList(); usage = GenerationUsage() }, onModels = { settings = true }, onStyle = { settings = true })
                        } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                            itemsIndexed(messages.filter { query.isBlank() || it.text.contains(query, true) }) { _, message ->
                                if (message.role == "user") Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, modifier = Modifier.widthIn(max = 560.dp)) { SelectionContainer { Text(message.text, Modifier.padding(18.dp)) } }
                                } else Column(Modifier.fillMaxWidth().widthIn(max = 860.dp)) {
                                    if (message.text.isEmpty()) Text(t("Generating…", "正在生成…"), color = colors.onSurface.copy(alpha = .65f))
                                    RichReply(message.text, scale)
                                    val clipboard = LocalClipboardManager.current
                                    Row { TextButton({ clipboard.setText(AnnotatedString(message.text)) }, enabled = message.text.isNotEmpty()) { Text(t("Copy", "复制")) }
                                        if (!message.completed) Text(t("Incomplete", "未完成"), Modifier.padding(12.dp), style = MaterialTheme.typography.caption) }
                                }
                            }
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Surface(shape = RoundedCornerShape(26.dp), color = colors.surface) {
                                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    TextField(prompt, { prompt = it.take(131072) }, placeholder = { Text(t("Ask Meshlit", "向 Meshlit 提问")) }, modifier = Modifier.weight(1f), maxLines = 6,
                                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
                                    Button(if (busy) ::stop else ::send, enabled = busy || (network && connected && hostAllowed() && model.isNotBlank() && prompt.isNotBlank()), shape = RoundedCornerShape(22.dp)) { Text(if (busy) t("Stop", "停止") else t("Send ↑", "发送 ↑")) }
                                }
                            }
                            Text(t("Reported output tokens", "报告的输出令牌") + ": ${usage.outputTokens ?: t("unknown", "未知")} · " + t("End-to-end tokens/s", "端到端令牌/秒") + ": ${usage.tokensPerSecond?.let { "%.2f".format(it) } ?: t("unknown", "未知")}", style = MaterialTheme.typography.caption, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            if (settings) DialogWindow(onCloseRequest = { settings = false }, title = t("Settings & connection", "设置与连接"), state = rememberDialogState(width = 660.dp, height = 760.dp)) {
                Surface(Modifier.fillMaxSize(), color = colors.background) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MenuHeading("style", t("Make it yours", "个性化设置"))
                        Row { WorkspaceLanguage.entries.forEach { l -> TextButton({ language = l; save() }) { Text((if (language == l) "✓ " else "") + l.label) } } }
                        Row { WorkspaceLook.entries.forEach { l -> TextButton({ look = l; save() }) { Text((if (look == l) "✓ " else "") + l.label) } } }
                        Text(t("Text size", "字体大小")); Slider(scale, { scale = it }, valueRange = .85f..1.5f, onValueChangeFinished = ::save)
                        if (preferencesError) Text(t("Appearance could not be saved.", "无法保存外观设置。"), color = colors.error)
                        Divider(); MenuHeading("models", t("Explicit host access", "明确授权主机访问"))
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(colibriHost, { stop(); colibriHost = it; connected = false; observedAt = null }); Text(t("Use Colibri host", "使用 Colibri 主机")) }
                        if (colibriHost) {
                            Row { ColibriMode.entries.forEach { mode -> TextButton({ stop(); colibriMode = mode; if (mode == ColibriMode.OFF) connected = false }) { Text((if (colibriMode == mode) "✓ " else "") + mode.name) } } }
                            Text(t("Auto uses this authorized, recently observed host because this desktop preview has no local engine. Refresh expires after 5 minutes. No server or model is installed. Agent switching is supported in the Android Experimental node with separate user grants; this preview has no agent controller.", "桌面预览没有本地引擎，因此自动模式使用已授权且近期确认的主机。刷新记录在五分钟后失效，不会安装服务器或模型。代理切换需要 Android 实验性节点中的单独授权；本预览没有代理控制器。"), style = MaterialTheme.typography.caption)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(network, { network = it; if (!it) { stop(); connected = false; models = emptyList(); model = "" } }); Text(t("Allow requests to the selected host", "允许向所选主机发送请求")) }
                        Text(t("Prompts and your token go to this host. TLS uses the system trust store; redirects are blocked. HTTP is limited to literal loopback. Host model operation is managed separately.", "提示词和令牌将发送至此主机。TLS 使用系统信任库，并禁止重定向。HTTP 仅限本机回环地址。主机模型需要单独管理。"), style = MaterialTheme.typography.caption)
                        OutlinedTextField(endpoint, { stop(); endpoint = it.take(2048); connected = false; model = ""; models = emptyList() }, label = { Text("https://host:port/v1") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(token, { stop(); token = it.take(8192); connected = false }, label = { Text(t("Client token · session only", "客户端令牌 · 仅当前会话")) }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                        Button(::connect, enabled = network && !busy && endpoint.isNotBlank() && (!colibriHost || colibriMode != ColibriMode.OFF)) { Text(t("Connect / refresh models", "连接 / 刷新模型")) }
                        models.forEach { id -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(model == id, { model = id }); Text(id) } }
                        if (connected && models.isEmpty()) Text(t("The host reports no models.", "主机未报告可用模型。"))
                        Text(t("Output limit", "输出上限") + ": ${budget.toInt()} tokens"); Slider(budget, { budget = it }, valueRange = 64f..4096f, steps = 62)
                        Text(t("Actual throughput appears only when the host reports token usage; it includes request latency. Language affects UI only.", "仅在主机报告令牌用量时显示吞吐量；此值包含请求延迟。语言设置仅影响界面。"), style = MaterialTheme.typography.caption)
                        TextButton({ settings = false }) { Text(t("Done", "完成")) }
                    }
                }
            }
        }
    }
}
@Composable private fun RichReply(text: String, scale: Float) {
    val document = remember(text) { formatReply(text) }
    val colors = MaterialTheme.colors
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            document.blocks.forEach { block -> when (block) {
                is ReplyBlock.Prose -> {
                    val annotated = buildAnnotatedString { block.spans.forEach { s -> withStyle(SpanStyle(fontWeight = if (s.bold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (s.italic) androidx.compose.ui.text.font.FontStyle.Italic else null,
                        fontFamily = if (s.code) FontFamily.Monospace else null, color = if (s.url != null) colors.primary else Color.Unspecified)) { append(s.text) } } }
                    Row(Modifier.padding(start = (block.indent.coerceAtMost(4) * 12).dp)) {
                        block.marker?.let { Text("$it ") }
                        Text(annotated, fontSize = ((when (block.heading) { 1 -> 30; 2 -> 25; 3 -> 21; else -> 16 }) * scale).sp,
                            lineHeight = ((if (block.heading > 0) 34 else 25) * scale).sp, fontWeight = if (block.heading > 0) FontWeight.Bold else null)
                    }
                }
                is ReplyBlock.Code -> Surface(shape = RoundedCornerShape(16.dp), color = colors.surface) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        val clipboard = LocalClipboardManager.current
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(block.language.ifBlank { "text" }, Modifier.weight(1f), style = MaterialTheme.typography.caption)
                            TextButton({ clipboard.setText(AnnotatedString(block.text)) }) { Text("Copy / 复制") } }
                        Text(block.text, Modifier.horizontalScroll(rememberScrollState()), fontFamily = FontFamily.Monospace, fontSize = (14 * scale).sp)
                    }
                }
                is ReplyBlock.Table -> Column(Modifier.horizontalScroll(rememberScrollState())) {
                    (listOf(block.header) + block.rows).forEachIndexed { index, row ->
                        Row { row.forEach { cell -> Text(cell.joinToString("") { it.text }, Modifier.width(200.dp).padding(12.dp), fontWeight = if (index == 0) FontWeight.Bold else null) } }; Divider()
                    }
                }
                is ReplyBlock.Chart -> Column(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                    Text(block.title, fontWeight = FontWeight.Bold)
                    val maximum = block.values.maxOfOrNull { kotlin.math.abs(it) }?.takeIf { it > 0 } ?: 1.0
                    block.labels.zip(block.values).forEach { (label, value) ->
                        Text("$label · $value ${block.unit}", style = MaterialTheme.typography.caption)
                        LinearProgressIndicator((kotlin.math.abs(value) / maximum).toFloat(), Modifier.fillMaxWidth().padding(vertical = 6.dp))
                    }
                }
                ReplyBlock.Rule -> Divider()
            } }
        }
    }
}
