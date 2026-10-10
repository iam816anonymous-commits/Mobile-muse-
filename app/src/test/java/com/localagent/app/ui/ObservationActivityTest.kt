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
class ObservationActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testObservationActivityLaunchAndViews() {
        val controller = Robolectric.buildActivity(ObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val statusText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvAccessibilityStatus).text.toString()
        assertTrue(statusText.contains("Status:"))
    }

    @Test
    fun testNodeTreeRenderingFormatting() {
        val childNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.Button",
            text = "Search",
            resourceId = "com.chrome:id/btn_search",
            clickable = true,
            parentInstanceId = "node_0"
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

        val treeStr = NodeTreeRenderer.renderSnapshotTree(snapshot)

        assertTrue(treeStr.contains("ROOT [com.android.chrome]"))
        assertTrue(treeStr.contains("FrameLayout"))
        assertTrue(treeStr.contains("Button"))
        assertTrue(treeStr.contains("Search"))
        assertTrue(treeStr.contains("com.chrome:id/btn_search"))
    }
}
