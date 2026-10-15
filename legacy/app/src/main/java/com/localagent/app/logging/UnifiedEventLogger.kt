package com.localagent.app.logging

import com.localagent.core.logging.*
import java.util.UUID
import java.util.concurrent.Executors

class UnifiedEventLogger(
    private val repository: EventRepository,
    private val maxRowsToKeep: Long = 50000L,
    private val maxStorageBytes: Long = 30 * 1024 * 1024L // 30 MB
) : EventLogger {

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var currentSession: AgentSession = initializeSession()

    override fun logEvent(event: AgentEvent) {
        val eventWithSession = if (event.sessionId.isBlank()) {
            event.copy(sessionId = currentSession.sessionId)
        } else {
            event
        }

        executor.execute {
            try {
                repository.insertEvent(eventWithSession)

                // Enforce low-storage cap check
                val currentFootprint = repository.getStorageFootprintBytes()
                val currentCount = repository.getEventCount()

                if (currentCount > maxRowsToKeep || currentFootprint > maxStorageBytes) {
                    repository.pruneEvents(maxRowsToKeep = maxRowsToKeep / 2)
                }
            } catch (e: Throwable) {
                // Logging failure MUST NOT crash the agent process
                System.err.println("UnifiedEventLogger error persisting event: ${e.message}")
            }
        }
    }

    override fun getActiveSession(): AgentSession {
        return currentSession
    }

    override fun startNewSession(reason: String): AgentSession {
        synchronized(this) {
            // Close active session if present
            currentSession.let { active ->
                val closed = active.copy(
                    endTime = System.currentTimeMillis(),
                    status = "COMPLETED",
                    metadataJson = "{\"closeReason\":\"$reason\"}"
                )
                executor.execute {
                    try { repository.updateSession(closed) } catch (_: Throwable) {}
                }
            }

            val newSession = AgentSession(
                sessionId = UUID.randomUUID().toString(),
                startTime = System.currentTimeMillis(),
                status = "ACTIVE",
                metadataJson = "{\"startReason\":\"$reason\"}"
            )
            currentSession = newSession
            executor.execute {
                try { repository.insertSession(newSession) } catch (_: Throwable) {}
            }
            return newSession
        }
    }

    private fun initializeSession(): AgentSession {
        val active = try { repository.getActiveSession() } catch (e: Throwable) { null }
        if (active != null) {
            return active
        }

        val session = AgentSession(
            sessionId = UUID.randomUUID().toString(),
            startTime = System.currentTimeMillis(),
            status = "ACTIVE",
            metadataJson = "{\"startReason\":\"PROCESS_START\"}"
        )
        executor.execute {
            try { repository.insertSession(session) } catch (_: Throwable) {}
        }
        return session
    }

    fun shutdown() {
        executor.shutdown()
    }
}
