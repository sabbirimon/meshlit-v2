package com.meshlit.ui.modern
import org.junit.Test
import org.junit.Assert.*
class SettingsDestinationsTest {
    @Test fun searchHonorsAllWordsAndAdvancedFilter(){
        assertEquals(listOf("logs"),SettingsDestinations.search("log export",false).map{it.id})
        assertTrue(SettingsDestinations.search("rootless",false).isEmpty())
        assertEquals(listOf("runtime"),SettingsDestinations.search("rootless",true).map{it.id})
        assertEquals(listOf("appearance"),SettingsDestinations.search("wallpaper colors",false).map{it.id})
    }
}
