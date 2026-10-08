package com.meshlit
import com.meshlit.chat.ChatOptions
import com.meshlit.core.common.control.*
import com.meshlit.ui.modern.WorkspaceDestinations
import com.meshlit.ui.modern.SettingsDestinations
import org.junit.Assert.*
import org.junit.Test
class BuildProfileTest {
    @Test fun channelRoutesAndDirectOperationsAgree() {
        val gate=OperationGate(allowedFeatures=BuildProfile.features)
        for(destination in WorkspaceDestinations.all) assertTrue(BuildProfile.routeAllowed(destination.id))
        for(destination in SettingsDestinations.all) assertTrue(BuildProfile.routeAllowed(destination.id))
        for(shortcut in WorkspaceDestinations.shortcuts) assertTrue(WorkspaceDestinations.all.any { it.id==shortcut })
        BuildProfile.requireChat(ChatOptions())
        if(BuildConfig.CORE_CANDIDATE) {
            assertFalse(BuildProfile.routeAllowed("ssh"));assertFalse(BuildProfile.routeAllowed("runtime"))
            assertThrows(IllegalStateException::class.java) { gate.requireAllowed(ManagedFeature.VM) }
            assertThrows(IllegalStateException::class.java) { gate.setFeature(ManagedFeature.HYPERL,true) }
            assertThrows(IllegalStateException::class.java) { BuildProfile.requireChat(ChatOptions(onlineProfileId="remote")) }
            assertThrows(IllegalStateException::class.java) { BuildProfile.requireChat(ChatOptions(phoneTools=true)) }
        } else {
            assertTrue(BuildProfile.routeAllowed("ssh"));gate.requireAllowed(ManagedFeature.VM)
        }
    }
}
