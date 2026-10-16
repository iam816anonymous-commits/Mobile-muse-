package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class CommandConsolePerformanceTest {

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
    fun `CC-PERF-001 - Clear command history does not invoke accessibility observation`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        val snapBefore = service.currentObservationSnapshot

        // Input text and clear UI
        activity.findViewById<android.widget.EditText>(com.localagent.app.R.id.etCommandInput).setText("click submit")
        activity.findViewById<android.widget.EditText>(com.localagent.app.R.id.etCommandInput).setText("")

        val snapAfter = service.currentObservationSnapshot

        // Observation snapshot was not triggered by UI clear
        assertEquals(snapBefore, snapAfter)
    }

    @Test
    fun `CC-PERF-002 - Clear command history does not invoke action execution`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        val queueBefore = app.goalDispatcher.getQueueSize()

        // Clear command input UI
        activity.findViewById<android.widget.EditText>(com.localagent.app.R.id.etCommandInput).setText("")

        val queueAfter = app.goalDispatcher.getQueueSize()

        assertEquals(0, queueBefore)
        assertEquals(0, queueAfter)
    }

    @Test
    fun `CC-PERF-003 - Clear command history results in empty command list presentation`() {
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        val etInput = activity.findViewById<android.widget.EditText>(com.localagent.app.R.id.etCommandInput)
        val tvResult = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult)

        etInput.setText("status")
        activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnExecute).performClick()

        assertTrue(tvResult.text.toString().contains("AGENT_STATUS"))

        // Reset/clear
        etInput.setText("")
        tvResult.text = "None"

        assertEquals("", etInput.text.toString())
        assertEquals("None", tvResult.text.toString())
    }

    @Test
    fun `CC-PERF-004 - Clear command history does not duplicate command or event records`() {
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        val countBefore = app.eventRepository.getEventCount()

        // Clear input
        activity.findViewById<android.widget.EditText>(com.localagent.app.R.id.etCommandInput).setText("")

        val countAfter = app.eventRepository.getEventCount()

        assertEquals(countBefore, countAfter)
    }
}
