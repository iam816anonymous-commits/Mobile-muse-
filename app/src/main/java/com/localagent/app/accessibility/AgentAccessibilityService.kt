package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.localagent.app.LocalAgentApplication
import com.localagent.core.evidence.EvidenceSource
import com.localagent.core.evidence.ObservationEvidence
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationSnapshot
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

data class WindowCandidateInfo(
    val windowId: Int,
    val windowType: Int,
    val windowTypeName: String,
    val packageName: String,
    val isActive: Boolean,
    val isFocused: Boolean,
    val score: Int
)

data class ObservationWindowDiagnostics(
    val foregroundPackage: String = "None",
    val foregroundWindowId: Int = -1,
    val foregroundWindowType: Int = 0,
    val foregroundIsActive: Boolean = false,
    val foregroundIsFocused: Boolean = false,
    val availableWindows: List<WindowCandidateInfo> = emptyList(),
    val selectedPackage: String = "None",
    val selectedWindowId: Int = -1,
    val selectedWindowType: Int = 0,
    val lastExternalPackage: String = "None",
    val lastExternalWindowId: Int = -1,
    val lastExternalWindowType: Int = 0,
    val lastExternalTimestamp: Long = 0L
)

class AgentAccessibilityService : AccessibilityService() {

    @Volatile
    var activePackageName: String = ""
        private set

    @Volatile
    var activeActivityName: String? = null
        private set

    @Volatile
    var lastExternalPackageName: String = "None"
        private set

    @Volatile
    var lastExternalActivityName: String? = null
        private set

    @Volatile
    var lastExternalWindowId: Int = -1
        private set

    @Volatile
    var lastExternalWindowType: Int = 0
        private set

    @Volatile
    var lastExternalObservationTimestamp: Long = 0L
        private set

    @Volatile
    var currentObservationSnapshot: ObservationSnapshot? = null
        set

    @Volatile
    var lastExternalObservationSnapshot: ObservationSnapshot? = null
        set(value) {
            field = value
            if (value != null && isValidExternalApplicationPackage(value.packageName)) {
                lastExternalPackageName = value.packageName
            }
        }

    @Volatile
    var latestDiagnostics: ObservationWindowDiagnostics = ObservationWindowDiagnostics()
        private set

    @Volatile
    var externalObservationState: ObservationEngineState = ObservationEngineState.IDLE

    val currentEvidence: ObservationEvidence?
        get() {
            val snap = currentObservationSnapshot ?: return null
            val evidence = ObservationEvidence.fromSnapshot(
                snapshot = snap,
                source = EvidenceSource.CURRENT_UI,
                windowType = latestDiagnostics.selectedWindowType
            )
            logEvidenceEvent("EVIDENCE_GENERATED", evidence)
            return evidence
        }

    val lastExternalEvidence: ObservationEvidence?
        get() {
            val snap = lastExternalObservationSnapshot ?: return null
            val evidence = ObservationEvidence.fromSnapshot(
                snapshot = snap,
                source = EvidenceSource.EXTERNAL_APP,
                windowType = lastExternalWindowType
            )
            logEvidenceEvent("EVIDENCE_GENERATED", evidence)
            return evidence
        }

