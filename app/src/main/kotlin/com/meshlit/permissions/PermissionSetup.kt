package com.meshlit.permissions
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.meshlit.ui.theme.ChatTokens as T

data class PermissionFeature(val name:String,val reason:String,val permissions:List<String>)
fun permissionFeatures()=listOf(
    PermissionFeature("Notifications","Download, generation and active-runtime status",if(Build.VERSION.SDK_INT>=33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()),
    PermissionFeature("Nearby devices","Bluetooth discovery and pairing",if(Build.VERSION.SDK_INT>=31) listOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE) else listOf(Manifest.permission.ACCESS_FINE_LOCATION)),
    PermissionFeature("Microphone","Optional voice input",listOf(Manifest.permission.RECORD_AUDIO)),
    PermissionFeature("Camera","Optional camera tools",listOf(Manifest.permission.CAMERA)),
    PermissionFeature("Location","Optional location tools",listOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PermissionSetupScreen(onBack:()->Unit){
    val context=LocalContext.current;var revision by remember{mutableIntStateOf(0)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){revision++}
    val lifecycle=androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycle){val observer=androidx.lifecycle.LifecycleEventObserver{_,event ->if(event==androidx.lifecycle.Lifecycle.Event.ON_RESUME) revision++};lifecycle.lifecycle.addObserver(observer);onDispose{lifecycle.lifecycle.removeObserver(observer)}}
    Scaffold(topBar={TopAppBar(title={Text("App permissions")},navigationIcon={IconButton(onClick=onBack){Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back")}})}){padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(T.large),verticalArrangement=Arrangement.spacedBy(T.medium)){
            item{Text("Choose what Meshlit can access",style=MaterialTheme.typography.headlineSmall)
                Text("Local text inference and the code editor need no camera, microphone or location access. File imports use Android's file picker. Grant optional features here or when you use them.")}
            permissionFeatures().forEach{feature ->item{
                val granted=remember(revision){feature.permissions.all{ContextCompat.checkSelfPermission(context,it)==PackageManager.PERMISSION_GRANTED}}
                Card{Column(Modifier.fillMaxWidth().padding(T.large)){Text(feature.name,style=MaterialTheme.typography.titleMedium);Text(feature.reason)
                    Text(if(granted) "Granted / not required on this Android version" else "Not granted")
                    if(!granted) Button(onClick={launcher.launch(feature.permissions.toTypedArray())}){Text("Request ${feature.name.lowercase()}")}
                }}
            }}
            item{OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}){Text("Accessibility for Android control")}
                OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:${context.packageName}")))}){Text("Open Android app settings")}
                Text("If Android no longer shows a permission prompt, use app settings. Accessibility and battery exemptions remain separate system choices.")}
        }
    }
}
@Composable fun FirstLaunchPermissionSetup(onSetup:()->Unit){
    val context=LocalContext.current;val prefs=remember{context.getSharedPreferences("permission-onboarding",0)}
    var show by remember{mutableStateOf(!prefs.getBoolean("seen",false))}
    fun close(){prefs.edit().putBoolean("seen",true).apply();show=false}
    if(show) AlertDialog(onDismissRequest={close()},title={Text("Set up optional permissions")},
        text={Text("You can grant notifications, nearby devices and optional voice or camera access now, or later when a feature needs it. Local text inference works without them.")},
        confirmButton={TextButton(onClick={close();onSetup()}){Text("Choose permissions")}},dismissButton={TextButton(onClick={close()}){Text("Later")}})
}
/** Feature entry points request notifications only when needed, never from a background service. */
@Composable fun rememberNotificationRequest():((()->Unit)->Unit){
    val context=LocalContext.current;var after by remember{mutableStateOf<(() -> Unit)?>(null)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){after?.invoke();after=null}
    return {action ->
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){after=action;launcher.launch(Manifest.permission.POST_NOTIFICATIONS)}else action()
    }
}
