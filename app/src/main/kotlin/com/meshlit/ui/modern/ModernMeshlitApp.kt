package com.meshlit.ui.modern

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlit.R
import com.meshlit.chat.*
import com.meshlit.core.inference.*
import com.meshlit.di.koinInject
import com.meshlit.ui.theme.ChatTokens as T
import kotlinx.coroutines.launch

/** Shared clean navigation for both APK flavors. Advanced legacy tools remain
 * accessible without taking over the main conversation surface. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMeshlitApp() {
    val chats=koinInject<ChatController>()
    val state by chats.state.collectAsStateWithLifecycle()
    val coordinator=koinInject<InferenceCoordinator>()
    val runtime by coordinator.state.collectAsStateWithLifecycle()
    val drawer=rememberDrawerState(DrawerValue.Closed)
    val scope=rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf("chat") }
    var legacy by rememberSaveable { mutableStateOf(false) }
    var permissionSetup by rememberSaveable{mutableStateOf(false)}
    com.meshlit.permissions.FirstLaunchPermissionSetup{permissionSetup=true}
    if(permissionSetup){com.meshlit.permissions.PermissionSetupScreen{permissionSetup=false};return}
    LaunchedEffect(chats) { chats.ready.await() }
    if(tab=="media"){MediaGenerationScreen{tab="chat"};return}
    if(tab=="tasks"){TaskManagerScreen{tab="chat"};return}
    if(tab=="ide"){com.meshlit.ide.CodeWorkspaceScreen{tab="chat"};return}
    if(tab=="devices"){ModernNetworkScreen{tab="chat"};return}
    if(legacy) {
        BackHandler { legacy=false }
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick={legacy=false}) { Text(stringResource(R.string.modern_back_chat)) }
            Box(Modifier.weight(1f)) { com.meshlit.ui.LegacyMeshlitApp() }
        }
        return
    }
    ModalNavigationDrawer(drawerState=drawer,drawerContent={
        ModalDrawerSheet(Modifier.width(T.sidebar)) {
            Column(Modifier.padding(T.large)) {
                Text("Meshlit",style=MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.modern_private_ai),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(T.section))
                FilledTonalButton(enabled=!state.running,onClick={chats.newChat();tab="chat";scope.launch{drawer.close()}},modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add,null);Spacer(Modifier.width(T.small));Text(stringResource(R.string.modern_new_chat)) }
            }
            Text(stringResource(R.string.modern_recent),Modifier.padding(horizontal=T.large),style=MaterialTheme.typography.labelMedium)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(T.small)) {
                items(state.conversations,key={it.id}) { conversation ->
                    NavigationDrawerItem(label={Text(conversation.title,maxLines=2)},selected=state.selectedId==conversation.id,
                        onClick={chats.select(conversation.id);tab="chat";scope.launch{drawer.close()}},
                        badge={IconButton(enabled=!state.running,onClick={chats.delete(conversation.id)}) {
                            Icon(Icons.Default.Delete,stringResource(R.string.modern_delete_chat)) }})
                }
            }
            NavigationDrawerItem(label={Text(stringResource(R.string.modern_models))},selected=tab=="models",onClick={tab="models";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Storage,null)})
            NavigationDrawerItem(label={Text(stringResource(R.string.modern_monitor))},selected=tab=="monitor",onClick={tab="monitor";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Insights,null)})
            NavigationDrawerItem(label={Text("Devices and clusters")},selected=tab=="devices",onClick={tab="devices";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Devices,null)})
            NavigationDrawerItem(label={Text("Task manager")},selected=tab=="tasks",onClick={tab="tasks";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Checklist,null)})
            NavigationDrawerItem(label={Text("Code workspace")},selected=tab=="ide",onClick={tab="ide";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Code,null)})
            NavigationDrawerItem(label={Text("Settings")},selected=tab=="settings",onClick={tab="settings";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Settings,null)})
            NavigationDrawerItem(label={Text(stringResource(R.string.modern_tools))},selected=false,onClick={legacy=true;scope.launch{drawer.close()}},icon={Icon(Icons.Default.Build,null)})
        }
    }) {
        Scaffold(topBar={TopAppBar(title={Column {
            Text(if(tab=="chat") state.current?.title ?: stringResource(R.string.modern_new_chat) else
                if(tab=="settings") "Settings" else stringResource(if(tab=="models") R.string.modern_models else R.string.modern_monitor),maxLines=1,style=MaterialTheme.typography.titleMedium)
            if(tab=="chat") TextButton(onClick={tab="models"},contentPadding=PaddingValues(T.tiny)) {
                Text(when(val current=runtime) {
                    is CoordinatorState.Ready -> current.model.modelName
                    is CoordinatorState.Loading -> stringResource(R.string.modern_loading)
                    is CoordinatorState.Generating -> stringResource(R.string.modern_generating)
                    CoordinatorState.Starting -> stringResource(R.string.modern_starting)
                    else -> stringResource(R.string.modern_choose_model)
                },maxLines=1,style=MaterialTheme.typography.labelMedium)
            }
        }},navigationIcon={IconButton(onClick={scope.launch{drawer.open()}}) {Icon(Icons.Default.Menu,stringResource(R.string.modern_menu))}},
            actions={IconButton(enabled=!state.running,onClick={chats.newChat();tab="chat"}) {Icon(Icons.Default.Add,stringResource(R.string.modern_new_chat))}})},
            bottomBar={ if(tab!="chat") NavigationBar {
                NavigationBarItem(tab=="chat",{tab="chat"},icon={Icon(Icons.Default.ChatBubbleOutline,null)},label={Text(stringResource(R.string.modern_chat))})
                NavigationBarItem(tab=="models",{tab="models"},icon={Icon(Icons.Default.Storage,null)},label={Text(stringResource(R.string.modern_models))})
                NavigationBarItem(tab=="monitor",{tab="monitor"},icon={Icon(Icons.Default.Insights,null)},label={Text(stringResource(R.string.modern_monitor))})
                NavigationBarItem(tab=="settings",{tab="settings"},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")})
            } }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding),contentAlignment=Alignment.TopCenter) {
                when(tab) {
                    "models" -> ModernModelsScreen()
                    "monitor" -> ModernMonitorScreen()
                    "settings" -> ModernSettingsScreen()
                    else -> ModernChatScreen(state,runtime,chats,onModels={tab="models"},onMonitor={tab="monitor"},onMedia={tab="media"})
                }
            }
        }
    }
}

@Composable
private fun ModernChatScreen(state:ChatState,runtime:CoordinatorState,controller:ChatController,onModels:()->Unit,onMonitor:()->Unit,onMedia:()->Unit) {
    var draft by rememberSaveable(state.selectedId) { mutableStateOf("") }
    var showOptions by remember{mutableStateOf(false)}
    if(showOptions) ChatOptionsDialog(state,controller){showOptions=false}
    val list=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val messages=state.current?.messages.orEmpty()
    val nearBottom by remember { derivedStateOf { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let {
        it>=list.layoutInfo.totalItemsCount-2 } ?: true } }
    LaunchedEffect(messages.lastOrNull()?.text?.length,messages.size) {
        if(messages.isNotEmpty() && nearBottom) list.scrollToItem(messages.lastIndex)
    }
    val canSend=(runtime is CoordinatorState.Ready || state.current?.options?.onlineProfileId!=null || state.current?.options?.routeId!=null) && !state.running && draft.isNotBlank() && draft.length<=12000
    val send={ if(canSend) {controller.send(draft);draft=""} }
    Column(Modifier.fillMaxSize().widthIn(max=T.contentMax)) {
        if(messages.isEmpty()) Column(Modifier.weight(1f).fillMaxWidth().padding(T.section),verticalArrangement=Arrangement.Center,
            horizontalAlignment=Alignment.CenterHorizontally) {
            Icon(Icons.Default.AutoAwesome,null,Modifier.size(T.touch),tint=MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(T.section))
            Text(stringResource(R.string.modern_greeting),style=MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(T.small))
            Text(stringResource(R.string.modern_greeting_body),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(T.section))
            listOf(R.string.modern_suggestion_explain,R.string.modern_suggestion_code,R.string.modern_suggestion_plan).forEach { id ->
                val text=stringResource(id)
                OutlinedButton(onClick={draft=text},Modifier.fillMaxWidth().padding(vertical=T.tiny)) {Text(text)}
            }
            if(runtime !is CoordinatorState.Ready) FilledTonalButton(onClick=onModels,Modifier.padding(top=T.large)) {
                Text(stringResource(R.string.modern_setup_model)) }
        } else Box(Modifier.weight(1f)) {
            LazyColumn(state=list,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(T.large),verticalArrangement=Arrangement.spacedBy(T.section)) {
                items(messages,key={it.id}) { message ->
                    val user=message.role=="user"
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=if(user) Arrangement.End else Arrangement.Start) {
                        if(user) Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier=Modifier.widthIn(max=T.bubbleMax)) {
                            SelectionContainer { Text(message.text,Modifier.padding(T.large),style=MaterialTheme.typography.bodyLarge) }
                        } else Column(Modifier.widthIn(max=T.bubbleMax)) {
                            Text("Meshlit",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(T.small))
                            if(message.text.isEmpty() && state.running) LinearProgressIndicator(Modifier.fillMaxWidth())
                            else MessageBody(message.text)
                            if(message.text.isNotEmpty()) {
                                val clipboard=LocalClipboardManager.current
                                IconButton(onClick={clipboard.setText(AnnotatedString(message.text))}) {Icon(Icons.Default.ContentCopy,stringResource(R.string.modern_copy))}
                            }
                        }
                    }
                }
            }
            if(!nearBottom && messages.isNotEmpty()) SmallFloatingActionButton(onClick={scope.launch{list.animateScrollToItem(messages.lastIndex)}},
                modifier=Modifier.align(Alignment.BottomCenter).padding(T.small)) {Icon(Icons.Default.ArrowDownward,stringResource(R.string.modern_latest))}
        }
        state.current?.usageNote?.let{Text(it,Modifier.padding(horizontal=T.large),style=MaterialTheme.typography.labelSmall)}
        state.error?.let { Box(Modifier.padding(horizontal=T.large)) {ErrorCard(it){controller.clearError()}} }
        if(runtime is CoordinatorState.Error) Box(Modifier.padding(horizontal=T.large)) {ErrorCard(runtime.message,onModels)}
        Column(Modifier.fillMaxWidth().imePadding().padding(T.large)) {
            Surface(shape=MaterialTheme.shapes.extraLarge,color=MaterialTheme.colorScheme.surfaceContainerLow,
                border=androidx.compose.foundation.BorderStroke(TinyBorder,MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.padding(T.small),verticalAlignment=Alignment.Bottom) {
                    ChatAttachmentActions(!state.running,onText={text->if(draft.length+text.length+2<=12000) draft=if(draft.isBlank()) text else "$draft\n\n$text" else controller.attachmentError("Attachment plus draft exceeds 12000 characters")},onMedia=onMedia)
                    OutlinedTextField(value=draft,onValueChange={draft=it},modifier=Modifier.weight(1f),
                        placeholder={Text(stringResource(R.string.modern_message))},maxLines=6,
                        keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={send()}),
                        colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedBorderColor=androidx.compose.ui.graphics.Color.Transparent))
                    IconButton(enabled=state.running || canSend,onClick={if(state.running) controller.stop() else send()},modifier=Modifier.size(T.touch)) {
                        Icon(if(state.running) Icons.Default.StopCircle else Icons.AutoMirrored.Filled.Send,
                            stringResource(if(state.running) R.string.modern_stop else R.string.modern_send),tint=MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center) {
                TextButton(onClick={showOptions=true},enabled=!state.running){Text(if(state.current?.options?.routeId!=null) "Router · options" else if(state.current?.options?.onlineProfileId!=null) "Online · options" else "Offline · options",style=MaterialTheme.typography.labelSmall)}
                TextButton(onClick=onModels) {Text(stringResource(R.string.modern_models),style=MaterialTheme.typography.labelSmall)}
                TextButton(onClick=onMonitor) {Text(stringResource(R.string.modern_monitor),style=MaterialTheme.typography.labelSmall)}
            }
        }
    }
}

/** Fenced code stays selectable, monospaced and copyable. Plain response text
 * stays selectable; no HTML/WebView rendering of untrusted model output. */
@Composable private fun MessageBody(text:String) {
    val chunks=remember(text) { text.split("```") }
    chunks.forEachIndexed { index,chunk ->
        if(index%2==1) {
            val code=chunk.substringAfter('\n',chunk)
            val clipboard=LocalClipboardManager.current
            Surface(shape=MaterialTheme.shapes.medium,color=MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().padding(T.medium)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(chunk.substringBefore('\n').take(30),style=MaterialTheme.typography.labelSmall)
                        IconButton(onClick={clipboard.setText(AnnotatedString(code))}) {Icon(Icons.Default.ContentCopy,stringResource(R.string.modern_copy))}
                    }
                    SelectionContainer {Text(code,fontFamily=FontFamily.Monospace,style=MaterialTheme.typography.bodyMedium)}
                }
            }
        } else SelectionContainer {Text(chunk,style=MaterialTheme.typography.bodyLarge)}
    }
}
private val TinyBorder get()=com.meshlit.ui.theme.ChatTokens.border
