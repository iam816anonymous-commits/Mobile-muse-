package com.localagent.app.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.logging.UnifiedEventLogger
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventFilter
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.result.ResultCode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class RoomEventRepositoryTest {

    private lateinit var database: AgentDatabase
    private lateinit var repository: RoomEventRepository
    private lateinit var logger: UnifiedEventLogger
    private lateinit var tempDbFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<LocalAgentApplication>()
        tempDbFile = File(context.cacheDir, "test_agent_${UUID.randomUUID()}.db")

        database = Room.inMemoryDatabaseBuilder(context, AgentDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = RoomEventRepository(database.eventDao(), database.sessionDao(), tempDbFile)
        logger = UnifiedEventLogger(repository)
    }

    @After
    fun tearDown() {
        logger.shutdown()
        database.close()
        if (tempDbFile.exists()) tempDbFile.delete()
    }

    @Test
    fun `testEventInsertionAndQueryReadBack`() {
        val activeSession = logger.getActiveSession()
        assertNotNull(activeSession)

        val event1 = AgentEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = activeSession.sessionId,
            subsystem = EventSubsystem.COMMAND,
            eventType = "COMMAND_INPUT_RECEIVED",
            actionType = "UI_CLICK",
            severity = EventSeverity.INFO,
            resultCode = ResultCode.SUCCESS_VERIFIED,
            metadataJson = "{\"rawInput\":\"click 6\"}"
        )

        val event2 = AgentEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = activeSession.sessionId,
            timestamp = event1.timestamp + 10L,
            subsystem = EventSubsystem.ACTION,
            eventType = "ACTION_RESULT",
            actionType = "UI_CLICK",
            severity = EventSeverity.INFO,
            resultCode = ResultCode.SUCCESS_VERIFIED,
            metadataJson = "{\"target\":\"digit_6\"}"
        )

        repository.insertEvent(event1)
        repository.insertEvent(event2)

        val count = repository.getEventCount()
        assertEquals(2L, count)

        val queriedEvents = repository.queryEvents(EventFilter(limit = 100))
        assertEquals(2, queriedEvents.size)

        val latest = queriedEvents[0]
        assertEquals(event2.eventId, latest.eventId)
        assertEquals("ACTION_RESULT", latest.eventType)
        assertEquals(ResultCode.SUCCESS_VERIFIED, latest.resultCode)

        val oldest = queriedEvents[1]
        assertEquals(event1.eventId, oldest.eventId)
        assertEquals("COMMAND_INPUT_RECEIVED", oldest.eventType)
    }
}
