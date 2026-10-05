package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
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
    fun testExternalObservationActivityLaunchAndViews() {
        val controller = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val statusText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvAccessibilityStatus).text.toString()
        assertTrue(statusText.contains("Engine State:"))

        val captureBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnCaptureExternalUi)
        assertNotNull(captureBtn)

        val stopBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnStopExternalObservation)
        assertNotNull(stopBtn)
    }

    @Test
    fun testExternalNodeTreeRendering() {
        val childNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.Button",
            text = "Search",
            resourceId = "com.android.chrome:id/search_btn",
            clickable = true
        )

        val rootNode = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.FrameLayout",
            packageName = "com.android.chrome",
            children = listOf(childNode)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-chrome-1",
            packageName = "com.android.chrome",
            nodeCount = 2,
            rootNode = rootNode,
            truncationInfo = ObservationTruncationInfo()
        )

        val activity = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().get()
        val method = ExternalObservationActivity::class.java.getDeclaredMethod("renderSnapshotNodeTree", ObservationSnapshot::class.java)
        method.isAccessible = true
        val treeStr = method.invoke(activity, snapshot) as String

        assertTrue(treeStr.contains("ROOT [com.android.chrome]"))
        assertTrue(treeStr.contains("FrameLayout"))
        assertTrue(treeStr.contains("Button id:search_btn text:\"Search\""))
        assertTrue(treeStr.contains("clickable"))
    }
}
