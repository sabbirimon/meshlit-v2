package com.meshlit.desktop

import java.net.InetAddress
import java.net.ServerSocket
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit

internal object DesktopStarter {
    const val filename = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
    const val size = 1117320736L
    const val sha256 = "6a1a2eb6d15622bf3c96857206351ba97e1af16c30d7a74ee38970e434e9407e"
    const val label = "Qwen2.5 1.5B Instruct · Q4_K_M"
    const val alias = "meshlit-local"
    const val prompt = "You are Meshlit, a helpful assistant running locally on the user's Mac. " +
        "Give clear, accurate answers with useful detail. Use Markdown headings, lists and code blocks " +
        "where helpful. Say when you do not know. You cannot access the Internet or execute tools."
}

internal data class LocalSession(val endpoint: HostEndpoint, val token: String)

/** Owns only the bundled CPU process. No downloads, persistent listener or remote fallback. */
internal class LocalEngine(private val resources: Path = Path.of(
    System.getProperty("compose.application.resources.dir", "desktopApp/build/local-resources")
), registerShutdownHook: Boolean = true) : AutoCloseable {
    private val lock = Any()
    @Volatile private var revision = 0L
    private var process: Process? = null
    private var privateDirectory: Path? = null
    private val hook = Thread({ stop() }, "meshlit-local-shutdown")
    init { if (registerShutdownHook) Runtime.getRuntime().addShutdownHook(hook) }
    fun available() = Files.isExecutable(resources.resolve("local/llama-server")) &&
        Files.isRegularFile(resources.resolve("models/${DesktopStarter.filename}"))

    fun start(customModel: Path? = null): LocalSession {
        stop()
        val expectedRevision = synchronized(lock) { revision }
        val server = resources.resolve("local/llama-server").toAbsolutePath()
        check(Files.isExecutable(server)) { "Packaged local engine is unavailable" }
        val model = (customModel ?: resources.resolve("models/${DesktopStarter.filename}")).toAbsolutePath()
        validateModel(model, customModel == null) { checkCurrent(expectedRevision) }
        val engineManifest = boundedJson(Files.readString(resources.resolve("local/engine.json")))
        val serverHash = engineManifest["sha256"]?.toString()?.trim('"') ?: error("Missing engine checksum")
        check(hash(server) { checkCurrent(expectedRevision) } == serverHash) { "Local engine checksum mismatch" }
        val token = ByteArray(32).also { SecureRandom().nextBytes(it) }
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
        val port = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        val host = HostEndpoint.parse("http://127.0.0.1:$port/v1")
        synchronized(lock) {
            checkCurrent(expectedRevision)
            val directory = Files.createTempDirectory("meshlit-local-",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
            privateDirectory = directory
            val secret = Files.createFile(directory.resolve("access-key"),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
            Files.writeString(secret, token + "\n")
            try {
                process = ProcessBuilder(server.toString(), "--model", model.toString(),
                    "--host", "127.0.0.1", "--port", port.toString(), "--api-key-file", secret.toString(),
                    "--alias", DesktopStarter.alias, "--ctx-size", "4096", "--parallel", "1",
                    "--threads", Runtime.getRuntime().availableProcessors().coerceIn(1, 4).toString(),
                    "--n-gpu-layers", "0", "--offline", "--no-webui")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start()
            } catch (error: Exception) { stop(); throw error }
        }
        val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
            .followRedirects(HttpClient.Redirect.NEVER).build()
        try {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
            while (System.nanoTime() < deadline) {
                checkCurrent(expectedRevision)
                check(synchronized(lock) { process?.isAlive == true }) { "Local process stopped during loading" }
                val ready = runCatching {
                    val request = HttpRequest.newBuilder(host.route("models")).timeout(Duration.ofSeconds(2))
                        .header("Authorization", "Bearer $token").GET().build()
                    val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                    response.statusCode() == 200 && response.body().length <= 262144 &&
                        HostClient(host, token).use { DesktopStarter.alias in it.models() }
                }.getOrDefault(false)
                if (ready) { checkCurrent(expectedRevision); return LocalSession(host, token) }
                Thread.sleep(150)
            }
            error("Local model loading exceeded two minutes")
        } catch (error: Exception) {
            synchronized(lock) { if (revision == expectedRevision) stop() }
            throw error
        } finally { http.shutdownNow() }
    }

    private fun checkCurrent(expected: Long) {
        if (revision != expected || Thread.currentThread().isInterrupted) throw CancellationException("Local load cancelled")
    }
    fun stop() {
        val owned = synchronized(lock) {
            revision++
            val result = process to privateDirectory
            process = null; privateDirectory = null
            result
        }
        owned.first?.destroy()
        owned.second?.let { directory ->
            runCatching { Files.deleteIfExists(directory.resolve("access-key")); Files.deleteIfExists(directory) }
        }
        owned.first?.let { child -> Thread({
            if (!child.waitFor(2, TimeUnit.SECONDS)) { child.destroyForcibly(); child.waitFor(2, TimeUnit.SECONDS) }
        }, "meshlit-local-stop").apply { isDaemon = true; start() } }
    }
    override fun close() {
        stop()
        runCatching { Runtime.getRuntime().removeShutdownHook(hook) }
    }
}

internal fun validateModel(path: Path, builtin: Boolean, checkCancelled: () -> Unit = {}) {
    require(Files.isRegularFile(path) && Files.size(path) in 16..(32L * 1024 * 1024 * 1024))
    Files.newInputStream(path).use { require(it.readNBytes(4).contentEquals(byteArrayOf(71, 71, 85, 70))) { "Not GGUF" } }
    if (builtin) {
        require(Files.size(path) == DesktopStarter.size)
        require(hash(path, checkCancelled) == DesktopStarter.sha256) { "Starter model checksum mismatch" }
    }
    checkCancelled()
}
private fun hash(path: Path, checkCancelled: () -> Unit): String {
    val digest = MessageDigest.getInstance("SHA-256")
    Files.newInputStream(path).use { input ->
        val buffer = ByteArray(1024 * 1024)
        while (true) { checkCancelled(); val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
