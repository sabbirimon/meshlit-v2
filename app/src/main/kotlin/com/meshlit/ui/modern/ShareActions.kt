package com.meshlit.ui.modern

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.FileProvider
import java.io.File

/** Human-initiated Android chooser; never publishes or sends in the background. */
internal fun shareGeneratedFile(context:Context,file:File) {
    val root=File(context.filesDir,"generated-media").canonicalFile
    require(file.canonicalFile.parentFile==root && file.isFile){"Only saved generated media can be shared"}
    val mime=when(file.extension.lowercase()){"png"->"image/png";"wav"->"audio/wav";"mp4"->"video/mp4";else->"application/octet-stream"}
    val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
    val send=Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM,uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData=android.content.ClipData.newUri(context.contentResolver,"Meshlit output",uri)
    context.startActivity(Intent.createChooser(send,"Share output"))
}

@Composable internal fun MessageActions(text:String) {
    val clipboard=LocalClipboardManager.current
    val context=LocalContext.current
    var error by remember{mutableStateOf<String?>(null)}
    Row {
        IconButton(onClick={clipboard.setText(AnnotatedString(text))}){Icon(Icons.Default.ContentCopy,"Copy response")}
        IconButton(onClick={try {
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT,text),"Share response"))
        }catch(e:Exception){error="No sharing app is available"}}){Icon(Icons.Default.Share,"Share response")}
    }
    error?.let{AlertDialog(onDismissRequest={error=null},title={Text("Cannot share")},text={Text(it)},
        confirmButton={TextButton(onClick={error=null}){Text("OK")}})}
}
