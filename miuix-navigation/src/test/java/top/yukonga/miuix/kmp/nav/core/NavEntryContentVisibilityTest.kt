package top.yukonga.miuix.kmp.nav.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavEntryContentVisibilityTest {
    @Test
    fun coveredSourceResumesOnBackPreviewAndPausesAgainOnCancel() {
        assertTrue(navEntryContentVisible(0f, 1f, false))
        assertFalse(navEntryContentVisible(1f, 1f, false))
        assertTrue(navEntryContentVisible(0.99f, 1f, false))
        assertFalse(navEntryContentVisible(1f, 1f, false))
        assertTrue(navEntryContentVisible(0f, 1f, false))
    }

    @Test
    fun incomingPreparationCanLoadButRemovedEndpointCannot() {
        assertTrue(navEntryContentVisible(-1f, 1f, false))
        assertTrue(navEntryContentVisible(-0.5f, 1f, true))
        assertFalse(navEntryContentVisible(-1f, 1f, true))
    }

    @Test
    fun modalKeepsItsExposedBackgroundAvailable() {
        assertTrue(navEntryContentVisible(1f, 2f, false))
        assertFalse(navEntryContentVisible(2f, 2f, false))
    }
}
