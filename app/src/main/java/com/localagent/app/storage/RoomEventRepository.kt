package com.localagent.app.storage

import androidx.sqlite.db.SimpleSQLiteQuery
import com.localagent.app.storage.dao.EventDao
import com.localagent.app.storage.dao.SessionDao
import com.localagent.app.storage.entity.AgentEventEntity
import com.localagent.app.storage.entity.AgentSessionEntity
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.AgentSession
import com.localagent.core.logging.EventFilter
import com.localagent.core.logging.EventRepository
import java.io.File

class RoomEventRepository(
    private val eventDao: EventDao,
    private val sessionDao: SessionDao,
    private val dbFile: File
) : EventRepository {

    override fun insertEvent(event: AgentEvent) {
        eventDao.insertEvent(AgentEventEntity.fromDomain(sanitizeEvent(event)))
    }

    override fun insertSession(session: AgentSession) {
        sessionDao.insertSession(AgentSessionEntity.fromDomain(session))
    }

    override fun updateSession(session: AgentSession) {
        sessionDao.updateSession(AgentSessionEntity.fromDomain(session))
    }

    override fun getActiveSession(): AgentSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    override fun getSessionById(sessionId: String): AgentSession? {
        return sessionDao.getSessionById(sessionId)?.toDomain()
    }

    override fun queryEvents(filter: EventFilter): List<AgentEvent> {
        val sqlBuilder = StringBuilder("SELECT * FROM agent_events WHERE 1=1")
        val args = mutableListOf<Any>()

        filter.sessionId?.let {
            sqlBuilder.append(" AND sessionId = ?")
            args.add(it)
        }
        filter.subsystem?.let {
            sqlBuilder.append(" AND subsystem = ?")
            args.add(it.name)
        }
        filter.severity?.let {
            sqlBuilder.append(" AND severity = ?")
            args.add(it.name)
        }
        filter.actionType?.let {
            sqlBuilder.append(" AND actionType = ?")
            args.add(it)
        }
        filter.resultCode?.let {
            sqlBuilder.append(" AND resultCode = ?")
            args.add(it.name)
        }
        filter.startTime?.let {
            sqlBuilder.append(" AND timestamp >= ?")
            args.add(it)
        }
        filter.endTime?.let {
            sqlBuilder.append(" AND timestamp <= ?")
            args.add(it)
        }

        sqlBuilder.append(" ORDER BY timestamp DESC LIMIT ? OFFSET ?")
        args.add(filter.limit)
        args.add(filter.offset)

        val query = SimpleSQLiteQuery(sqlBuilder.toString(), args.toTypedArray())
        return eventDao.queryEventsRaw(query).map { it.toDomain() }
    }

    override fun getEventCount(sessionId: String?): Long {
        return if (sessionId == null) {
            eventDao.getTotalEventCount()
        } else {
            eventDao.getSessionEventCount(sessionId)
        }
    }

    override fun pruneEvents(maxRowsToKeep: Long, maxAgeMs: Long?): Int {
        var deleted = 0
        if (maxAgeMs != null) {
            val cutoff = System.currentTimeMillis() - maxAgeMs
            deleted += eventDao.deleteEventsOlderThan(cutoff)
        }

        val currentCount = eventDao.getTotalEventCount()
        if (currentCount > maxRowsToKeep) {
            val excess = (currentCount - maxRowsToKeep).toInt()
            deleted += eventDao.deleteOldestEvents(excess)
        }
        return deleted
    }

    override fun getStorageFootprintBytes(): Long {
        var size = 0L
        if (dbFile.exists()) size += dbFile.length()

        val walFile = File(dbFile.parentFile, "${dbFile.name}-wal")
        if (walFile.exists()) size += walFile.length()

        val shmFile = File(dbFile.parentFile, "${dbFile.name}-shm")
        if (shmFile.exists()) size += shmFile.length()

        return size
    }

    private fun sanitizeEvent(event: AgentEvent): AgentEvent {
        val sanitizedMetadata = sanitizeText(event.metadataJson)
        return event.copy(metadataJson = sanitizedMetadata)
    }

    private fun sanitizeText(input: String): String {
        var sanitized = input.replace(Regex("(?i)(\"(?:password|token|secret|pin|credit_card)\"\\s*:\\s*\")[^\"]+(\")")) {
            "${it.groupValues[1]}[REDACTED]${it.groupValues[2]}"
        }
        sanitized = sanitized.replace(Regex("(?i)(\"(?:password|token|secret|pin|credit_card)\"\\s*:\\s*)[0-9a-zA-Z_.]+")) {
            "${it.groupValues[1]}\"[REDACTED]\""
        }
        return sanitized
    }
}
