package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    @Volatile
    var lastExternalPackageName: String = ""
        private set

    @Volatile
    var lastExternalActivityName: String? = null
        private set

    @Volatile
    var currentObservationSnapshot: ObservationSnapshot? = null
        private set

    @Volatile
    var lastExternalObservationSnapshot: ObservationSnapshot? = null
        private set

    private val extractor = ObservationSnapshotExtractor(maxNodes = 500, maxDepth = 30)

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

        val pkg = event.packageName?.toString() ?: ""
        if (pkg.isNotBlank()) {
            activePackageName = pkg
            if (pkg != "com.localagent.app") {
                lastExternalPackageName = pkg
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.className?.toString()?.let { cls ->
                if (cls.isNotBlank() && cls.contains(".")) {
                    activeActivityName = cls
                    if (pkg.isNotBlank() && pkg != "com.localagent.app") {
                        lastExternalActivityName = cls
                    }
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

        // 1. Capture primary active root node
        val rootNode = rootInActiveWindow
        val primarySnapshot = if (rootNode != null) {
            try {
                extractor.extractSnapshot(
                    rootNodeInfo = rootNode,
                    packageName = rootNode.packageName?.toString() ?: activePackageName,
                    activityName = activeActivityName,
                    windowId = rootNode.windowId
                )
            } finally {
                rootNode.recycle()
            }
        } else {
            ObservationSnapshot(
                packageName = activePackageName,
                activityName = activeActivityName,
                nodeCount = 0
            )
        }

        currentObservationSnapshot = primarySnapshot

        val primaryPkg = primarySnapshot.packageName.ifBlank { activePackageName }
        if (primaryPkg.isNotBlank() && primaryPkg != "com.localagent.app") {
            lastExternalObservationSnapshot = primarySnapshot
        }

        // 2. If primary snapshot is LocalAgent, attempt to inspect interactive windows for last external window
        if (primaryPkg == "com.localagent.app" || primaryPkg.isBlank()) {
            val externalSnapshotFromWindows = findExternalWindowSnapshot()
            if (externalSnapshotFromWindows != null) {
                lastExternalObservationSnapshot = externalSnapshotFromWindows
            }
        }

        val eventType = if (primarySnapshot.truncationInfo.isTruncated) {
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
                metadataJson = "{\"pkg\":\"$primaryPkg\",\"nodeCount\":${primarySnapshot.nodeCount},\"truncated\":${primarySnapshot.truncationInfo.isTruncated}}"
            )
        )

        return primarySnapshot
    }

    private fun findExternalWindowSnapshot(): ObservationSnapshot? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null

        try {
            val interactiveWindows = windows ?: return null
            for (window in interactiveWindows) {
                val windowRoot: AccessibilityNodeInfo? = window.root
                if (windowRoot != null) {
                    try {
                        val pkg = windowRoot.packageName?.toString() ?: ""
                        if (pkg.isNotBlank() && pkg != "com.localagent.app") {
                            return extractor.extractSnapshot(
                                rootNodeInfo = windowRoot,
                                packageName = pkg,
                                activityName = lastExternalActivityName,
                                windowId = window.id
                            )
                        }
                    } finally {
                        windowRoot.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("AgentAccessibilityService: Error inspecting windows: ${e.message}")
        }
        return null
    }

    companion object {
        @Volatile
        var INSTANCE: AgentAccessibilityService? = null
            private set

        val isBound: Boolean
            get() = INSTANCE != null
    }
}
