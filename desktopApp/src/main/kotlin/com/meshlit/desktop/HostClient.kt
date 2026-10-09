package com.meshlit.desktop

import com.meshlit.workspace.ChatTurn
import com.meshlit.workspace.GenerationBudget
import com.meshlit.workspace.GenerationUsage
import kotlinx.serialization.json.*
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

/** System TLS, no redirects, no background discovery, no persisted bearer secrets. */
class HostEndpoint private constructor(val base: URI) {
    val loopback: Boolean get() = base.host in setOf("127.0.0.1", "::1", "[::1]")
    fun route(path: String): URI = URI(base.toString().trimEnd('/') + "/" + path)
    companion object {
        fun parse(text: String): HostEndpoint {
            require(text.length in 1..2048 && text.none { it.isWhitespace() || it.isISOControl() }) { "Enter an HTTPS /v1 endpoint, or HTTP on literal loopback." }
            val uri = URI(text)
            require(uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
            require(uri.scheme == "https" || (uri.scheme == "http" && uri.host in setOf("127.0.0.1", "::1", "[::1]"))) { "Remote hosts require HTTPS with a trusted certificate." }
            require(uri.port == -1 || uri.port in 1..65535)
            require(uri.rawPath == "/v1" || uri.rawPath == "/v1/") { "Endpoint must end in /v1." }
            return HostEndpoint(URI(text.trimEnd('/')))
        }
    }
}
class HostClient(private val endpoint: HostEndpoint, private val token: String) : AutoCloseable {
    init {
        require(token.length <= 8192 && token.none { it.isISOControl() })
        require(endpoint.loopback || token.isNotBlank()) { "A remote host requires a client token." }
    }

    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NEVER).build()
    private val deadline = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "meshlit-client-deadline").apply { isDaemon = true } }
    private val closed = AtomicBoolean(false)
    private val lifecycle = Any()
    @Volatile private var active: CompletableFuture<HttpResponse<InputStream>>? = null
    @Volatile private var body: InputStream? = null
    private fun request(path: String): HttpRequest.Builder {
        check(!closed.get()) { "Client closed" }
        return HttpRequest.newBuilder(endpoint.route(path)).timeout(Duration.ofSeconds(120))
            .header("Accept", "application/json").apply { if (token.isNotBlank()) header("Authorization", "Bearer $token") }
    }
    private fun <T> response(request: HttpRequest, consume: (InputStream, String) -> T): T {
        val responseBody = java.util.concurrent.atomic.AtomicReference<InputStream?>()
        val timedOut = AtomicBoolean(false)
        val (pending, timeout) = synchronized(lifecycle) {
            check(!closed.get()) { "Client closed" }
            check(active == null) { "One request per client at a time." }
            val future = http.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
            active = future
            val timer = deadline.schedule({ timedOut.set(true); future.cancel(true); runCatching { responseBody.get()?.close() } }, 120, TimeUnit.SECONDS)
            future to timer
        }
        try {
            val result = pending.get(120, TimeUnit.SECONDS)
            body = result.body(); responseBody.set(body)
            check(!closed.get() && !timedOut.get()) { "Request cancelled or timed out" }
            require(result.statusCode() == 200) { "Host returned HTTP ${result.statusCode()}; response body withheld." }
            return body!!.use { consume(it, result.headers().firstValue("Content-Type").orElse("")) }
        } finally {
            runCatching { responseBody.getAndSet(null)?.close() }
            synchronized(lifecycle) { body = null; active = null; timeout.cancel(false) }
        }
    }
    fun models(): List<String> = response(request("models").GET().build()) { stream, _ ->
        val bytes = stream.readNBytes(262145)
        require(bytes.size <= 262144) { "Model list exceeds 256 KiB." }
        val root = boundedJson(bytes.decodeToString(throwOnInvalidSequence = true))
        val list = root["data"]?.jsonArray ?: error("Missing model list")
        require(list.size <= 512)
        list.map { it.jsonObject["id"]?.jsonPrimitive?.content ?: error("Missing model id") }
            .onEach { require(it.length in 1..512 && it.none(Char::isISOControl)) }.distinct()
    }
    fun generate(model: String, turns: List<ChatTurn>, budget: GenerationBudget, onText: (String) -> Unit): GenerationUsage {
        require(model.length in 1..512 && model.none(Char::isISOControl))
        require(turns.size in 1..64 && turns.last().role == "user" && turns.sumOf { it.content.length.toLong() } <= 131072)
        val payload = buildJsonObject {
            put("model", model); put("stream", true); put("max_tokens", budget.maxOutputTokens)
            putJsonObject("stream_options") { put("include_usage", true) }
            putJsonArray("messages") { turns.forEach { t -> add(buildJsonObject { put("role", t.role); put("content", t.content) }) } }
        }.toString()
        val start = System.nanoTime()
        return response(request("chat/completions").header("Content-Type", "application/json")
            .header("Accept", "text/event-stream").POST(HttpRequest.BodyPublishers.ofString(payload)).build()) { stream, type ->
            require(type.substringBefore(';').trim().lowercase() == "text/event-stream") { "Host did not return a token stream." }
            var total = 0; var outputSize = 0; var tokens: Long? = null; var finished = false
            val event = StringBuilder()
            fun dispatch(): Boolean {
                if (event.isEmpty()) return false
                val data = event.toString().trimEnd('\n'); event.setLength(0)
                if (data == "[DONE]") return true
                val json = boundedJson(data)
                if (json["error"] != null) error("Host reported a generation error; details withheld.")
                json["usage"]?.takeIf { it is JsonObject }?.jsonObject?.get("completion_tokens")?.jsonPrimitive?.longOrNull?.let {
                    require(it in 0..10_000_000); tokens = it
                }
                json["choices"]?.jsonArray?.forEach { choice ->
                    val delta = choice.jsonObject["delta"]?.jsonObject
                    val content = delta?.get("content")?.takeIf { it is JsonPrimitive && it != JsonNull }?.jsonPrimitive?.content
                    if (content != null) { outputSize += content.length; require(outputSize <= 131072); onText(content) }
                }
                return false
            }
            val line = StringBuilder()
            while (!finished) {
                if (closed.get() || Thread.currentThread().isInterrupted) throw CancellationException("Request cancelled")
                val c = stream.read()
                if (c == -1) { if (line.isNotEmpty() || event.isNotEmpty()) error("Incomplete token stream"); break }
                require(++total <= 2_097_152) { "Token stream exceeds 2 MiB." }
                // Preserve UTF-8 bytes until the complete line; no per-byte Unicode decoding.
                if (c == 10) {
                    val value = line.toString().map { it.code.toByte() }.toByteArray().decodeToString(throwOnInvalidSequence = true).trimEnd('\r'); line.setLength(0)
                    if (value.isEmpty()) finished = dispatch()
                    else if (value.startsWith("data:")) { event.append(value.removePrefix("data:").removePrefix(" ")).append('\n'); require(event.length <= 65536) }
                } else { line.append(c.toChar()); require(line.length <= 65536) { "SSE line exceeds 64 KiB." } }
            }
            require(finished) { "Host closed the stream before completion." }
            require(outputSize > 0) { "Host returned no text." }
            GenerationUsage(tokens, System.nanoTime() - start)
        }
    }
    override fun close() {
        synchronized(lifecycle) { closed.set(true); active?.cancel(true); deadline.shutdownNow() }
        runCatching { body?.close() }; http.shutdownNow()
    }
}

/** Bound nesting before the provider-controlled JSON reaches the recursive parser. */
internal fun boundedJson(text: String): JsonObject {
    require(text.length <= 262144)
    var depth = 0; var quoted = false; var escaped = false
    text.forEach { character ->
        if (quoted) {
            if (escaped) escaped = false
            else if (character == '\\') escaped = true
            else if (character == '"') quoted = false
        } else when (character) {
            '"' -> quoted = true
            '{', '[' -> { depth++; require(depth <= 32) { "Host JSON nesting exceeds 32 levels." } }
            '}', ']' -> { depth--; require(depth >= 0) }
        }
    }
    require(depth == 0 && !quoted)
    return Json.parseToJsonElement(text).jsonObject
}
