package com.localagent.core.capability

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CapabilityRegistryTest {

    private lateinit var registry: CapabilityRegistry

    @Before
    fun setUp() {
        registry = CapabilityRegistry()
    }

    @Test
    fun testRegisterAndGetCapability() {
        val rule = CapabilityRule(
            capabilityId = "UI_CLICK",
            name = "UI Click",
            category = CapabilityCategory.UI_CONTROL,
            minApi = 27,
            verificationStrategy = "TargetClickStrategy"
        )

        registry.registerCapability(rule)

        val retrieved = registry.getCapability("UI_CLICK")
        assertNotNull(retrieved)
        assertEquals("UI_CLICK", retrieved.capabilityId)
        assertEquals(CapabilityCategory.UI_CONTROL, retrieved.category)
    }

    @Test
    fun testIsCapabilitySupportedApiCheck() {
        val rule = CapabilityRule(
            capabilityId = "GLOBAL_LOCK_SCREEN",
            name = "Global Lock Screen",
            category = CapabilityCategory.NAVIGATION,
            minApi = 28,
            verificationStrategy = "LockScreenStrategy"
        )

        registry.registerCapability(rule)

        assertFalse(registry.isCapabilitySupported("GLOBAL_LOCK_SCREEN", deviceApi = 27))
        assertTrue(registry.isCapabilitySupported("GLOBAL_LOCK_SCREEN", deviceApi = 28))
        assertTrue(registry.isCapabilitySupported("GLOBAL_LOCK_SCREEN", deviceApi = 34))
    }
}
