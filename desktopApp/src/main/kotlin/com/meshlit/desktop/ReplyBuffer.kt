package com.meshlit.desktop

/** Lossless bounded text accumulation; UI snapshots are coalesced, native token
 * metrics remain supplied by the engine. Single writer, no per-token UI blocking. */
internal class ReplyBuffer(private val clock: () -> Long = System::nanoTime) {
    private val text = StringBuilder()
    private var lastPublish: Long? = null
    fun append(chunk: String): String? {
        require(chunk.length <= 131072-text.length) { "Reply exceeds display limit" }
        text.append(chunk)
        val now = clock()
        if (lastPublish == null || now-lastPublish!! >= 33_000_000L) {
            lastPublish = now; return text.toString()
        }
        return null
    }
    fun snapshot() = text.toString()
}
