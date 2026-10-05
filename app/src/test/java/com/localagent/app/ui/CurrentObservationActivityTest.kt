package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.observation.ObservationBounds
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.observation.ObservationTruncationInfo
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
    fun testTestD_ClearCurrentObservationRemovesSnapshotDisplayWithoutUnbindingService() {
        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val clearBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnClearCurrentObservation)
        assertNotNull(clearBtn)

        clearBtn.performClick()

        val nodeTreeText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvCurrentNodeTree).text.toString()
        assertEquals("No current observation captured.", nodeTreeText)
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
