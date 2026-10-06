package com.meshlit.ui.modern

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val providers=koinInject<com.meshlit.providers.OnlineProviders>()
    val profiles by providers.profiles.collectAsStateWithLifecycle()
    val selectedProfile=profiles.firstOrNull{it.id==state.current?.options?.onlineProfileId}
    val coordinator=koinInject<InferenceCoordinator>()
    val runtime by coordinator.state.collectAsStateWithLifecycle()
    val drawer=rememberDrawerState(DrawerValue.Closed)
    val scope=rememberCoroutineScope()
    var tab by rememberSaveable { mutableStateOf("chat") }
    var settingsDestination by remember { mutableStateOf<String?>(null) }
    var legacy by rememberSaveable { mutableStateOf(false) }
    var permissionSetup by rememberSaveable{mutableStateOf(false)}
    com.meshlit.permissions.FirstLaunchPermissionSetup{permissionSetup=true}
    if(permissionSetup){com.meshlit.permissions.PermissionSetupScreen{permissionSetup=false};return}
    LaunchedEffect(chats) { chats.ready.await() }
    BackHandler(tab!="chat"){tab="chat"}
    if(tab=="media"){MediaGenerationScreen{tab="chat"};return}
    if(tab=="vision"){
        Scaffold(topBar={TopAppBar(title={Text("Photo and camera")},navigationIcon={IconButton(onClick={tab="chat"}){
            Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back to chat")}})}){padding->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)){
                com.meshlit.ui.screens.VisionScreen({tab="chat"},omitHeader=true)
            }
        };return
    }
    if(tab=="tasks"){TaskManagerScreen{tab="chat"};return}
    if(tab=="ide"){com.meshlit.ide.CodeWorkspaceScreen{tab="chat"};return}
    if(tab=="help"){HelpTutorialScreen{tab="chat"};return}
    if(tab=="cloud"){CloudManagementScreen{tab="chat"};return}
    if(tab=="devices"){ModernNetworkScreen{tab="chat"};return}
    if(legacy) {
        BackHandler { legacy=false }
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick={legacy=false}) { Text(stringResource(R.string.modern_back_chat)) }
            Box(Modifier.weight(1f)) { com.meshlit.ui.LegacyMeshlitApp() }
        }
        return
    }
    val glass=com.meshlit.ui.theme.LocalMeshlitThemeConfig.current.surfaceStyle==com.meshlit.ui.theme.SurfaceStyle.GLASS
    val colors=MaterialTheme.colorScheme
    ModalNavigationDrawer(modifier=Modifier.background(Brush.linearGradient(if(glass) listOf(colors.primaryContainer,colors.background,colors.secondaryContainer) else listOf(colors.background,colors.background))),drawerState=drawer,drawerContent={
        ModalDrawerSheet(Modifier.width(T.sidebar)) {
          LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=T.large)) {
            item {Column(Modifier.padding(T.large)) {
                Text("Meshlit",style=MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.modern_private_ai),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(T.section))
                FilledTonalButton(enabled=!state.running,onClick={chats.newChat();tab="chat";scope.launch{drawer.close()}},modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add,null);Spacer(Modifier.width(T.small));Text(stringResource(R.string.modern_new_chat)) }
            }
            }
            item {Text(stringResource(R.string.modern_recent),Modifier.padding(horizontal=T.large,vertical=T.small),style=MaterialTheme.typography.labelMedium)}
                items(state.conversations,key={"chat:${it.id}"}) { conversation ->
                    NavigationDrawerItem(label={Text(conversation.title,maxLines=2)},selected=state.selectedId==conversation.id,
                        onClick={chats.select(conversation.id);tab="chat";scope.launch{drawer.close()}},
                        badge={IconButton(enabled=!state.running,onClick={chats.delete(conversation.id)}) {
                            Icon(Icons.Default.Delete,stringResource(R.string.modern_delete_chat)) }})
                }
            item {NavigationDrawerItem(label={Text(stringResource(R.string.modern_models))},selected=tab=="models",onClick={tab="models";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Storage,null)})}
            item {NavigationDrawerItem(label={Text(stringResource(R.string.modern_monitor))},selected=tab=="monitor",onClick={tab="monitor";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Insights,null)})}
            item {NavigationDrawerItem(label={Text("Devices and clusters")},selected=tab=="devices",onClick={tab="devices";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Devices,null)})}
            item {NavigationDrawerItem(label={Text("Task manager")},selected=tab=="tasks",onClick={tab="tasks";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Checklist,null)})}
            item {NavigationDrawerItem(label={Text("Code workspace")},selected=tab=="ide",onClick={tab="ide";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Code,null)})}
            item {NavigationDrawerItem(label={Text("Cloud and credentials")},selected=tab=="cloud",onClick={tab="cloud";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Cloud,null)})}
            item {NavigationDrawerItem(label={Text("Guide and tutorial")},selected=tab=="help",onClick={tab="help";scope.launch{drawer.close()}},icon={Icon(Icons.Default.HelpOutline,null)})}
            item {NavigationDrawerItem(label={Text("Settings")},selected=tab=="settings",onClick={tab="settings";scope.launch{drawer.close()}},icon={Icon(Icons.Default.Settings,null)})}
            item {NavigationDrawerItem(label={Text(stringResource(R.string.modern_tools))},selected=false,onClick={legacy=true;scope.launch{drawer.close()}},icon={Icon(Icons.Default.Build,null)})}
          }
        }
    }) {
        Scaffold(
            containerColor=if(glass) Color.Transparent else colors.background,
            // Settings owns its toolbar; never stack an app toolbar above it.
            contentWindowInsets=if(tab=="settings") WindowInsets(0,0,0,0) else ScaffoldDefaults.contentWindowInsets,
            topBar={if(tab!="settings") TopAppBar(
                title={if(tab=="chat") Column(Modifier.heightIn(min=T.touch).clickable(onClickLabel="Choose model",onClick={tab="models"}),verticalArrangement=Arrangement.Center) {
                    Text("Meshlit",maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(if(state.current?.options?.routeId!=null) "Model router" else if(state.current?.options?.onlineProfileId!=null)
                            selectedProfile?.let{"${it.name} · ${it.model}"} ?: "Online model" else when(val current=runtime) {
                            is CoordinatorState.Ready -> current.model.modelName
                            is CoordinatorState.Loading -> stringResource(R.string.modern_loading)
                            is CoordinatorState.Generating -> stringResource(R.string.modern_generating)
                            CoordinatorState.Starting -> stringResource(R.string.modern_starting)
                            else -> stringResource(R.string.modern_choose_model)
                        },maxLines=1,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.labelSmall,
                            color=colors.onSurfaceVariant,modifier=Modifier.weight(1f,fill=false))
                        Icon(Icons.Default.ExpandMore,null,Modifier.size(16.dp),tint=colors.onSurfaceVariant)
                    }
                } else Text(stringResource(if(tab=="models") R.string.modern_models else R.string.modern_monitor),
                    maxLines=1,style=MaterialTheme.typography.titleLarge)},
                navigationIcon={IconButton(onClick={scope.launch{drawer.open()}}) {Icon(Icons.Default.Menu,stringResource(R.string.modern_menu))}},
                actions={if(tab=="chat") IconButton(enabled=!state.running,onClick={chats.newChat()}) {
                    Icon(Icons.Default.EditNote,stringResource(R.string.modern_new_chat))}},
                colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent))},
            bottomBar={ if(tab!="chat" && (tab!="settings" || settingsDestination==null)) NavigationBar(containerColor=colors.surfaceContainerLow,tonalElevation=0.dp) {
                NavigationBarItem(tab=="chat",{tab="chat"},icon={Icon(Icons.Default.ChatBubbleOutline,null)},label={Text(stringResource(R.string.modern_chat))})
                NavigationBarItem(tab=="models",{tab="models"},icon={Icon(Icons.Default.Storage,null)},label={Text(stringResource(R.string.modern_models))})
                NavigationBarItem(tab=="monitor",{tab="monitor"},icon={Icon(Icons.Default.Insights,null)},label={Text(stringResource(R.string.modern_monitor))})
                NavigationBarItem(tab=="settings",{tab="settings"},icon={Icon(Icons.Default.Settings,null)},label={Text("Settings")})
            } }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),contentAlignment=Alignment.TopCenter) {
                when(tab) {
                    "models" -> ModernModelsScreen()
                    "monitor" -> ModernMonitorScreen()
                    "settings" -> ModernSettingsScreen(onMenu={scope.launch{drawer.open()}},onDestinationChanged={settingsDestination=it})
                    else -> ModernChatScreen(state,runtime,chats,onModels={tab="models"},onMedia={tab="media"},onVision={tab="vision"})
                }
            }
        }
    }
}

