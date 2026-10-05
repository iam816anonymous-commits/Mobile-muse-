package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.result.ResultCode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class EventLogActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()

        app.eventRepository.insertEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = app.eventLogger.getActiveSession().sessionId,
                timestamp = System.currentTimeMillis() - 1000,
                subsystem = EventSubsystem.COMMAND,
                eventType = "EVENT_LOG_TEST_EVENT",
                resultCode = ResultCode.SUCCESS_VERIFIED
            )
        )
    }

    @Test
    fun testEventLogActivityHydratesStream() {
        val controller = Robolectric.buildActivity(EventLogActivity::class.java).create().start().resume()
        val activity = controller.get()

        Thread.sleep(200)

        val logStream = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvEventLogStream).text.toString()
        assertTrue(logStream.contains("EVENT_LOG_TEST_EVENT"))
    }
}
