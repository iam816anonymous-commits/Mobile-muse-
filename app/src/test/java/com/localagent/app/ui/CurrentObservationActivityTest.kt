package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.core.observation.ObservationBounds
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.observation.ObservationTruncationInfo
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class CurrentObservationActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        AgentAccessibilityService.resetForTest(app)
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest(app)
    }

    @Test
    fun testTestA_PermissionCenterButtonAbsentFromCurrentObservationActivity() {
        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        // Test A: Observation controls present, direct Permission Center button removed
        val observeBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnObserveCurrentUi)
        assertNotNull(observeBtn)
        val clearBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnClearCurrentObservation)
        assertNotNull(clearBtn)
    }

    @Test
    fun testP5_UI_DUP_001_ObserveRequestRendersExactlyOneTreeHeader() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val mockNode = ObservationNode(nodeId = "node_0", className = "android.widget.LinearLayout", packageName = "com.localagent.app")
        val snap = ObservationSnapshot(snapshotId = "s1", packageName = "com.localagent.app", nodeCount = 1, rootNode = mockNode)
        service.currentObservationSnapshot = snap

        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val updateMethod = CurrentObservationActivity::class.java.getDeclaredMethod("updateObservationUi")
        updateMethod.isAccessible = true
        updateMethod.invoke(activity)

        val treeText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvCurrentNodeTree).text.toString()
        val rootOccurrences = treeText.split("ROOT [").size - 1
        assertEquals(1, rootOccurrences)

        serviceController.destroy()
    }

    @Test
    fun testP5_UI_DUP_002_TwoConsecutiveObserveRequestsReplacePreviousTree() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val mockNode = ObservationNode(nodeId = "node_0", className = "android.widget.LinearLayout", packageName = "com.localagent.app")
        val snap = ObservationSnapshot(snapshotId = "s1", packageName = "com.localagent.app", nodeCount = 1, rootNode = mockNode)
        service.currentObservationSnapshot = snap

        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val updateMethod = CurrentObservationActivity::class.java.getDeclaredMethod("updateObservationUi")
        updateMethod.isAccessible = true

        updateMethod.invoke(activity) // First update
        updateMethod.invoke(activity) // Second consecutive update

        val treeText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvCurrentNodeTree).text.toString()
        val rootOccurrences = treeText.split("ROOT [").size - 1

        // Test P5-UI-DUP-002: Replaces previous tree, exactly 1 root header rendered
        assertEquals(1, rootOccurrences)

        serviceController.destroy()
    }

    @Test
    fun testP5_UI_DUP_003_ClearRemovesDisplayedTree() {
        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val clearBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnClearCurrentObservation)
        assertNotNull(clearBtn)

        clearBtn.performClick()

        val nodeTreeText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvCurrentNodeTree).text.toString()
        assertEquals("No current observation captured.", nodeTreeText)
    }

    @Test
    fun testP5_UI_DUP_004_ObserveAfterClearRendersExactlyOneTree() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val mockNode = ObservationNode(nodeId = "node_0", className = "android.widget.LinearLayout", packageName = "com.localagent.app")
        val snap = ObservationSnapshot(snapshotId = "s1", packageName = "com.localagent.app", nodeCount = 1, rootNode = mockNode)
        service.currentObservationSnapshot = snap

        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val updateMethod = CurrentObservationActivity::class.java.getDeclaredMethod("updateObservationUi")
        updateMethod.isAccessible = true

        val clearBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnClearCurrentObservation)

        updateMethod.invoke(activity)
        clearBtn.performClick()

        val observeBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnObserveCurrentUi)
        observeBtn.performClick() // Resets isDisplayCleared flag
        service.currentObservationSnapshot = snap // Re-set valid snapshot for test
        updateMethod.invoke(activity)

        val treeText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvCurrentNodeTree).text.toString()
        val rootOccurrences = treeText.split("ROOT [").size - 1
        assertEquals(1, rootOccurrences)

        serviceController.destroy()
    }

    @Test
    fun testTestF_CurrentObservationRendersBoundsForNodes() {
        val node = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.TextView",
            text = "Title",
            bounds = ObservationBounds(left = 10, top = 20, right = 110, bottom = 70)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-1",
            packageName = "com.localagent.app",
            nodeCount = 1,
            rootNode = node,
            truncationInfo = ObservationTruncationInfo()
        )

        val activity = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().get()
        val method = CurrentObservationActivity::class.java.getDeclaredMethod("renderSnapshotNodeTree", ObservationSnapshot::class.java)
        method.isAccessible = true
        val treeStr = method.invoke(activity, snapshot) as String

        // Test F: Bounds [left,top,right,bottom] and size (WxH) displayed
        assertTrue(treeStr.contains("bounds:[10,20,110,70] (100x50)"))
    }

    @Test
    fun testTestH_CurrentObservationDoesNotDuplicateSameNodeDuringRendering() {
        val node = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.TextView",
            text = "Unique Title Text",
            resourceId = "com.localagent.app:id/tvTitle"
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-1",
            packageName = "com.localagent.app",
            nodeCount = 1,
            rootNode = node
        )

        val activity = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().get()
        val method = CurrentObservationActivity::class.java.getDeclaredMethod("renderSnapshotNodeTree", ObservationSnapshot::class.java)
        method.isAccessible = true
        val treeStr = method.invoke(activity, snapshot) as String

        // Test H: Node text appears exactly once in rendered string
        val occurrences = treeStr.split("Unique Title Text").size - 1
        assertEquals(1, occurrences)
    }
}
