package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityWindowInfo
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
        service.captureLiveSnapshot()
        assertNotNull(service.currentObservationSnapshot)
        assertNull(service.lastExternalObservationSnapshot)
    }

    @Test
    fun testValidExternalApplicationPackageClassification() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        assertTrue(service.isValidExternalApplicationPackage("com.android.chrome"))
        assertTrue(service.isValidExternalApplicationPackage("com.google.android.apps.chrome"))
        assertTrue(service.isValidExternalApplicationPackage("com.android.calculator2"))
        assertTrue(service.isValidExternalApplicationPackage("com.android.settings"))

        assertFalse(service.isValidExternalApplicationPackage("com.localagent.app"))
        assertFalse(service.isValidExternalApplicationPackage("com.android.systemui"))
        assertFalse(service.isValidExternalApplicationPackage("com.google.android.apps.nexuslauncher"))
        assertFalse(service.isValidExternalApplicationPackage("com.android.launcher3"))
        assertFalse(service.isValidExternalApplicationPackage(""))
    }

    @Test
    fun testWindowCandidateScoreRanking() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        val method = AgentAccessibilityService::class.java.getDeclaredMethod(
            "calculateWindowScore",
            Int::class.java,
            String::class.java,
            Boolean::class.java,
            Boolean::class.java
        )
        method.isAccessible = true

        val chromeScore = method.invoke(service, AccessibilityWindowInfo.TYPE_APPLICATION, "com.android.chrome", true, true) as Int
        val systemUiScore = method.invoke(service, AccessibilityWindowInfo.TYPE_SYSTEM, "com.android.systemui", true, true) as Int

        assertTrue(chromeScore > systemUiScore)
        assertEquals(100, chromeScore)
        assertEquals(50, systemUiScore)
    }
}
