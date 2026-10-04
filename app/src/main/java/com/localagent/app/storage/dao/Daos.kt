package com.localagent.app.storage.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.localagent.app.storage.entity.AgentEventEntity
import com.localagent.app.storage.entity.AgentSessionEntity

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertEvent(event: AgentEventEntity)

    @RawQuery
    fun queryEventsRaw(query: SupportSQLiteQuery): List<AgentEventEntity>

    @Query("SELECT COUNT(*) FROM agent_events")
    fun getTotalEventCount(): Long

    @Query("SELECT COUNT(*) FROM agent_events WHERE sessionId = :sessionId")
    fun getSessionEventCount(sessionId: String): Long

    @Query("DELETE FROM agent_events WHERE timestamp < :cutoffTime")
    fun deleteEventsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM agent_events WHERE rowid IN (SELECT rowid FROM agent_events ORDER BY timestamp ASC LIMIT :limit)")
    fun deleteOldestEvents(limit: Int): Int
}

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSession(session: AgentSessionEntity)

    @Update
    fun updateSession(session: AgentSessionEntity)

    @Query("SELECT * FROM agent_sessions WHERE status = 'ACTIVE' ORDER BY startTime DESC LIMIT 1")
    fun getActiveSession(): AgentSessionEntity?

    @Query("SELECT * FROM agent_sessions WHERE sessionId = :sessionId")
    fun getSessionById(sessionId: String): AgentSessionEntity?
}
