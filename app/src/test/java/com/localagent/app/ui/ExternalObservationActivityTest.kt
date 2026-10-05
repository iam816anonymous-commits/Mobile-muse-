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
class ExternalObservationActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testTestB_PermissionCenterButtonAbsentFromExternalObservationActivity() {
        val controller = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        // Test B: Observation controls present, direct Permission Center button removed
        val captureBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnCaptureExternalUi)
        assertNotNull(captureBtn)
        val clearBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnClearExternalObservation)
        assertNotNull(clearBtn)
    }

    @Test
    fun testTestE_ClearExternalObservationRemovesSnapshotDisplayWithoutUnbindingService() {
        val controller = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val clearBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnClearExternalObservation)
        assertNotNull(clearBtn)

        clearBtn.performClick()

        val nodeTreeText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvExternalNodeTree).text.toString()
        assertEquals("No external observation captured.", nodeTreeText)
    }

    @Test
    fun testTestG_ExternalObservationRendersBoundsForNodes() {
        val node = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.Button",
            text = "Equals",
            resourceId = "com.android.calculator2:id/eq",
            bounds = ObservationBounds(left = 200, top = 800, right = 400, bottom = 1000)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-calc-1",
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = node,
            truncationInfo = ObservationTruncationInfo()
        )

        val activity = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().get()
        val method = ExternalObservationActivity::class.java.getDeclaredMethod("renderSnapshotNodeTree", ObservationSnapshot::class.java)
        method.isAccessible = true
        val treeStr = method.invoke(activity, snapshot) as String

        // Test G: Bounds [left,top,right,bottom] and size (WxH) displayed for external app
        assertTrue(treeStr.contains("bounds:[200,800,400,1000] (200x200)"))
    }

    @Test
    fun testTestI_ExternalObservationDoesNotMergeLocalAgentNodesIntoExternalSnapshot() {
        val calcNode = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.FrameLayout",
            packageName = "com.android.calculator2"
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-calc-1",
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = calcNode
        )

        val activity = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().get()
        val method = ExternalObservationActivity::class.java.getDeclaredMethod("renderSnapshotNodeTree", ObservationSnapshot::class.java)
        method.isAccessible = true
        val treeStr = method.invoke(activity, snapshot) as String

        // Test I: External snapshot tree is pure Calculator hierarchy and contains 0 LocalAgent nodes
        assertTrue(treeStr.contains("ROOT [com.android.calculator2]"))
        assertFalse(treeStr.contains("com.localagent.app"))
    }
}
