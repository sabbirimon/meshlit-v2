package com.meshlit.chat

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ChatOptionsTest {
    @Test fun oldChatsDoNotSilentlyGainWebOrPhoneTools() {
        val old=Json.decodeFromString<ChatOptions>("{\"maxTokens\":16}")
        assertFalse(old.webTools);assertFalse(old.phoneTools);old.validate()
    }
    @Test fun savedExplicitGrantsRoundTripAndCannotSelectRemoteOrRoutedModels() {
        val options=ChatOptions(webTools=true,phoneTools=true)
        options.validate()
        assertEquals(options,Json.decodeFromString<ChatOptions>(Json.encodeToString(ChatOptions.serializer(),options)))
        assertThrows(IllegalArgumentException::class.java){options.copy(onlineProfileId="provider").validate()}
        assertThrows(IllegalArgumentException::class.java){options.copy(routeId="route").validate()}
    }
}
