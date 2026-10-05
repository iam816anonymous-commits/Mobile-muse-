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
class CurrentObservationActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testCurrentObservationActivityLaunchAndViews() {
        val controller = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val activity = controller.get()

        val statusText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvAccessibilityStatus).text.toString()
        assertTrue(statusText.contains("Engine State:"))

        val observeBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnObserveCurrentUi)
        assertNotNull(observeBtn)

        val stopBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnStopCurrentObservation)
        assertNotNull(stopBtn)
    }

    @Test
    fun testCurrentNodeTreeRendering() {
        val childNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.TextView",
            text = "Home Status",
            resourceId = "com.localagent.app:id/tvTitle"
        )

        val rootNode = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.LinearLayout",
            packageName = "com.localagent.app",
            children = listOf(childNode)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-local-1",
            packageName = "com.localagent.app",
            nodeCount = 2,
            rootNode = rootNode,
            truncationInfo = ObservationTruncationInfo()
        )

        val activity = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().get()
        val method = CurrentObservationActivity::class.java.getDeclaredMethod("renderSnapshotNodeTree", ObservationSnapshot::class.java)
        method.isAccessible = true
        val treeStr = method.invoke(activity, snapshot) as String

        assertTrue(treeStr.contains("ROOT [com.localagent.app]"))
        assertTrue(treeStr.contains("LinearLayout"))
        assertTrue(treeStr.contains("TextView id:tvTitle text:\"Home Status\""))
    }
}
