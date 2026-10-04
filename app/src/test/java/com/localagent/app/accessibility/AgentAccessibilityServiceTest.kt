package com.localagent.app.accessibility

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class AgentAccessibilityServiceTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testUnboundServiceStatus() {
        assertFalse(AgentAccessibilityService.isBound)
        assertNull(AgentAccessibilityService.INSTANCE)
    }

    @Test
    fun testObservationIsReadOnlyWithNoActionExecution() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        assertFalse(AgentAccessibilityService.isBound)

        val snapshot = service.captureLiveSnapshot()
        assertNotNull(snapshot)
        assertEquals(0, snapshot.nodeCount)
        assertNull(snapshot.rootNode)

        // Verify zero action policy or execution dispatched during observation
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }

    @Test
    fun testPreservationOfLastExternalObservationSnapshot() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        // Initially no snapshots
        assertNull(service.currentObservationSnapshot)
        assertNull(service.lastExternalObservationSnapshot)

        // Simulate observing current UI (LocalAgent app itself)
        val localSnap = service.captureLiveSnapshot()
        assertNotNull(service.currentObservationSnapshot)
        // Since package was blank or com.localagent.app, external snapshot remains null
        assertNull(service.lastExternalObservationSnapshot)
    }
}
