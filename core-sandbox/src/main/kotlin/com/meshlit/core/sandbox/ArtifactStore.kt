package com.meshlit.core.sandbox

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Imports individual trusted artifacts; never executes or unpacks an archive. */
class ArtifactStore(private val directory: File) {
    suspend fun verify(file: File, sha256: String): Boolean = withContext(Dispatchers.IO) {
        require(sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Expected a SHA-256 digest" }
        if (!file.isFile) return@withContext false
        digest(file).equals(sha256, ignoreCase = true)
    }

    suspend fun install(source: File, name: String, sha256: String, replace: Boolean = false): File =
        withContext(Dispatchers.IO) {
            require(name.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{0,63}")) && name != "." && name != "..")
            require(sha256.matches(Regex("[a-fA-F0-9]{64}")))
            require(source.isFile && source.length() in 1..(8L * 1024 * 1024 * 1024)) { "Invalid artifact size" }
            require(directory.isDirectory || directory.mkdirs()) { "Cannot create artifact directory" }
            val target = File(directory, name)
            require(!target.exists() || replace) { "Artifact exists; pass --replace to repair it" }
            require(source.canonicalFile != target.canonicalFile) { "Source and destination must differ" }
            require(directory.usableSpace > source.length() + 64L * 1024 * 1024) { "Insufficient free storage" }
            val staging = File.createTempFile("import-", ".part", directory)
            try {
                source.inputStream().use { input ->
                    staging.outputStream().use { output ->
                        val buffer = ByteArray(65536)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 8L * 1024 * 1024 * 1024) { "Artifact grew beyond limit" }
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }
                require(digest(staging).equals(sha256, ignoreCase = true)) { "Artifact checksum mismatch" }
                require(staging.renameTo(target)) { "Atomic artifact replacement failed" }
                target
            } finally {
                staging.delete()
            }
        }

    private suspend fun digest(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) {
                coroutineContext.ensureActive()
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
