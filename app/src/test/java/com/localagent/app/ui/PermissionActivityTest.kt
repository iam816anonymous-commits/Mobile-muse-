package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
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
}
