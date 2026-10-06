package com.meshlit.ui.modern

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlit.observability.AppLoggerFactory.appLogger as logger
import com.meshlit.core.inference.models.EnergyEstimate
import com.meshlit.power.*
import com.meshlit.devices.*
import com.meshlit.di.koinInject
import com.meshlit.control.AgentBackend
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable fun PowerCostsScreen(onBack:()->Unit) {
    val context=LocalContext.current;val repository=remember{PowerRepository(context)};val scope=rememberCoroutineScope()
    var reading by remember{mutableStateOf<PowerReading?>(null)}
    var policy by remember{mutableStateOf(repository.policy())};var energy by remember{mutableDoubleStateOf(0.0)};var observedSeconds by remember{mutableLongStateOf(0)}
    val samples=remember{mutableListOf<PowerReading>()}
    var price by remember{mutableStateOf(repository.pricePerKwh()?.toString().orEmpty())}
    var efficiency by remember{mutableStateOf(repository.efficiency().toString())};var currency by remember{mutableStateOf(repository.currency())}
    var message by remember{mutableStateOf<String?>(null)}
    LaunchedEffect(Unit){var previous:PowerReading?=null
        while(isActive){val next=repository.reading();previous?.let{old->EnergyEstimate.dischargeWh(old.observedAtMs,next.observedAtMs,old.signedBatteryWatts,next.signedBatteryWatts,next.charging!=false)?.let{energy+=it;observedSeconds+=(next.observedAtMs-old.observedAtMs)/1000}}
            reading=next;previous=next;samples.add(next);if(samples.size>720) samples.removeAt(0);delay(5000)}
    }
    val exporter=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-ndjson")){uri->if(uri!=null){val snapshot=samples.toList();scope.launch{try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"wt")?.bufferedWriter()?.use{writer->snapshot.forEach{writer.write(Json.encodeToString(it));writer.newLine()}} ?: error("Cannot open export")};message="Exported ${snapshot.size} observed samples"}catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}}}}
    fun savePolicy(next:PowerPolicy){try{repository.save(next);policy=next;message="Saved; transfers may pause within 5 seconds. Resume manually when ready."}catch(e:Exception){message=e.message}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=onBack){Text("Back to settings")};Text("Power & costs",style=MaterialTheme.typography.headlineSmall)}
        item{Card{Column(Modifier.padding(16.dp)){
            Text("Battery: ${reading?.batteryPercent?.let{"$it%"} ?: "unknown"} · ${when(reading?.charging){true->"charging";false->"not charging";null->"unknown status"}}")
            Text("Current: ${reading?.currentUa?.let{"$it µA"} ?: "unavailable"} · voltage: ${reading?.voltageMv?.let{"$it mV"} ?: "unavailable"}")
            Text("Signed battery power: ${reading?.signedBatteryWatts?.let{"%.3f W".format(it)} ?: "unavailable"}")
            Text("Temperature: ${reading?.temperatureC?.let{"$it °C"} ?: "unknown"} · thermal status: ${reading?.thermalStatus ?: "unsupported"} · power saver: ${reading?.powerSave ?: "unknown"}")
            Text("Observed discharge: ${"%.6f".format(energy)} Wh over $observedSeconds valid seconds since opening this screen.")
            Text("Battery-side whole-device estimate. Charging, missing sensors and gaps over 15 seconds are excluded. Zero may mean no valid discharge samples. This does not isolate Meshlit or measure USB output / wall-plug input.",style=MaterialTheme.typography.bodySmall)
        }}}
        item{Text("Workload policy",style=MaterialTheme.typography.titleMedium)
            Text("Minimum battery: ${policy.minimumBattery}%");Slider(policy.minimumBattery.toFloat(),{savePolicy(policy.copy(minimumBattery=it.toInt()))},valueRange=5f..80f)
            Row{Text("Pause downloads below threshold",Modifier.weight(1f));Switch(policy.pauseLowBatteryDownloads,{savePolicy(policy.copy(pauseLowBatteryDownloads=it))})}
            Row{Text("Downloads on unmetered network only",Modifier.weight(1f));Switch(policy.unmeteredDownloadsOnly,{savePolicy(policy.copy(unmeteredDownloadsOnly=it))})}
            Row{Text("Require charging for new model loads",Modifier.weight(1f));Switch(policy.chargingForNewLoads,{savePolicy(policy.copy(chargingForNewLoads=it))})}
            Text("Running inference is not forcibly interrupted by these controls. USB power delivery and bypass charging depend on device firmware.")
            OutlinedButton(onClick={BatteryOptimizationHelper(context).openBatteryOptimizationSettings()}){Text("Android battery restrictions")}}
        item{Text("Electricity estimate",style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(price,{price=it},label={Text("Your electricity price / kWh")});OutlinedTextField(efficiency,{efficiency=it},label={Text("Charging efficiency assumption (0.1–1.0)")});OutlinedTextField(currency,{currency=it.uppercase()},label={Text("Currency")})
            Button(onClick={try{repository.saveCost(price.takeIf{it.isNotBlank()}?.let{it.toDoubleOrNull() ?: error("Invalid electricity price")},efficiency.toDoubleOrNull() ?: error("Invalid efficiency"),currency);message="Cost assumptions saved"}catch(e:Exception){message=e.message}}){Text("Save assumptions")}
            val estimate=repository.pricePerKwh()?.let{EnergyEstimate.cost(energy,it,repository.efficiency())}
            Text(estimate?.let{"Observed discharge recharge estimate: ${"%.8f".format(it)} ${repository.currency()}"} ?: "Cost unknown; enter your tariff to estimate.")
            Text("Cloud token usage and per-response cost appear in chat when reported by the provider and both token prices are configured. Provider invoices remain authoritative.")}
        item{OutlinedButton(onClick={exporter.launch("meshlit-power-${System.currentTimeMillis()}.jsonl")}){Text("Export observed power samples")};message?.let{Text(it)}}
    }
}

