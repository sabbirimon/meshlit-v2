package com.meshlit.ui.modern

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import com.meshlit.core.gpu.*
import com.meshlit.di.koinInject
import com.meshlit.hyperl.HyperLWorkbenchController
import com.meshlit.operations.OperationsControl
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HyperLScreen(back:()->Unit) {
    val control=koinInject<OperationsControl>();val policy by control.gate.policy.collectAsState()
    val controller=remember(control){HyperLWorkbenchController(control.gate)};val scope=rememberCoroutineScope()
    val clipboard=LocalClipboardManager.current
    var selected by remember{mutableStateOf("weighted_relu")};var menu by remember{mutableStateOf(false)}
    val first=remember{HyperLLibrary.recipe("weighted_relu")}
    var program by remember{mutableStateOf(HyperLCodec.json.encodeToString(first.program))}
    var inputs by remember{mutableStateOf(HyperLCodec.json.encodeToString(first.example))}
    var budget by remember{mutableStateOf("16")};var output by remember{mutableStateOf("Choose a recipe, review the inputs and run it locally.")}
    var running by remember{mutableStateOf<Job?>(null)};var sourceTarget by remember{mutableStateOf(HyperLTarget.METAL)}
    fun start(work:suspend()->String){if(running?.isActive==true)return;val job=scope.launch(start=CoroutineStart.LAZY) {
        output="Running…"
        try{output=work()}catch(e:CancellationException){output="Stopped; no result published";throw e}
        catch(e:Exception){output="Failed: ${e.message.orEmpty().take(1024)}"}
        finally{running=null}
    };running=job;job.start()}
    Scaffold(topBar={TopAppBar(title={Text("HyperL libraries")},navigationIcon={TextButton(onClick=back){Text("Back")}})}){padding->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("hyperl-list"),contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            item{Text("Twelve local preprocessing recipes · no root, VM or network required. Android execution uses the CPU reference. Metal/Vulkan exports are source for separately qualified hosts; this screen does not run a phone GPU or full AI model.")}
            if(policy.emergencyStopped)item{Text("Emergency stop is latched. Resume from Operations to allow execution.",color=MaterialTheme.colorScheme.error)}
            item{Box{OutlinedButton(onClick={menu=true},enabled=running?.isActive!=true,modifier=Modifier.testTag("hyperl-recipe")){Text(HyperLLibrary.recipe(selected).title)}
                DropdownMenu(menu,{menu=false}){HyperLLibrary.ids.forEach{id->DropdownMenuItem(text={Text(HyperLLibrary.recipe(id).title)},onClick={
                    selected=id;val r=HyperLLibrary.recipe(id);program=HyperLCodec.json.encodeToString(r.program);inputs=HyperLCodec.json.encodeToString(r.example);menu=false
                })}}};Text(HyperLLibrary.recipe(selected).purpose)}
            item{OutlinedTextField(program,{if(it.length<=65536)program=it},Modifier.fillMaxWidth().testTag("hyperl-program"),label={Text("Program JSON")},minLines=6,maxLines=14,enabled=running?.isActive!=true)}
            item{OutlinedTextField(inputs,{if(it.length<=65536)inputs=it},Modifier.fillMaxWidth().testTag("hyperl-inputs"),label={Text("Input vectors JSON")},minLines=3,maxLines=8,enabled=running?.isActive!=true)}
            item{OutlinedTextField(budget,{if(it.length<=4)budget=it},modifier=Modifier.testTag("hyperl-budget"),label={Text("Array budget · MiB (1–1024)")},singleLine=true,enabled=running?.isActive!=true)}
            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                OutlinedButton(enabled=running?.isActive!=true,onClick={start{withContext(Dispatchers.Default){val p=controller.plan(program,inputs,budget.toLong()*1024*1024);"Valid graph · estimated ${p.estimatedBytes} bytes · available ${p.headroomBytes} · admitted ${p.admitted}"}}}){Text("Validate")}
                Button(enabled=running?.isActive!=true,onClick={start{val r=controller.execute(program,inputs,budget.toLong()*1024*1024)
                    "${r.backend} · ${r.values.size} outputs · ${"%.3f".format(r.wallMs)} ms total\n"+HyperLCodec.json.encodeToString(r.values.take(256))+if(r.values.size>256)"\nPreview limited to 256 values" else ""
                }}){Text("Run CPU")}
                OutlinedButton(enabled=running?.isActive==true,onClick={running?.cancel()}){Text("Stop")}
            }}
            item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(sourceTarget==HyperLTarget.METAL,{sourceTarget=HyperLTarget.METAL},label={Text("Metal source")},enabled=running?.isActive!=true)
                FilterChip(sourceTarget==HyperLTarget.VULKAN_SPIRV,{sourceTarget=HyperLTarget.VULKAN_SPIRV},label={Text("Vulkan source")},enabled=running?.isActive!=true)
            };OutlinedButton(enabled=running?.isActive!=true,onClick={start{controller.emit(program,sourceTarget)}}){Text("Generate source")}}
            item{Text(output,modifier=Modifier.testTag("hyperl-output"),style=MaterialTheme.typography.bodySmall)}
            item{OutlinedButton(enabled=running?.isActive!=true,onClick={clipboard.setText(AnnotatedString(output))}){Text("Copy output")}}
            item{Text("Array estimates are admission checks, not reservations. Reductions are CPU only. Per-function HyperL controls and emergency stop apply to execution and source generation. Inputs remain in this screen; nothing is uploaded or delegated to agents.")}
        }
    }
}
