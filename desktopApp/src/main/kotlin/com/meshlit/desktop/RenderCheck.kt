package com.meshlit.desktop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Explicit developer smoke command for the packaged JVM/native graphics library; no network. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun renderCheck(path: String) {
    System.setProperty("java.awt.headless", "true")
    ImageComposeScene(1120, 800) { Workspace() }.use { scene ->
        scene.render().use { image ->
            image.encodeToData(EncodedImageFormat.PNG)?.use { File(path).writeBytes(it.bytes) }
                ?: error("PNG encoder unavailable")
        }
    }
    println("Native desktop workspace rendered; no model or device inference tested.")
}
