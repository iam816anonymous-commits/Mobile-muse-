package com.localagent.app.accessibility

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.observation.NodeIdentityConfidence
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class ObservationSnapshotExtractorPhase5Test {

    private lateinit var app: LocalAgentApplication
    private lateinit var extractor: ObservationSnapshotExtractor

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        extractor = ObservationSnapshotExtractor(maxNodes = 500, maxDepth = 30)
    }

    @Test
    fun testNullRootExtraction() {
        val snapshot = extractor.extractSnapshot(
            rootNodeInfo = null,
            packageName = "com.example.nullpkg",
            windowId = 123
        )

        assertNotNull(snapshot)
        assertEquals("com.example.nullpkg", snapshot.packageName)
        assertEquals(123, snapshot.windowId)
        assertEquals(0, snapshot.nodeCount)
        assertNull(snapshot.rootNode)
        assertFalse(snapshot.truncationInfo.isTruncated)
    }

    @Test
    fun testIdentityAndConfidenceAssignment() {
        val (exactId, exactConf) = com.localagent.core.observation.ObservationNode.computeIdentity(
            packageName = "com.example",
            className = "android.widget.TextView",
            resourceId = "com.example:id/tvTitle",
            text = "Welcome",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )
        assertEquals(NodeIdentityConfidence.EXACT, exactConf)
        assertTrue(exactId.contains("tvTitle"))

        val (highId, highConf) = com.localagent.core.observation.ObservationNode.computeIdentity(
            packageName = "com.example",
            className = "android.widget.ImageView",
            resourceId = "com.example:id/imgLogo",
            text = null,
            contentDescription = null,
            childIndex = 1,
            parentIdentity = null
        )
        assertEquals(NodeIdentityConfidence.HIGH, highConf)
        assertTrue(highId.contains("imgLogo"))

        val (medId, medConf) = com.localagent.core.observation.ObservationNode.computeIdentity(
            packageName = "com.example",
            className = "android.widget.Button",
            resourceId = null,
            text = "Click Me",
            contentDescription = null,
            childIndex = 2,
            parentIdentity = "parent_0"
        )
        assertEquals(NodeIdentityConfidence.MEDIUM, medConf)
        assertTrue(medId.contains("Click Me"))
    }

    @Test
    fun testObservationStateAndStopControls() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        assertFalse(AgentAccessibilityService.isBound)

        val snap = service.captureLiveSnapshot()
        assertNotNull(snap)

        // Verify zero action execution during snapshot capture
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }
}
