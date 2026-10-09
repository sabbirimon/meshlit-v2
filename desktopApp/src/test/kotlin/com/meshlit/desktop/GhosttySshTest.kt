package com.meshlit.desktop
import java.nio.ByteBuffer
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64
import kotlin.test.*
import org.junit.Test
class GhosttySshTest {
    @Test fun pinAndLiteralArguments() {
        val type="ssh-ed25519".toByteArray(); val wire=ByteBuffer.allocate(4+type.size+4+32).putInt(type.size).put(type).putInt(32).put(ByteArray(32){it.toByte()}).array()
        val key="ssh-ed25519 "+Base64.getEncoder().encodeToString(wire)
        val pin="SHA256:"+Base64.getEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(wire))
        assertTrue(GhosttySsh.knownHost("node.example",2222,key,pin).startsWith("[node.example]:2222 ssh-ed25519 "))
        assertFailsWith<IllegalArgumentException>{GhosttySsh.knownHost("node.example",22,key,"SHA256:"+"A".repeat(43))}
        val args=GhosttySsh.arguments(Path.of("/Applications/Ghostty.app/Contents/MacOS/ghostty"),Path.of("/usr/bin/ssh"),"node.example",2222,"owner",Path.of("/tmp/key with spaces"),Path.of("/tmp/pins"))
        assertTrue("StrictHostKeyChecking=yes" in args); assertTrue("ClearAllForwardings=yes" in args); assertTrue("/tmp/key with spaces" in args)
        assertFalse(args.any { it == "sh" || it == "-c" }); assertTrue("--config-default-files=false" in args)
        assertFailsWith<IllegalArgumentException>{GhosttySsh.arguments(Path.of("/tmp/g"),Path.of("/usr/bin/ssh"),"-oProxyCommand=bad",22,"owner",Path.of("/tmp/key"),Path.of("/tmp/pins"))}
    }
}
