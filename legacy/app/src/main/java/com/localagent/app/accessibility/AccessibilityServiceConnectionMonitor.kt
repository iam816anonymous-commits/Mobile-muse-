package com.localagent.app.accessibility

import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import com.localagent.app.LocalAgentApplication
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

enum class AccessibilityLifecycleState {
    UNBOUND,
    CONNECTING,
    BOUND,
    DISCONNECTING,
    DEGRADED
}

data class ServiceConnectionInfo(
    val state: AccessibilityLifecycleState = AccessibilityLifecycleState.UNBOUND,
    val connectedTimestampMs: Long = 0L,
    val disconnectedTimestampMs: Long = 0L,
    val lastStateChangeTimestampMs: Long = System.currentTimeMillis(),
    val isSystemAccessibilityEnabled: Boolean = false
)

interface AccessibilityServiceLifecycleListener {
    fun onAccessibilityLifecycleStateChanged(info: ServiceConnectionInfo)
}

class AccessibilityServiceConnectionMonitor(private val context: Context) {

    private val accessibilityManager: AccessibilityManager? =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private val listeners = CopyOnWriteArrayList<AccessibilityServiceLifecycleListener>()

    @Volatile
    var connectionInfo: ServiceConnectionInfo = ServiceConnectionInfo()
        private set

    private val systemStateChangeListener = AccessibilityManager.AccessibilityStateChangeListener { enabled ->
        val app = context.applicationContext as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACCESSIBILITY,
                eventType = "SYSTEM_ACCESSIBILITY_STATE_CHANGED",
                severity = EventSeverity.INFO,
                metadataJson = "{\"systemEnabled\":$enabled}"
            )
        )

        val newState = if (enabled && AgentAccessibilityService.isBound) {
            AccessibilityLifecycleState.BOUND
        } else if (enabled) {
            AccessibilityLifecycleState.CONNECTING
        } else {
            AccessibilityLifecycleState.DEGRADED
        }

        updateState(
            newState = newState,
            isSystemEnabled = enabled
        )
    }

    init {
        val initialSystemEnabled = accessibilityManager?.isEnabled ?: false
        val initialState = if (AgentAccessibilityService.isBound) {
            AccessibilityLifecycleState.BOUND
        } else if (initialSystemEnabled) {
            AccessibilityLifecycleState.UNBOUND
        } else {
            AccessibilityLifecycleState.DEGRADED
        }

        connectionInfo = ServiceConnectionInfo(
            state = initialState,
            isSystemAccessibilityEnabled = initialSystemEnabled
        )

        try {
            accessibilityManager?.addAccessibilityStateChangeListener(systemStateChangeListener)
        } catch (e: Exception) {
            System.err.println("AccessibilityServiceConnectionMonitor: Error registering state listener: ${e.message}")
        }
    }

    fun addListener(listener: AccessibilityServiceLifecycleListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            listener.onAccessibilityLifecycleStateChanged(connectionInfo)
        }
    }

    fun removeListener(listener: AccessibilityServiceLifecycleListener) {
        listeners.remove(listener)
    }

    fun notifyServiceConnected() {
        val now = System.currentTimeMillis()
        updateState(
            newState = AccessibilityLifecycleState.BOUND,
            connectedTimestamp = now,
            isSystemEnabled = accessibilityManager?.isEnabled ?: true
        )

        val app = context.applicationContext as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACCESSIBILITY,
                eventType = "AGENT_SERVICE_CONNECTED",
                severity = EventSeverity.INFO,
                metadataJson = "{\"connectedTimestampMs\":$now,\"state\":\"BOUND\"}"
            )
        )
    }

    fun notifyServiceDisconnected(reason: String = "UNBOUND") {
        val now = System.currentTimeMillis()
        val newState = AccessibilityLifecycleState.DEGRADED
        updateState(
            newState = newState,
            disconnectedTimestamp = now,
            isSystemEnabled = accessibilityManager?.isEnabled ?: false
        )

        val app = context.applicationContext as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACCESSIBILITY,
                eventType = "AGENT_SERVICE_DISCONNECTED",
                severity = EventSeverity.WARNING,
                metadataJson = "{\"disconnectedTimestampMs\":$now,\"reason\":\"$reason\",\"state\":\"DEGRADED\"}"
            )
        )
    }

    private fun updateState(
        newState: AccessibilityLifecycleState,
        connectedTimestamp: Long = connectionInfo.connectedTimestampMs,
        disconnectedTimestamp: Long = connectionInfo.disconnectedTimestampMs,
        isSystemEnabled: Boolean = connectionInfo.isSystemAccessibilityEnabled
    ) {
        val updated = ServiceConnectionInfo(
            state = newState,
            connectedTimestampMs = connectedTimestamp,
            disconnectedTimestampMs = disconnectedTimestamp,
            lastStateChangeTimestampMs = System.currentTimeMillis(),
            isSystemAccessibilityEnabled = isSystemEnabled
        )
        connectionInfo = updated

        listeners.forEach { listener ->
            try {
                listener.onAccessibilityLifecycleStateChanged(updated)
            } catch (e: Exception) {
                System.err.println("AccessibilityServiceConnectionMonitor: Error notifying listener: ${e.message}")
            }
        }
    }

    fun cleanup() {
        try {
            accessibilityManager?.removeAccessibilityStateChangeListener(systemStateChangeListener)
        } catch (e: Exception) {
            System.err.println("AccessibilityServiceConnectionMonitor: Error unregistering listener: ${e.message}")
        }
        listeners.clear()
    }
}