@Composable
private fun ModernChatScreen(state:ChatState,runtime:CoordinatorState,controller:ChatController,onModels:()->Unit,onMedia:()->Unit,onVision:()->Unit) {
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
    val colors=MaterialTheme.colorScheme
    val theme=com.meshlit.ui.theme.LocalMeshlitThemeConfig.current
    val light=colors.background.red+colors.background.green+colors.background.blue>1.5f
    // Static, inexpensive glow: no blur, animation or rendered model placeholders.
    val referencePalette=!theme.dynamicColors && theme.accentHue==com.meshlit.ui.theme.AccentHue.SKY && theme.customPalette is com.meshlit.ui.theme.CustomPalette.None
    val keyboardVisible=WindowInsets.ime.getBottom(LocalDensity.current)>0
    val glow=when {
        theme.highContrast->colors.background
        referencePalette && light->Color(0xFF81D9F5)
        referencePalette->Color(0xFF18254C)
        else->androidx.compose.ui.graphics.lerp(colors.background,colors.primary,0.2f)
    }
    Box(Modifier.fillMaxSize().imePadding().background(Brush.verticalGradient(0f to colors.background,0.62f to colors.background,1f to glow))) {
    Column(Modifier.fillMaxSize().widthIn(max=T.contentMax).align(Alignment.TopCenter)) {
        if(messages.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth()) {
            if(!keyboardVisible) Column(Modifier.align(Alignment.Center).padding(horizontal=T.section),
                horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(Icons.Default.AutoAwesome,null,Modifier.size(32.dp),tint=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(T.large))
                Text(stringResource(R.string.modern_greeting),style=MaterialTheme.typography.headlineSmall,
                    textAlign=TextAlign.Center,modifier=Modifier.widthIn(max=280.dp))
            }
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
                                MessageActions(message.text)
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
        Column(Modifier.fillMaxWidth().padding(horizontal=T.large,vertical=if(keyboardVisible) T.small else T.medium)) {
            Surface(shape=RoundedCornerShape(32.dp),color=MaterialTheme.colorScheme.surface.copy(alpha=0.97f),
                shadowElevation=2.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal=T.tiny,vertical=T.tiny),verticalAlignment=Alignment.CenterVertically) {
                    ChatAttachmentActions(!state.running,onText={text->if(draft.length+text.length+2<=12000) draft=if(draft.isBlank()) text else "$draft\n\n$text" else controller.attachmentError("Attachment plus draft exceeds 12000 characters")},onMedia=onMedia,onVision=onVision,onOptions={showOptions=true})
                    TextField(value=draft,onValueChange={draft=it},modifier=Modifier.weight(1f),
                        placeholder={Text(stringResource(R.string.modern_message),style=MaterialTheme.typography.bodyLarge)},maxLines=6,
                        keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={send()}),
                        colors=TextFieldDefaults.colors(focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,
                            focusedIndicatorColor=Color.Transparent,unfocusedIndicatorColor=Color.Transparent))
                    IconButton(onClick=onVision,enabled=!state.running) {
                        Icon(Icons.Default.Image,"Photo and camera input",tint=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledIconButton(enabled=state.running || canSend,onClick={if(state.running) controller.stop() else send()},modifier=Modifier.size(T.touch),
                        colors=IconButtonDefaults.filledIconButtonColors(containerColor=colors.primary,contentColor=colors.onPrimary)) {
                        Icon(if(state.running) Icons.Default.Stop else Icons.Default.ArrowUpward,
                            stringResource(if(state.running) R.string.modern_stop else R.string.modern_send))
                    }
                }
            }
            val mode=if(state.current?.options?.routeId!=null) "Model router" else if(state.current?.options?.onlineProfileId!=null) "Online provider" else "Offline model"
            if(!keyboardVisible) Text(mode,Modifier.fillMaxWidth().padding(top=T.small),textAlign=TextAlign.Center,
                style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
