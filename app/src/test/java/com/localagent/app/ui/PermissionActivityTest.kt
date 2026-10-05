package com.localagent.app.ui

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class PermissionActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testPermissionActivityLaunchAndSummary() {
        val controller = Robolectric.buildActivity(PermissionActivity::class.java).create().start().resume()
        val activity = controller.get()

        val summaryText = activity.findViewById<android.widget.TextView>(R.id.tvPermissionSummary).text.toString()
        assertTrue(summaryText.contains("REQUIRED NOW:"))
        assertTrue(summaryText.contains("OPTIONAL NOW:"))
        assertTrue(summaryText.contains("FUTURE PHASE:"))

        val a11yBtn = activity.findViewById<android.widget.Button>(R.id.btnOpenAccessibilitySettings)
        assertNotNull(a11yBtn)
    }

    @Test
    fun testMainActivityNavigationToPermissionActivity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val mainActivity = controller.get()

        val btnPermissionCenter = mainActivity.findViewById<android.widget.Button>(R.id.btnOpenPermissionCenterScreen)
        assertNotNull(btnPermissionCenter)

        btnPermissionCenter.performClick()

        val expectedIntent = Intent(mainActivity, PermissionActivity::class.java)
        val nextStartedActivity = shadowOf(mainActivity).nextStartedActivity
        assertNotNull(nextStartedActivity)
        assertEquals(expectedIntent.component, nextStartedActivity.component)
    }

    @Test
    fun testObservationScreensNavigateToPermissionCenter() {
        val currController = Robolectric.buildActivity(CurrentObservationActivity::class.java).create().start().resume()
        val currActivity = currController.get()
        val btnCurrPerm = currActivity.findViewById<android.widget.Button>(R.id.btnOpenPermissionCenter)
        assertNotNull(btnCurrPerm)
        btnCurrPerm.performClick()
        val nextFromCurr = shadowOf(currActivity).nextStartedActivity
        assertNotNull(nextFromCurr)
        assertEquals(PermissionActivity::class.java.name, nextFromCurr.component?.className)

        val extController = Robolectric.buildActivity(ExternalObservationActivity::class.java).create().start().resume()
        val extActivity = extController.get()
        val btnExtPerm = extActivity.findViewById<android.widget.Button>(R.id.btnOpenPermissionCenter)
        assertNotNull(btnExtPerm)
        btnExtPerm.performClick()
        val nextFromExt = shadowOf(extActivity).nextStartedActivity
        assertNotNull(nextFromExt)
        assertEquals(PermissionActivity::class.java.name, nextFromExt.component?.className)

        val evController = Robolectric.buildActivity(EvidenceActivity::class.java).create().start().resume()
        val evActivity = evController.get()
        val btnEvPerm = evActivity.findViewById<android.widget.Button>(R.id.btnOpenPermissionCenter)
        assertNotNull(btnEvPerm)
        btnEvPerm.performClick()
        val nextFromEv = shadowOf(evActivity).nextStartedActivity
        assertNotNull(nextFromEv)
        assertEquals(PermissionActivity::class.java.name, nextFromEv.component?.className)
    }

    @Test
    fun testPermissionActivityCategorizedViews() {
        val controller = Robolectric.buildActivity(PermissionActivity::class.java).create().start().resume()
        val activity = controller.get()

        val tvA11yDetails = activity.findViewById<android.widget.TextView>(R.id.tvAccessibilityDetails).text.toString()
        val tvStorageDetails = activity.findViewById<android.widget.TextView>(R.id.tvStorageDetails).text.toString()
        val tvOverlayDetails = activity.findViewById<android.widget.TextView>(R.id.tvOverlayDetails).text.toString()

        assertTrue(tvA11yDetails.contains("STATUS: ACTION REQUIRED — SERVICE_UNBOUND"))
        assertTrue(tvStorageDetails.contains("Target Phase: Phase 2 (Durable Storage)"))
        assertTrue(tvOverlayDetails.contains("STATUS: NOT CURRENTLY REQUIRED — INVENTORY ONLY"))
    }
}
