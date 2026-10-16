package com.localagent.core.logging

import com.localagent.core.command.CommandSource
import com.localagent.core.result.ResultCode

enum class EventSubsystem {
    COMMAND, ACCESSIBILITY, OBSERVATION, ACTION, PERMISSION, HARDWARE,
    VOICE, WORKFLOW, MEMORY, LEARNING, BROWSER, SOLVER, SYSTEM, TEST,
    POLICY, OVERLAY, AI_PLANNER, RECOVERY, ERROR
}

enum class EventSeverity {
    DEBUG, INFO, WARNING, ERROR, CRITICAL
}

data class AgentEvent(
    val eventId: String,
    val sessionId: String,
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val subsystem: EventSubsystem,
    val eventType: String,
    val actionType: String? = null,
    val sourceChannel: String = CommandSource.CONSOLE.name,
    val targetPackage: String? = null,
    val targetViewId: String? = null,
    val resultCode: ResultCode? = null,
    val durationMs: Long = 0L,
    val severity: EventSeverity = EventSeverity.INFO,
    val errorCode: String? = null,
    val metadataJson: String = "{}"
)

data class AgentSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, INTERRUPTED
    val metadataJson: String = "{}"
)

data class EventFilter(
    val sessionId: String? = null,
    val subsystem: EventSubsystem? = null,
    val severity: EventSeverity? = null,
    val actionType: String? = null,
    val resultCode: ResultCode? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

interface EventLogger {
    fun logEvent(event: AgentEvent)
    fun getActiveSession(): AgentSession
    fun startNewSession(reason: String): AgentSession
}

interface EventRepository {
    fun insertEvent(event: AgentEvent)
    fun insertSession(session: AgentSession)
    fun updateSession(session: AgentSession)
    fun getActiveSession(): AgentSession?
    fun getSessionById(sessionId: String): AgentSession?
    fun queryEvents(filter: EventFilter): List<AgentEvent>
    fun getEventCount(sessionId: String? = null): Long
    fun pruneEvents(maxRowsToKeep: Long, maxAgeMs: Long? = null): Int
    fun getStorageFootprintBytes(): Long
}