@Composable fun ExternalDevicesScreen(onBack:()->Unit) {
    val context=LocalContext.current;val repository=remember{ExternalDevices(context)}
    var status by remember{mutableStateOf<ExternalDeviceStatus?>(null)};var message by remember{mutableStateOf<String?>(null)}
    val folder=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null) try{
        context.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        message="Folder granted; use Files for storage access and Models to import GGUF files.";status=repository.status()
    }catch(e:Exception){message=e.message}}
    LaunchedEffect(Unit){var paths:Set<String>?=null;while(isActive){try{val next=withContext(Dispatchers.IO){repository.status()};val newPaths=next.devices.map{it.path}.toSet();if(paths!=null && newPaths!=paths) logger("ExternalDevices").info("hardware.usb.changed","USB topology changed",mapOf("attached" to (newPaths-paths!!).size.toString(),"detached" to (paths!!-newPaths).size.toString()));paths=newPaths;status=next}catch(e:CancellationException){throw e}catch(e:Exception){message="Device probe failed: ${e.javaClass.simpleName}"};delay(3000)}}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=onBack){Text("Back to settings")};Text("External devices / OTG",style=MaterialTheme.typography.headlineSmall)
            Text("USB host support: ${status?.usbHostSupported ?: "probing"}. Detected ${status?.devices?.size ?: 0} USB devices. Refreshes every 3 seconds while open.")}
        items(status?.devices.orEmpty(),key={it.path}){device->Card{Column(Modifier.padding(16.dp)){
            Text("USB %04x:%04x".format(device.vendorId,device.productId),style=MaterialTheme.typography.titleMedium);Text(device.kind);Text("${device.endpoints} endpoints · classes ${device.classes} · access ${if(device.permission) "granted" else "not granted"}");Text(device.executionSupport,style=MaterialTheme.typography.bodySmall)
            OutlinedButton(enabled=!device.permission,onClick={try{repository.requestPermission(device.path);message="Android permission requested. Access status updates after the prompt."}catch(e:Exception){message=e.message}}){Text("Request device access")}
        }}}
        item{Text("Android storage volumes",style=MaterialTheme.typography.titleMedium)}
        items(status?.volumes.orEmpty()){volume->Text("${volume.description} · ${volume.state} · ${if(volume.removable) "removable" else "built in"}")}
        item{Button(onClick={folder.launch(null)}){Text("Grant an external storage folder")};Text("${status?.grantedTrees?.size ?: 0} persisted storage grants. Android must mount a drive before its folder can be selected. No raw disk writes or automatic formatting.")
            Text("GPU, NPU, FPGA, serial adapters and Ethernet require an actual driver/plugin. USB authorization alone does not supply one. Networked accelerators can join as authenticated peers after runtime capability verification.")
            message?.let{Text(it)}}
    }
}

@Composable fun AgentManagementScreen(onBack:()->Unit) {
    val backend=koinInject<AgentBackend>();val jobs by backend.controller.jobs.collectAsStateWithLifecycle()
    val registry=koinInject<com.meshlit.core.mcp.McpToolRegistry>();val scope=rememberCoroutineScope()
    var revision by remember{mutableIntStateOf(0)};var query by remember{mutableStateOf("")}
    val tools=remember(revision){registry.list()}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=onBack){Text("Back to settings")};Text("Agent management",style=MaterialTheme.typography.headlineSmall);Text("Saved permissions govern typed commands. Online access also needs opt-in on the selected provider profile. OpenClaw and Android autonomy have their own explicit controls.")}
        items(AgentBackend.Scope.entries){permission->Row{Text(permission.name,Modifier.weight(1f));Switch(backend.delegated(permission),{backend.setDelegated(permission,it);revision++})}}
        item{Text("${jobs.count{!it.terminal}} active typed agent jobs · ${tools.size} registered MCP tools")
            OutlinedButton(onClick={scope.launch{backend.controller.ready.await();backend.controller.jobs.value.filter{!it.terminal}.forEach{backend.controller.cancel(it.command.requestId)}}}){Text("Cancel active typed agent jobs")}
            OutlinedTextField(query,{query=it},label={Text("Filter registered tools")});TextButton(onClick={revision++}){Text("Refresh registry")}}
        items(tools.filter{"${it.name} ${it.description}".contains(query,true)},key={it.name}){tool->Text("${tool.name}\n${tool.description}",style=MaterialTheme.typography.bodySmall)}
    }
}
