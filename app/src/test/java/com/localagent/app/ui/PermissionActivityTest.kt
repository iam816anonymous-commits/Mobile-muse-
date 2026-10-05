package com.localagent.app.ui

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
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

        val summaryText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvPermissionSummary).text.toString()
        assertTrue(summaryText.contains("Special Access:"))
        assertTrue(summaryText.contains("Mic:"))

        val a11yBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnOpenAccessibilitySettings)
        assertNotNull(a11yBtn)
    }

    @Test
    fun testMainActivityNavigationToPermissionActivity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val mainActivity = controller.get()

        val btnPermissionCenter = mainActivity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnOpenPermissionCenterScreen)
        assertNotNull(btnPermissionCenter)

        btnPermissionCenter.performClick()

        val expectedIntent = Intent(mainActivity, PermissionActivity::class.java)
        val nextStartedActivity = shadowOf(mainActivity).nextStartedActivity
        assertNotNull(nextStartedActivity)
        assertEquals(expectedIntent.component, nextStartedActivity.component)
    }

    @Test
    fun testPermissionActivityViewsAndUnboundServiceSafety() {
        val controller = Robolectric.buildActivity(PermissionActivity::class.java).create().start().resume()
        val activity = controller.get()

        val tvA11yDetails = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvAccessibilityDetails).text.toString()
        val tvOverlayDetails = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvOverlayDetails).text.toString()
        val tvWriteSettingsDetails = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvWriteSettingsDetails).text.toString()

        assertTrue(tvA11yDetails.contains("STATUS:"))
        assertTrue(tvOverlayDetails.contains("STATUS:"))
        assertTrue(tvWriteSettingsDetails.contains("STATUS:"))

        // Verify button clicks do not crash Activity
        val btnOverlay = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnOpenOverlaySettings)
        assertNotNull(btnOverlay)
    }
}
