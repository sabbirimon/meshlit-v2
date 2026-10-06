package com.meshlit.core.inference.models
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Offline protocol/response failure checks; not paid API or model-quality proof. */
class OnlineMediaClientTest {
    private val p=OnlineProfile("media","media","https://example.invalid/v1","vision-model",enabled=true)
    private fun client(body:String,onRequest:(Request)->Unit={})=OnlineMediaClient(OkHttpClient.Builder().addInterceptor{chain->
        onRequest(chain.request());Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }.build())
    @Test fun disabledProfilesFailBeforeAnyRequest()=runBlocking {
        var calls=0
        try{client("{}"){calls++}.startVideo(p.copy(enabled=false),"secret","video-model","task");fail("disabled")}
        catch(_:IllegalArgumentException){assertEquals(0,calls)}
    }
    @Test fun videoJobUsesMultipartAndActualReturnedState()=runBlocking {
        val r=client("""{"id":"video_123","status":"queued","progress":7}"""){request->
            assertEquals("/v1/videos",request.url.encodedPath);assertEquals("Bearer secret",request.header("Authorization"))
            assertTrue(request.body is MultipartBody)
        }.startVideo(p,"secret","video-model","task")
        assertEquals("video_123",r.id);assertEquals("queued",r.status);assertEquals(7,r.progress)
    }
    @Test fun invalidRemoteIdCannotBecomeARequestPath()=runBlocking {
        var calls=0
        try{client("{}"){calls++}.videoStatus(p,"secret","../other");fail("bad ID")}
        catch(_:IllegalArgumentException){assertEquals(0,calls)}
    }
    @Test fun imageUrlIsNotAutomaticallyFetchedAndNoFileIsInstalled()=runBlocking {
        val f=File.createTempFile("media-contract-",".png").apply{delete()}
        try{client("""{"data":[{"url":"https://other.invalid/image"}]}""").image(p,"secret","image-model","task",f);fail("URL-only response")}
        catch(_:IllegalStateException){assertFalse(f.exists())}finally{f.delete()}
    }
    @Test fun nonPngPayloadIsNotAnInstalledImage()=runBlocking {
        val f=File.createTempFile("media-contract-",".png").apply{delete()}
        try{client("""{"data":[{"b64_json":"bm90LWltYWdlLWJ5dGVz"}]}""").image(p,"secret","image-model","task",f);fail("bad container")}
        catch(_:IllegalArgumentException){assertFalse(f.exists())}finally{f.delete()}
    }
}
