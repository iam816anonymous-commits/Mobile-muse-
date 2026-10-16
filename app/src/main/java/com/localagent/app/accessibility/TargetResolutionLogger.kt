package com.localagent.app.accessibility

import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventLogger
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.result.ResultCode
import com.localagent.core.resolver.TargetResolutionResult
import com.localagent.core.resolver.TargetResolutionStatus
import java.util.UUID

class TargetResolutionLogger(
    private val eventLogger: EventLogger
) {
    private val loggedResolutionsInRequest = mutableSetOf<String>()

    fun beginRequest() {
        loggedResolutionsInRequest.clear()
    }

    fun logTargetResolution(result: TargetResolutionResult) {
        val deduplicationKey = "${result.requestedNodeId}_${result.actionType.name}_${result.status.name}"
        if (loggedResolutionsInRequest.contains(deduplicationKey)) {
            return
        }
        loggedResolutionsInRequest.add(deduplicationKey)

        val eventType = when (result.status) {
            TargetResolutionStatus.RESOLVED -> if (result.reacquired) "TARGET_REACQUISITION_RESOLVED" else "TARGET_RESOLUTION_RESOLVED"
            else -> if (result.reacquired) "TARGET_REACQUISITION_FAILED" else "TARGET_RESOLUTION_FAILED"
        }

        val resultCode = when (result.status) {
            TargetResolutionStatus.RESOLVED -> ResultCode.DISPATCHED_BUT_NOT_VERIFIED
            TargetResolutionStatus.TARGET_NOT_FOUND -> ResultCode.TARGET_NOT_FOUND
            TargetResolutionStatus.NO_ACTIONABLE_TARGET -> ResultCode.TARGET_NOT_ACTIONABLE
            TargetResolutionStatus.STALE_SNAPSHOT -> ResultCode.TARGET_STALE
            TargetResolutionStatus.INVALID_REQUEST -> ResultCode.INVALID_INPUT
        }

        val activeSession = eventLogger.getActiveSession()

        val event = AgentEvent(
            eventId = UUID.randomUUID().toString(),
            sessionId = activeSession.sessionId,
            subsystem = EventSubsystem.ACCESSIBILITY,
            eventType = eventType,
            actionType = "RESOLVE_${result.actionType.name}",
            sourceChannel = "TARGET_RESOLVER",
            targetViewId = result.resolvedNodeIdentity ?: result.requestedNodeIdentity ?: result.requestedNodeId,
            resultCode = resultCode,
            severity = if (result.status == TargetResolutionStatus.RESOLVED) EventSeverity.INFO else EventSeverity.WARNING,
            errorCode = result.failureReason,
            metadataJson = result.toJsonObject().toString()
        )

        eventLogger.logEvent(event)
    }

    fun endRequest() {
        loggedResolutionsInRequest.clear()
    }
}
