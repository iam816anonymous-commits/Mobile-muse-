package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.logging.EventFilter
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.result.ResultCode
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class MainActivityPhase2Test {

    private lateinit var activity: MainActivity
    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<LocalAgentApplication>()
        app.initializeCoreDomain()
        activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().get()
    }

    @Test
    fun testValidCommandProducesPersistentEvent() {
        try {
            activity.executeCommandFromInput("click 7")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        Thread.sleep(300)

        val events = app.eventRepository.queryEvents(EventFilter(limit = 20))
        assertTrue(events.isNotEmpty())

        val inputEvent = events.find { it.eventType == "COMMAND_INPUT_RECEIVED" }
        assertNotNull(inputEvent)
        assertTrue(inputEvent.metadataJson.contains("click 7"))

        val parsedEvent = events.find { it.eventType == "COMMAND_PARSED" }
        assertNotNull(parsedEvent)
        assertEquals("UI_CLICK", parsedEvent.actionType)
    }

    @Test
    fun testUnknownCommandRejectionIsPersisted() {
        try {
            activity.executeCommandFromInput("not real cmd")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        Thread.sleep(300)

        val events = app.eventRepository.queryEvents(EventFilter(limit = 20))
        val rejectedEvent = events.find { it.eventType == "COMMAND_REJECTED" }

        assertNotNull(rejectedEvent)
        assertEquals(ResultCode.UNKNOWN_COMMAND, rejectedEvent.resultCode)
        assertEquals(EventSubsystem.COMMAND, rejectedEvent.subsystem)
        assertTrue(rejectedEvent.metadataJson.contains("not real cmd"))
    }

    @Test
    fun testEmptyInputRejectionIsPersisted() {
        try {
            activity.executeCommandFromInput("   ")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        Thread.sleep(300)

        val events = app.eventRepository.queryEvents(EventFilter(limit = 20))
        val rejectedEvent = events.find { it.eventType == "COMMAND_REJECTED" }

        assertNotNull(rejectedEvent)
        assertEquals(ResultCode.INVALID_INPUT, rejectedEvent.resultCode)
    }
}
