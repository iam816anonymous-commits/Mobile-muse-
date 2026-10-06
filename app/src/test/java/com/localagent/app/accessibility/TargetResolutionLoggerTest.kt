package com.localagent.app.accessibility

import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventLogger
import com.localagent.core.logging.AgentSession
import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.resolver.TargetActionType
import com.localagent.core.resolver.TargetResolutionResult
import com.localagent.core.resolver.TargetResolutionStatus
import com.localagent.core.resolver.TargetResolutionStrategy
import org.junit.Assert.*
import org.junit.Test

class TargetResolutionLoggerTest {

    private class FakeEventLogger : EventLogger {
        val loggedEvents = mutableListOf<AgentEvent>()
        val activeSession = AgentSession(
            sessionId = "test-session-123",
            startTime = System.currentTimeMillis()
        )

        override fun logEvent(event: AgentEvent) {
            loggedEvents.add(event)
        }

        override fun getActiveSession(): AgentSession = activeSession

        override fun startNewSession(reason: String): AgentSession = activeSession
    }

    @Test
    fun `logTargetResolution passes eventId and deduplicates within request`() {
        val fakeLogger = FakeEventLogger()
        val targetLogger = TargetResolutionLogger(fakeLogger)

        val result = TargetResolutionResult(
            requestedNodeId = "text_7",
            resolvedNodeId = "btn_7",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.CLICKABLE_ANCESTOR,
            confidence = NodeIdentityConfidence.EXACT,
            status = TargetResolutionStatus.RESOLVED
        )

        targetLogger.beginRequest()
        targetLogger.logTargetResolution(result)
        targetLogger.logTargetResolution(result) // duplicate call
        targetLogger.endRequest()

        assertEquals(1, fakeLogger.loggedEvents.size)

        val event = fakeLogger.loggedEvents[0]
        assertTrue(event.eventId.isNotBlank())
        assertEquals("test-session-123", event.sessionId)
        assertEquals("TARGET_RESOLUTION_RESOLVED", event.eventType)
        assertEquals("RESOLVE_CLICK", event.actionType)
        assertEquals("TARGET_RESOLVER", event.sourceChannel)
    }
}
