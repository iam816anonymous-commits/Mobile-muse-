package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.result.ResultCode
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class MainActivityEventHydrationTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()

        val session = app.eventLogger.getActiveSession()
        app.eventRepository.insertEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = session.sessionId,
                timestamp = System.currentTimeMillis() + 1000,
                subsystem = EventSubsystem.COMMAND,
                eventType = "HISTORICAL_EVENT_001",
                actionType = "UI_CLICK",
                resultCode = ResultCode.SUCCESS_VERIFIED
            )
        )
    }

    @Test
    fun testEventHistoryHydratedFromDatabaseOnLaunch() {
        val activityController = Robolectric.buildActivity(EventLogActivity::class.java).create().start().resume()
        val activity = activityController.get()

        Thread.sleep(300)

        val logStreamText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvEventLogStream).text.toString()

        assertTrue(logStreamText.contains("HISTORICAL_EVENT_001"), "Expected UI to hydrate HISTORICAL_EVENT_001 from agent.db. Actual: $logStreamText")
    }
}
