package com.localagent.app.ui

import com.localagent.app.LocalAgentApplication
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = LocalAgentApplication()
        app.initializeCoreDomain()
    }

    @Test
    fun testCoreDomainInitialization() {
        assertNotNull(app.capabilityRegistry)
        assertNotNull(app.policyEngine)
        assertNotNull(app.goalDispatcher)

        assertTrue(app.capabilityRegistry.isCapabilitySupported("UI_CLICK", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("GLOBAL_BACK", deviceApi = 27))
    }

    @Test
    fun testGoalDispatcherIntegration() {
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }
}