    private fun logEvidenceEvent(eventType: String, evidence: ObservationEvidence) {
        val app = application as? LocalAgentApplication ?: return
        val activeSessionId = app.eventLogger.getActiveSession().sessionId
        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = eventType,
                metadataJson = "{\"evidenceId\":\"${evidence.evidenceId}\",\"snapshotId\":\"${evidence.snapshotId}\",\"pkg\":\"${evidence.packageName}\",\"source\":\"${evidence.sourceChannel.name}\",\"provenanceHash\":\"${evidence.provenance.provenanceHash}\"}"
            )
        )
    }

    private val extractor = ObservationSnapshotExtractor(maxNodes = 500, maxDepth = 30)

    fun onServiceConnectedForTest() {
        onServiceConnected()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        INSTANCE = this

        val app = application as? LocalAgentApplication
        app?.accessibilityConnectionMonitor?.notifyServiceConnected()

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
            if (isValidExternalApplicationPackage(pkg)) {
                lastExternalPackageName = pkg
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.className?.toString()?.let { cls ->
                if (cls.isNotBlank() && cls.contains(".")) {
                    activeActivityName = cls
                    if (isValidExternalApplicationPackage(pkg)) {
                        lastExternalActivityName = cls
                    }
                }
            }
        }

        // Authoritative external observation session check
        if (externalObservationState != ObservationEngineState.OBSERVING) {
            return
        }

        // Evaluate external application windows on window state, window list, or window content changes
        val isRelevantEvent = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> true
            else -> false
        }

        if (isRelevantEvent && isValidExternalApplicationPackage(pkg)) {
            evaluateExternalApplicationWindows()
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
        app?.accessibilityConnectionMonitor?.notifyServiceDisconnected(reason = "ON_UNBIND")

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

    override fun onDestroy() {
        INSTANCE = null
        val app = application as? LocalAgentApplication
        app?.accessibilityConnectionMonitor?.notifyServiceDisconnected(reason = "ON_DESTROY")
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    open fun captureLiveSnapshot(): ObservationSnapshot {
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

        val windowCandidates = collectWindowCandidates()
        val recycledNodes = Collections.newSetFromMap(IdentityHashMap<AccessibilityNodeInfo, Boolean>())

        // Request-scoped deduplication set for external candidate rejection events
        val rejectedPackagesInRequest = mutableSetOf<String>()

        // Select best candidate overall for primary snapshot
        val bestOverall = selectBestWindow(windowCandidates)
        val selectedNode: AccessibilityNodeInfo?
        val selectedPkg: String
        val selectedWindowId: Int
        val selectedWindowType: Int

        if (bestOverall != null) {
            selectedNode = bestOverall.node
            selectedPkg = bestOverall.candidate.packageName.ifBlank { activePackageName }
            selectedWindowId = bestOverall.candidate.windowId
            selectedWindowType = bestOverall.candidate.windowType
        } else {
            selectedNode = rootInActiveWindow
            selectedPkg = selectedNode?.packageName?.toString() ?: activePackageName
            selectedWindowId = selectedNode?.windowId ?: -1
            selectedWindowType = 0
        }

        val primarySnapshot = if (selectedNode != null) {
            try {
                extractor.extractSnapshot(
                    rootNodeInfo = selectedNode,
                    packageName = selectedPkg,
                    activityName = activeActivityName,
                    windowId = selectedWindowId
                )
            } finally {
                selectedNode.recycle()
                recycledNodes.add(selectedNode)
            }
        } else {
            ObservationSnapshot(
                packageName = selectedPkg,
                activityName = activeActivityName,
                windowId = selectedWindowId,
                nodeCount = 0
            )
        }

        currentObservationSnapshot = primarySnapshot

        // If primary snapshot is a valid external app, preserve it as last external snapshot if observing
        if (isValidExternalApplicationPackage(selectedPkg)) {
            lastExternalPackageName = selectedPkg
            lastExternalWindowId = selectedWindowId
            lastExternalWindowType = selectedWindowType
            lastExternalObservationTimestamp = System.currentTimeMillis()
            if (externalObservationState == ObservationEngineState.OBSERVING) {
                lastExternalObservationSnapshot = primarySnapshot
            }
        } else if (externalObservationState == ObservationEngineState.OBSERVING) {
            // Primary is LocalAgent or System UI: inspect valid external window candidates
            val bestExternal = selectBestExternalWindow(
                candidates = windowCandidates,
                activeSessionId = activeSessionId,
                rejectedPackagesInRequest = rejectedPackagesInRequest
            )
            if (bestExternal != null && bestExternal.node != null && !recycledNodes.contains(bestExternal.node)) {
                try {
                    val extPkg = bestExternal.candidate.packageName
                    val extSnap = extractor.extractSnapshot(
                        rootNodeInfo = bestExternal.node,
                        packageName = extPkg,
                        activityName = lastExternalActivityName,
                        windowId = bestExternal.candidate.windowId
                    )
                    lastExternalPackageName = extPkg
                    lastExternalWindowId = bestExternal.candidate.windowId
                    lastExternalWindowType = bestExternal.candidate.windowType
                    lastExternalObservationTimestamp = System.currentTimeMillis()
                    lastExternalObservationSnapshot = extSnap
                } finally {
                    bestExternal.node.recycle()
                    recycledNodes.add(bestExternal.node)
                }
            }
        }

        // Clean up remaining unselected window root nodes safely
        windowCandidates.forEach { candidate ->
            candidate.node?.let { node ->
                if (!recycledNodes.contains(node)) {
                    node.recycle()
                    recycledNodes.add(node)
                }
            }
        }

        // Update diagnostics
        val fgCandidate = windowCandidates.firstOrNull { it.candidate.isActive && it.candidate.isFocused }
            ?: windowCandidates.firstOrNull { it.candidate.isActive }
            ?: windowCandidates.firstOrNull()

        latestDiagnostics = ObservationWindowDiagnostics(
            foregroundPackage = fgCandidate?.candidate?.packageName ?: activePackageName,
            foregroundWindowId = fgCandidate?.candidate?.windowId ?: -1,
            foregroundWindowType = fgCandidate?.candidate?.windowType ?: 0,
            foregroundIsActive = fgCandidate?.candidate?.isActive ?: false,
            foregroundIsFocused = fgCandidate?.candidate?.isFocused ?: false,
            availableWindows = windowCandidates.map { it.candidate },
            selectedPackage = selectedPkg,
            selectedWindowId = selectedWindowId,
            selectedWindowType = selectedWindowType,
            lastExternalPackage = lastExternalPackageName,
            lastExternalWindowId = lastExternalWindowId,
            lastExternalWindowType = lastExternalWindowType,
            lastExternalTimestamp = lastExternalObservationTimestamp
        )

        // Log window selection diagnostics
        val candidatesSummary = windowCandidates.joinToString(";") {
            "${it.candidate.windowId}:${it.candidate.packageName}:${it.candidate.windowTypeName}:${it.candidate.score}"
        }

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "OBSERVATION_WINDOW_SELECTED",
                metadataJson = "{\"selectedPkg\":\"$selectedPkg\",\"selectedWindowId\":$selectedWindowId,\"selectedWindowType\":$selectedWindowType,\"candidateCount\":${windowCandidates.size},\"candidates\":\"$candidatesSummary\"}"
            )
        )

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
                metadataJson = "{\"pkg\":\"$selectedPkg\",\"nodeCount\":${primarySnapshot.nodeCount},\"truncated\":${primarySnapshot.truncationInfo.isTruncated}}"
            )
        )

        return primarySnapshot
    }

    @Suppress("DEPRECATION")
    fun evaluateExternalApplicationWindows() {
        if (externalObservationState != ObservationEngineState.OBSERVING) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""
        val recycledNodes = Collections.newSetFromMap(IdentityHashMap<AccessibilityNodeInfo, Boolean>())
        val rejectedPackagesInRequest = mutableSetOf<String>()
        try {
            val windowCandidates = collectWindowCandidates()
            val bestExternal = selectBestExternalWindow(
                candidates = windowCandidates,
                activeSessionId = activeSessionId,
                rejectedPackagesInRequest = rejectedPackagesInRequest
            )

            if (bestExternal != null && bestExternal.node != null) {
                val extPkg = bestExternal.candidate.packageName
                if (isValidExternalApplicationPackage(extPkg) && bestExternal.candidate.score >= 50) {
                    try {
                        val extSnap = extractor.extractSnapshot(
                            rootNodeInfo = bestExternal.node,
                            packageName = extPkg,
                            activityName = lastExternalActivityName,
                            windowId = bestExternal.candidate.windowId
                        )
                        lastExternalPackageName = extPkg
                        lastExternalWindowId = bestExternal.candidate.windowId
                        lastExternalWindowType = bestExternal.candidate.windowType
                        lastExternalObservationTimestamp = System.currentTimeMillis()
                        lastExternalObservationSnapshot = extSnap
                    } finally {
                        bestExternal.node.recycle()
                        recycledNodes.add(bestExternal.node)
                    }
                }
            }

            windowCandidates.forEach { candidate ->
                candidate.node?.let { node ->
                    if (!recycledNodes.contains(node)) {
                        node.recycle()
                        recycledNodes.add(node)
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("AgentAccessibilityService: Error evaluating external windows: ${e.message}")
        }
    }

    fun getLiveExternalRootNode(targetPackageName: String? = null): AccessibilityNodeInfo? {
        val windowCandidates = collectWindowCandidates()

        // Match targetPackageName directly if provided
        if (!targetPackageName.isNullOrBlank() && isValidExternalApplicationPackage(targetPackageName)) {
            val directMatch = windowCandidates.firstOrNull { it.candidate.packageName == targetPackageName && it.node != null }
            if (directMatch?.node != null) {
                val matchedNode = AccessibilityNodeInfo.obtain(directMatch.node)
                windowCandidates.forEach { it.node?.recycle() }
                return matchedNode
            }
        }

        // Fallback 1: Check rootInActiveWindow if its package matches targetPackageName or is a valid external package
        val activeRoot = rootInActiveWindow
        if (activeRoot != null) {
            val activePkg = activeRoot.packageName?.toString() ?: ""
            if (!targetPackageName.isNullOrBlank() && activePkg == targetPackageName) {
                windowCandidates.forEach { it.node?.recycle() }
                return activeRoot
            }
            if (targetPackageName.isNullOrBlank() && isValidExternalApplicationPackage(activePkg)) {
                windowCandidates.forEach { it.node?.recycle() }
                return activeRoot
            }
            activeRoot.recycle()
        }

        // Fallback 2: Select best valid external window candidate
        val bestExternal = selectBestExternalWindow(windowCandidates, "", mutableSetOf())
        if (bestExternal?.node != null) {
            val matchedNode = AccessibilityNodeInfo.obtain(bestExternal.node)
            windowCandidates.forEach { it.node?.recycle() }
            return matchedNode
        }

        // Clean up candidate nodes
        windowCandidates.forEach { it.node?.recycle() }
        return null
    }

    fun getSnapshotForContext(isExternal: Boolean): ObservationSnapshot? {
        return if (isExternal) {
            lastExternalObservationSnapshot ?: currentObservationSnapshot?.takeIf { isValidExternalApplicationPackage(it.packageName) }
        } else {
            currentObservationSnapshot ?: captureLiveSnapshot()
        }
    }

    fun isValidExternalApplicationPackage(pkg: String): Boolean {
        if (pkg.isBlank()) return false
        if (pkg == "com.localagent.app") return false
        if (pkg == "com.android.systemui") return false
        if (pkg.endsWith(".launcher") || pkg.endsWith(".launcher3") || pkg == "com.google.android.apps.nexuslauncher" || pkg == "com.android.launcher3") return false
        return true
    }

    private fun getCandidateRejectionReason(pkg: String): String {
        return when {
            pkg.isBlank() -> "BLANK_PACKAGE"
            pkg == "com.localagent.app" -> "LOCAL_AGENT_NOT_EXTERNAL_APPLICATION"
            pkg == "com.android.systemui" -> "SYSTEM_UI_NOT_VALID_EXTERNAL_APPLICATION"
            pkg.contains("launcher") -> "LAUNCHER_NOT_VALID_EXTERNAL_APPLICATION"
            else -> "INVALID_EXTERNAL_APPLICATION"
        }
    }

    private data class InternalCandidate(
        val candidate: WindowCandidateInfo,
        val node: AccessibilityNodeInfo?
    )

    private fun collectWindowCandidates(): List<InternalCandidate> {
        val list = mutableListOf<InternalCandidate>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val interactiveWindows = windows
                if (interactiveWindows != null) {
                    for (window in interactiveWindows) {
                        val root = window.root
                        val pkg = root?.packageName?.toString() ?: ""
                        val type = window.type
                        val active = window.isActive
                        val focused = window.isFocused
                        val score = calculateWindowScore(type, pkg, active, focused)

                        list.add(
                            InternalCandidate(
                                candidate = WindowCandidateInfo(
                                    windowId = window.id,
                                    windowType = type,
                                    windowTypeName = getWindowTypeName(type),
                                    packageName = pkg,
                                    isActive = active,
                                    isFocused = focused,
                                    score = score
                                ),
                                node = if (root != null) AccessibilityNodeInfo.obtain(root) else null
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                System.err.println("AgentAccessibilityService: Error listing windows: ${e.message}")
            }
        }

        // Fallback if rootInActiveWindow is not already in list
        if (list.isEmpty()) {
            val root = rootInActiveWindow
            if (root != null) {
                val pkg = root.packageName?.toString() ?: activePackageName
                val score = calculateWindowScore(AccessibilityWindowInfo.TYPE_APPLICATION, pkg, isActive = true, isFocused = true)
                list.add(
                    InternalCandidate(
                        candidate = WindowCandidateInfo(
                            windowId = root.windowId,
                            windowType = AccessibilityWindowInfo.TYPE_APPLICATION,
                            windowTypeName = "TYPE_APPLICATION",
                            packageName = pkg,
                            isActive = true,
                            isFocused = true,
                            score = score
                        ),
                        node = AccessibilityNodeInfo.obtain(root)
                    )
                )
            }
        }

        return list.sortedByDescending { it.candidate.score }
    }

    private fun selectBestWindow(candidates: List<InternalCandidate>): InternalCandidate? {
        return candidates.maxByOrNull { it.candidate.score }
    }

    private fun selectBestExternalWindow(
        candidates: List<InternalCandidate>,
        activeSessionId: String,
        rejectedPackagesInRequest: MutableSet<String>
    ): InternalCandidate? {
        val validCandidates = mutableListOf<InternalCandidate>()
        val app = application as? LocalAgentApplication

        for (item in candidates) {
            val pkg = item.candidate.packageName
            if (isValidExternalApplicationPackage(pkg)) {
                validCandidates.add(item)
            } else if (pkg.isNotBlank() && pkg != "com.localagent.app") {
                val reason = getCandidateRejectionReason(pkg)
                if (rejectedPackagesInRequest.add(pkg)) {
                    app?.eventLogger?.logEvent(
                        AgentEvent(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = activeSessionId,
                            subsystem = EventSubsystem.OBSERVATION,
                            eventType = "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED",
                            metadataJson = "{\"pkg\":\"$pkg\",\"reason\":\"$reason\"}"
                        )
                    )
                }
            }
        }
        return validCandidates.maxByOrNull { it.candidate.score }
    }

    private fun calculateWindowScore(
        type: Int,
        packageName: String,
        isActive: Boolean,
        isFocused: Boolean
    ): Int {
        var score = 0

        // Prioritize TYPE_APPLICATION (1)
        if (type == AccessibilityWindowInfo.TYPE_APPLICATION) {
            score += 50
            if (isValidExternalApplicationPackage(packageName)) {
                score += 10 // Preferred valid non-system application
            }
        } else {
            score += 10
        }

        if (isActive && isFocused) {
            score += 40
        } else if (isActive) {
            score += 30
        } else if (isFocused) {
            score += 20
        }

        return score
    }

    private fun getWindowTypeName(type: Int): String {
        return when (type) {
            AccessibilityWindowInfo.TYPE_APPLICATION -> "TYPE_APPLICATION"
            AccessibilityWindowInfo.TYPE_INPUT_METHOD -> "TYPE_INPUT_METHOD"
            AccessibilityWindowInfo.TYPE_SYSTEM -> "TYPE_SYSTEM"
            AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "TYPE_ACCESSIBILITY_OVERLAY"
            AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "TYPE_SPLIT_SCREEN_DIVIDER"
            else -> "TYPE_UNKNOWN_$type"
        }
    }

    companion object {
        @Volatile
        var INSTANCE: AgentAccessibilityService? = null
            private set

        val isBound: Boolean
            get() = INSTANCE != null

        fun resetForTest(app: LocalAgentApplication? = null) {
            INSTANCE = null
            app?.accessibilityConnectionMonitor?.notifyServiceDisconnected("TEST_RESET")
        }
    }
}
