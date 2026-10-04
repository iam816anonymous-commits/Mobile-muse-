package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.localagent.app.LocalAgentApplication
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationSnapshot
import java.util.UUID

class AgentAccessibilityService : AccessibilityService() {

    @Volatile
    var activePackageName: String = ""
        private set

    @Volatile
    var activeActivityName: String? = null
        private set

    private val extractor = ObservationSnapshotExtractor(maxNodes = 500, maxDepth = 30)
    private var lastEventTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        INSTANCE = this

        val app = application as? LocalAgentApplication
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = app.eventLogger.getActiveSession().sessionId,
                subsystem = EventSubsystem.ACCESSIBILITY,
                eventType = "ACCESSIBILITY_SERVICE_CONNECTED",
                severity = EventSeverity.INFO,
                metadataJson = "{\"status\":\"BOUND\"}"
            )
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        if (now - lastEventTimestamp < 150) { // 150ms event debouncing
            return
        }
        lastEventTimestamp = now

        event.packageName?.toString()?.let { pkg ->
            if (pkg.isNotBlank()) activePackageName = pkg
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.className?.toString()?.let { cls ->
                if (cls.isNotBlank() && cls.contains(".")) {
                    activeActivityName = cls
                }
            }
        }
    }

    override fun onInterrupt() {
        val app = application as? LocalAgentApplication
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: "",
                subsystem = EventSubsystem.ACCESSIBILITY,
                eventType = "ACCESSIBILITY_SERVICE_INTERRUPTED",
                severity = EventSeverity.WARNING
            )
        )
    }

    override fun onUnbind(intent: Intent?): Boolean {
        INSTANCE = null
        val app = application as? LocalAgentApplication
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: "",
                subsystem = EventSubsystem.ACCESSIBILITY,
                eventType = "ACCESSIBILITY_SERVICE_UNBOUND",
                severity = EventSeverity.WARNING
            )
        )
        return super.onUnbind(intent)
    }

    fun captureLiveSnapshot(): ObservationSnapshot {
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "OBSERVATION_STARTED"
            )
        )

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            val failedSnapshot = ObservationSnapshot(
                packageName = activePackageName,
                activityName = activeActivityName,
                nodeCount = 0
            )
            app?.eventLogger?.logEvent(
                AgentEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = activeSessionId,
                    subsystem = EventSubsystem.OBSERVATION,
                    eventType = "OBSERVATION_FAILED",
                    severity = EventSeverity.WARNING,
                    metadataJson = "{\"reason\":\"rootInActiveWindow returned null\"}"
                )
            )
            return failedSnapshot
        }

        try {
            val snapshot = extractor.extractSnapshot(
                rootNodeInfo = rootNode,
                packageName = activePackageName,
                activityName = activeActivityName,
                windowId = rootNode.windowId
            )

            val eventType = if (snapshot.truncationInfo.isTruncated) {
                "OBSERVATION_TRUNCATED"
            } else {
                "OBSERVATION_COMPLETED"
            }

            app?.eventLogger?.logEvent(
                AgentEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = activeSessionId,
                    subsystem = EventSubsystem.OBSERVATION,
                    eventType = eventType,
                    metadataJson = "{\"nodeCount\":${snapshot.nodeCount},\"truncated\":${snapshot.truncationInfo.isTruncated}}"
                )
            )

            return snapshot
        } finally {
            rootNode.recycle()
        }
    }

    companion object {
        @Volatile
        var INSTANCE: AgentAccessibilityService? = null
            private set

        val isBound: Boolean
            get() = INSTANCE != null
    }
}
