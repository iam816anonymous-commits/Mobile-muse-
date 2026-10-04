package com.localagent.app.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.logging.UnifiedEventLogger
import com.localagent.core.command.CommandSource
import com.localagent.core.logging.*
import com.localagent.core.result.ResultCode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class RoomEventRepositoryTest {

    private lateinit var db: AgentDatabase
    private lateinit var repository: RoomEventRepository
    private lateinit var dbFile: File

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dbFile = File(context.cacheDir, "test_agent.db")
        db = Room.inMemoryDatabaseBuilder(context, AgentDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = RoomEventRepository(db.eventDao(), db.sessionDao(), dbFile)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndQueryEvent() {
        val session = AgentSession(
            sessionId = "session-123",
            startTime = System.currentTimeMillis()
        )
        repository.insertSession(session)

        val event = AgentEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = "session-123",
            subsystem = EventSubsystem.COMMAND,
            eventType = "COMMAND_RECEIVED",
            actionType = "UI_CLICK",
            sourceChannel = CommandSource.CONSOLE.name,
            resultCode = ResultCode.SUCCESS_VERIFIED,
            metadataJson = "{\"testKey\":\"testValue\"}"
        )
        repository.insertEvent(event)

        val queried = repository.queryEvents(EventFilter(sessionId = "session-123"))
        assertEquals(1, queried.size)
        assertEquals(event.eventId, queried[0].eventId)
        assertEquals(EventSubsystem.COMMAND, queried[0].subsystem)
        assertEquals(ResultCode.SUCCESS_VERIFIED, queried[0].resultCode)
    }

    @Test
    fun testPruningPolicy() {
        val session = AgentSession(sessionId = "session-prune", startTime = System.currentTimeMillis())
        repository.insertSession(session)

        for (i in 1..20) {
            val event = AgentEvent(
                eventId = "event-$i",
                sessionId = "session-prune",
                timestamp = System.currentTimeMillis() + i,
                subsystem = EventSubsystem.SYSTEM,
                eventType = "TEST_EVENT"
            )
            repository.insertEvent(event)
        }

        assertEquals(20, repository.getEventCount("session-prune"))

        val pruned = repository.pruneEvents(maxRowsToKeep = 10)
        assertEquals(10, pruned)
        assertEquals(10, repository.getEventCount("session-prune"))
    }

    @Test
    fun testSanitizationOfSensitiveMetadata() {
        val session = AgentSession(sessionId = "session-san", startTime = System.currentTimeMillis())
        repository.insertSession(session)

        val event = AgentEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = "session-san",
            subsystem = EventSubsystem.COMMAND,
            eventType = "SENSITIVE_INPUT",
            metadataJson = "{\"password\": \"secret123\", \"pin\": \"4321\"}"
        )
        repository.insertEvent(event)

        val queried = repository.queryEvents(EventFilter(sessionId = "session-san"))
        assertEquals(1, queried.size)
        assertFalse(queried[0].metadataJson.contains("secret123"))
        assertFalse(queried[0].metadataJson.contains("4321"))
        assertTrue(queried[0].metadataJson.contains("[REDACTED]"))
    }

    @Test
    fun testUnifiedLoggerSessionManagement() {
        val logger = UnifiedEventLogger(repository)
        val session1 = logger.getActiveSession()
        assertNotNull(session1.sessionId)

        val session2 = logger.startNewSession("USER_REQUEST")
        assertNotEquals(session1.sessionId, session2.sessionId)
        assertTrue(logger.getActiveSession().metadataJson.contains("USER_REQUEST"))

        logger.shutdown()
    }
}
