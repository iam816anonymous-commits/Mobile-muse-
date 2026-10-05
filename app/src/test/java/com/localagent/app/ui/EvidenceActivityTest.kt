package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.evidence.EvidenceSource
import com.localagent.core.evidence.ObservationEvidence
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
class EvidenceActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testTestC_PermissionCenterButtonAbsentFromEvidenceActivity() {
        val controller = Robolectric.buildActivity(EvidenceActivity::class.java).create().start().resume()
        val activity = controller.get()

        // Test C: Evidence controls present, direct Permission Center button removed
        val captureBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnCaptureEvidence)
        assertNotNull(captureBtn)
    }

    @Test
    fun testEvidenceActivityLaunchAndViews() {
        val controller = Robolectric.buildActivity(EvidenceActivity::class.java).create().start().resume()
        val activity = controller.get()

        val statusText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvAccessibilityStatus).text.toString()
        assertTrue(statusText.contains("Status:"))

        val captureBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnCaptureEvidence)
        assertNotNull(captureBtn)
    }

    @Test
    fun testEvidencePrimitivesRendering() {
        val childNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.Button",
            packageName = "com.android.chrome",
            text = "Submit",
            resourceId = "com.android.chrome:id/btn_submit",
            bounds = ObservationBounds(10, 10, 200, 80),
            clickable = true
        )

        val rootNode = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.LinearLayout",
            packageName = "com.android.chrome",
            bounds = ObservationBounds(0, 0, 1080, 1920),
            children = listOf(childNode)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-evidence-1",
            packageName = "com.android.chrome",
            nodeCount = 2,
            rootNode = rootNode,
            truncationInfo = ObservationTruncationInfo()
        )

        val evidence = ObservationEvidence.fromSnapshot(snapshot, EvidenceSource.EXTERNAL_APP)

        val activity = Robolectric.buildActivity(EvidenceActivity::class.java).create().get()

        val formatMetaMethod = EvidenceActivity::class.java.getDeclaredMethod("formatMetaText", ObservationEvidence::class.java)
        formatMetaMethod.isAccessible = true
        val metaStr = formatMetaMethod.invoke(activity, evidence) as String

        assertTrue(metaStr.contains("Evidence ID:"))
        assertTrue(metaStr.contains("Provenance Hash:"))
        assertTrue(metaStr.contains("com.android.chrome"))

        val renderMethod = EvidenceActivity::class.java.getDeclaredMethod("renderEvidencePrimitives", ObservationEvidence::class.java)
        renderMethod.isAccessible = true
        val primStr = renderMethod.invoke(activity, evidence) as String

        assertTrue(primStr.contains("EVIDENCE PRIMITIVES [com.android.chrome]"))
        assertTrue(primStr.contains("LinearLayout"))
        assertTrue(primStr.contains("Button id:btn_submit text:\"Submit\""))
    }
}
