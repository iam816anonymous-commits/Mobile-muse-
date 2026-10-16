package com.localagent.app.storage.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.AgentSession
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.result.ResultCode

@Entity(tableName = "agent_events")
data class AgentEventEntity(
    @PrimaryKey
    val eventId: String,
    val sessionId: String,
    val correlationId: String?,
    val timestamp: Long,
    val subsystem: String,
    val eventType: String,
    val actionType: String?,
    val sourceChannel: String,
    val targetPackage: String?,
    val targetViewId: String?,
    val resultCode: String?,
    val durationMs: Long,
    val severity: String,
    val errorCode: String?,
    val metadataJson: String
) {
    fun toDomain(): AgentEvent {
        return AgentEvent(
            eventId = eventId,
            sessionId = sessionId,
            correlationId = correlationId,
            timestamp = timestamp,
            subsystem = try { EventSubsystem.valueOf(subsystem) } catch (e: Exception) { EventSubsystem.SYSTEM },
            eventType = eventType,
            actionType = actionType,
            sourceChannel = sourceChannel,
            targetPackage = targetPackage,
            targetViewId = targetViewId,
            resultCode = resultCode?.let { try { ResultCode.valueOf(it) } catch (e: Exception) { null } },
            durationMs = durationMs,
            severity = try { EventSeverity.valueOf(severity) } catch (e: Exception) { EventSeverity.INFO },
            errorCode = errorCode,
            metadataJson = metadataJson
        )
    }

    companion object {
        fun fromDomain(event: AgentEvent): AgentEventEntity {
            return AgentEventEntity(
                eventId = event.eventId,
                sessionId = event.sessionId,
                correlationId = event.correlationId,
                timestamp = event.timestamp,
                subsystem = event.subsystem.name,
                eventType = event.eventType,
                actionType = event.actionType,
                sourceChannel = event.sourceChannel,
                targetPackage = event.targetPackage,
                targetViewId = event.targetViewId,
                resultCode = event.resultCode?.name,
                durationMs = event.durationMs,
                severity = event.severity.name,
                errorCode = event.errorCode,
                metadataJson = event.metadataJson
            )
        }
    }
}

@Entity(tableName = "agent_sessions")
data class AgentSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val startTime: Long,
    val endTime: Long?,
    val status: String,
    val metadataJson: String
) {
    fun toDomain(): AgentSession {
        return AgentSession(
            sessionId = sessionId,
            startTime = startTime,
            endTime = endTime,
            status = status,
            metadataJson = metadataJson
        )
    }

    companion object {
        fun fromDomain(session: AgentSession): AgentSessionEntity {
            return AgentSessionEntity(
                sessionId = session.sessionId,
                startTime = session.startTime,
                endTime = session.endTime,
                status = session.status,
                metadataJson = session.metadataJson
            )
        }
    }
}
