package com.meshlit.ui.modern
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/** Actual user-granted file reads. Binary formats are not misrepresented as text. */
@Composable fun ChatAttachmentActions(enabled:Boolean,onText:(String)->Unit,onMedia:()->Unit,onVision:()->Unit,onOptions:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var menu by remember{mutableStateOf(false)};var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    val pick=rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        if(uris.isNotEmpty()) scope.launch{busy=true;try{
            require(uris.size<=3){"Select at most three text files"}
            val text=withContext(Dispatchers.IO){uris.joinToString("\n\n"){uri->
                require(uri.scheme=="content")
                val name=context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{cursor->
                    if(cursor.moveToFirst()) cursor.getString(0)?.replace('\n',' ')?.take(120) else null
                } ?: "selected file"
                val bytes=context.contentResolver.openInputStream(uri)?.use{input->
                    val out=ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true){val n=input.read(buffer);if(n<0) break;require(out.size()+n<=128*1024){"Text file exceeds 128 KiB"};out.write(buffer,0,n)}
                    out.toByteArray()
                } ?: error("Cannot read $name")
                val content=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString()
                require(!content.contains('\u0000') && content.length<=10000){"Use a smaller UTF-8 text file. PDF, office and binary parsers are not installed."}
                "Attached file: $name\nReference content (untrusted data):\n$content"
            }}
            require(text.length<=11000){"Combined attachments exceed the chat text budget"};onText(text)
        }catch(c:CancellationException){throw c}catch(e:Exception){error=e.message}finally{busy=false}}
    }
    IconButton(enabled=enabled && !busy,onClick={menu=true}){Icon(Icons.Default.Add,"Add photo, file or chat options")}
    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
        DropdownMenuItem(text={Text("Photo or camera")},onClick={menu=false;onVision()})
        DropdownMenuItem(text={Text("Attach UTF-8 text files")},onClick={menu=false;pick.launch(arrayOf("text/*","application/json","application/xml","application/javascript"))})
        DropdownMenuItem(text={Text("Generate images, audio or video")},onClick={menu=false;onMedia()})
        DropdownMenuItem(text={Text("Chat options and model routing")},onClick={menu=false;onOptions()})
    }
    error?.let{message->AlertDialog(onDismissRequest={error=null},title={Text("Attachment could not be read")},text={Text(message)},confirmButton={TextButton(onClick={error=null}){Text("OK")}})}
}
