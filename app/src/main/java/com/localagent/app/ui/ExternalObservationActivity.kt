package com.localagent.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.accessibility.ObservationEngineState
import com.localagent.app.accessibility.ObservationWindowDiagnostics
import com.localagent.app.databinding.ActivityExternalObservationBinding
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import java.util.UUID

class ExternalObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExternalObservationBinding
    private var lastKnownA11yBound = false
    private var externalObservationState: ObservationEngineState = ObservationEngineState.IDLE
    private var isDisplayCleared: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExternalObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateObservationUi()
    }

    override fun onResume() {
        super.onResume()
        checkPassiveDegradationAndRefresh()
    }

    private fun setupListeners() {
        binding.btnCaptureExternalUi.setOnClickListener {
            isDisplayCleared = false
            externalObservationState = ObservationEngineState.OBSERVING
            triggerExternalObservation()
        }

        binding.btnStopExternalObservation.setOnClickListener {
            stopExternalObservation()
        }

        binding.btnClearExternalObservation.setOnClickListener {
            clearExternalObservation()
        }
    }

    private fun clearExternalObservation() {
        isDisplayCleared = true
        Toast.makeText(this, "External observation display cleared.", Toast.LENGTH_SHORT).show()
        updateObservationUi()
    }

    private fun stopExternalObservation() {
        externalObservationState = ObservationEngineState.STOPPED
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "OBSERVATION_STOPPED",
                sourceChannel = "EXTERNAL_OBSERVATION_SCREEN",
                severity = EventSeverity.INFO,
                metadataJson = "{\"mode\":\"EXTERNAL_APP\",\"reason\":\"USER_STOP_REQUESTED\"}"
            )
        )

        Toast.makeText(this, "External app observation stopped. Service remains active.", Toast.LENGTH_SHORT).show()
        updateObservationUi()
    }

    private fun checkPassiveDegradationAndRefresh() {
        val currentA11yBound = AgentAccessibilityService.isBound
        if (lastKnownA11yBound && !currentA11yBound) {
            val app = application as? LocalAgentApplication
            val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

            app?.eventLogger?.logEvent(
                AgentEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = activeSessionId,
                    subsystem = EventSubsystem.ACCESSIBILITY,
                    eventType = "PASSIVE_DEGRADATION_DETECTED",
                    sourceChannel = "EXTERNAL_OBSERVATION_SCREEN",
                    severity = EventSeverity.WARNING,
                    metadataJson = "{\"reason\":\"Accessibility Service unbound while screen active\"}"
                )
            )
        }
        lastKnownA11yBound = currentA11yBound
        updateObservationUi()
    }

    private fun triggerExternalObservation() {
        if (externalObservationState == ObservationEngineState.STOPPED) {
            Toast.makeText(this, "Observation is STOPPED. Tap Start External Observation to resume.", Toast.LENGTH_SHORT).show()
            updateObservationUi()
            return
        }

        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "OBSERVATION_REQUESTED",
                sourceChannel = "EXTERNAL_OBSERVATION_SCREEN"
            )
        )

        if (AgentAccessibilityService.isBound) {
            AgentAccessibilityService.INSTANCE?.captureLiveSnapshot()
        } else {
            app?.eventLogger?.logEvent(
                AgentEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = activeSessionId,
                    subsystem = EventSubsystem.ACCESSIBILITY,
                    eventType = "PASSIVE_DEGRADATION_DETECTED",
                    sourceChannel = "EXTERNAL_OBSERVATION_SCREEN",
                    severity = EventSeverity.WARNING,
                    metadataJson = "{\"reason\":\"External observation requested while Accessibility Service unbound\"}"
                )
            )
        }
        updateObservationUi()
    }

    private fun updateObservationUi() {
        val a11yBound = AgentAccessibilityService.isBound
        val activePkg = AgentAccessibilityService.INSTANCE?.activePackageName ?: "None"

        val statusText = if (a11yBound) {
            "Mode: EXTERNAL_APP | Engine State: $externalObservationState | Active Pkg: $activePkg"
        } else {
            "Engine State: UNAVAILABLE (Passive Degradation) — Open Permission Center from Home to Enable | Active Pkg: $activePkg"
        }
        binding.tvAccessibilityStatus.text = statusText

        val diag = AgentAccessibilityService.INSTANCE?.latestDiagnostics
        binding.tvDiagnosticsContent.text = formatDiagnosticsContent(diag)

        if (isDisplayCleared) {
            binding.tvExternalSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false"
            binding.tvExternalNodeTree.text = "No external observation captured."
            return
        }

        val externalSnap = AgentAccessibilityService.INSTANCE?.lastExternalObservationSnapshot

        if (externalSnap != null) {
            binding.tvExternalSnapshotMeta.text = "Package: ${externalSnap.packageName} | Nodes: ${externalSnap.nodeCount} | Depth: ${externalSnap.truncationInfo.maxDepthReached} | Truncated: ${externalSnap.truncationInfo.isTruncated}"
            binding.tvExternalNodeTree.text = renderSnapshotNodeTree(externalSnap)
        } else {
            binding.tvExternalSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false"
            binding.tvExternalNodeTree.text = if (a11yBound) "No external observation captured." else "Accessibility Service unbound. Open Permission Center from Home to enable service."
        }
    }

    private fun formatDiagnosticsContent(diag: ObservationWindowDiagnostics?): String {
        if (diag == null) {
            return "Foreground Package: None\nValidated Target: None\nLast Valid External Package: None"
        }

        val service = AgentAccessibilityService.INSTANCE
        val isValidTarget = service?.isValidExternalApplicationPackage(diag.selectedPackage) ?: false
        val targetStatus = if (isValidTarget) "VALID_EXTERNAL_APP (${diag.selectedPackage})" else "REJECTED (${diag.selectedPackage})"

        val sb = StringBuilder()
        sb.append("Foreground Package: ${diag.foregroundPackage}\n")
        sb.append("Foreground Window ID: ${diag.foregroundWindowId} | Type: ${diag.foregroundWindowType}\n")
        sb.append("Validated Target: $targetStatus\n\n")

        sb.append("Available Windows (${diag.availableWindows.size}):\n")
        if (diag.availableWindows.isEmpty()) {
            sb.append("- None listed\n")
        } else {
            diag.availableWindows.forEach { win ->
                val isValid = service?.isValidExternalApplicationPackage(win.packageName) ?: false
                val tag = if (isValid) "[VALID_APP]" else "[SYSTEM/LOCAL]"
                sb.append("- id:${win.windowId} $tag ${win.windowTypeName} pkg:${if (win.packageName.isBlank()) "Unknown" else win.packageName} score:${win.score}\n")
            }
        }

        sb.append("\nLast Valid External App: pkg:${diag.lastExternalPackage} id:${diag.lastExternalWindowId} type:${diag.lastExternalWindowType}")
        return sb.toString()
    }

    private fun renderSnapshotNodeTree(snapshot: ObservationSnapshot): String {
        val root = snapshot.rootNode ?: return "Empty root node"
        val sb = StringBuilder()
        sb.append("ROOT [${snapshot.packageName}]\n")
        renderNode(root, depth = 0, sb = sb, isLast = true, indent = "")
        return sb.toString()
    }

    private fun renderNode(
        node: ObservationNode,
        depth: Int,
        sb: StringBuilder,
        isLast: Boolean,
        indent: String
    ) {
        val branch = if (depth == 0) "" else if (isLast) "└── " else "├── "
        val classSimple = node.className.substringAfterLast('.')
        val textClean = node.text?.replace("\r", "")?.replace("\n", "\\n")?.take(80)
        val textStr = textClean?.let { " text:\"$it\"" } ?: ""
        val descClean = node.contentDescription?.replace("\r", "")?.replace("\n", "\\n")?.take(80)
        val descStr = descClean?.let { " desc:\"$it\"" } ?: ""
        val resIdStr = node.resourceId?.let { " id:${it.substringAfterLast('/')}" } ?: ""
        val identityStr = if (node.nodeIdentity.isNotBlank()) " [identity:${node.nodeIdentity} conf:${node.identityConfidence}]" else ""
        val boundsStr = " bounds:[${node.bounds.left},${node.bounds.top},${node.bounds.right},${node.bounds.bottom}] (${node.bounds.width}x${node.bounds.height})"
        val flagsStr = buildFlagsString(node)

        sb.append(indent).append(branch).append(classSimple).append(resIdStr).append(textStr).append(descStr).append(identityStr).append(boundsStr).append(flagsStr).append("\n")

        val childIndent = indent + if (depth == 0) "" else if (isLast) "    " else "│   "
        val children = node.children
        children.forEachIndexed { index, child ->
            renderNode(child, depth + 1, sb, isLast = (index == children.size - 1), indent = childIndent)
        }
    }

    private fun buildFlagsString(node: ObservationNode): String {
        val flags = mutableListOf<String>()
        if (node.clickable) flags.add("clickable")
        if (node.longClickable) flags.add("longClickable")
        if (node.scrollable) flags.add("scrollable")
        if (node.editable) flags.add("editable")
        if (node.focused) flags.add("focused")
        if (node.selected) flags.add("selected")
        if (node.checked) flags.add("checked")
        if (node.enabled) flags.add("enabled")
        if (node.visibleToUser) flags.add("visible")
        return if (flags.isNotEmpty()) " [${flags.joinToString(",")}]" else ""
    }
}
