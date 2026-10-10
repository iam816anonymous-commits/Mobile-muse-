package com.localagent.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.accessibility.ObservationEngineState
import com.localagent.app.accessibility.ObservationWindowDiagnostics
import com.localagent.app.databinding.ActivityObservationBinding
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObservationBinding
    private var isDisplayCleared: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateObservationUi()
    }

    override fun onResume() {
        super.onResume()
        updateObservationUi()
    }

    private fun setupListeners() {
        binding.btnOpenAccessibilitySettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                System.err.println("ObservationActivity: Error opening Settings: ${e.message}")
            }
        }

        binding.btnObserveUi.setOnClickListener {
            isDisplayCleared = false
            triggerLiveObservation()
        }

        binding.btnClearObservation.setOnClickListener {
            isDisplayCleared = true
            Toast.makeText(this, "Observation display cleared.", Toast.LENGTH_SHORT).show()
            updateObservationUi()
        }
    }

    private fun triggerLiveObservation() {
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "OBSERVATION_REQUESTED",
                sourceChannel = "UNIFIED_OBSERVATION_SCREEN"
            )
        )

        if (AgentAccessibilityService.isBound) {
            AgentAccessibilityService.INSTANCE?.captureLiveSnapshot()
        }
        updateObservationUi()
    }

    private fun updateObservationUi() {
        val a11yBound = AgentAccessibilityService.isBound
        val service = AgentAccessibilityService.INSTANCE
        val activePkg = service?.activePackageName ?: "None"
        val lastExtPkg = service?.lastExternalPackageName ?: "None"

        val targetPkg = if (service?.isValidExternalApplicationPackage(lastExtPkg) == true) lastExtPkg else activePkg
        val isForegroundMatch = activePkg == targetPkg

        binding.tvAccessibilityStatus.text = "Status: ${if (a11yBound) "READY (BOUND)" else "SERVICE_UNBOUND"} | Target: $targetPkg | FgMatch: $isForegroundMatch"

        val diag = service?.latestDiagnostics
        binding.tvDiagnosticsContent.text = formatDiagnosticsContent(diag)

        if (isDisplayCleared) {
            binding.tvSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false | Timestamp: None"
            binding.tvNodeTree.text = "Observation display cleared."
            return
        }

        // Authoritative external observation snapshot
        val snap = service?.getSnapshotForContext(isExternal = true) ?: service?.currentObservationSnapshot

        if (snap != null) {
            val dateStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(snap.timestamp))
            binding.tvSnapshotMeta.text = "Gen: ${snap.generation} | Package: ${snap.packageName} | Nodes: ${snap.nodeCount} | Depth: ${snap.truncationInfo.maxDepthReached} | Truncated: ${snap.truncationInfo.isTruncated} | Time: $dateStr"
            binding.tvNodeTree.text = NodeTreeRenderer.renderSnapshotTree(snap)
        } else {
            binding.tvSnapshotMeta.text = "Gen: 0 | Package: None | Nodes: 0 | Depth: 0 | Truncated: false | Timestamp: None"
            binding.tvNodeTree.text = if (a11yBound) "No observation captured yet. Tap Refresh Observation." else "Accessibility Service unbound. Open A11y Settings to enable service."
        }
    }

    private fun formatDiagnosticsContent(diag: ObservationWindowDiagnostics?): String {
        if (diag == null) {
            return "Foreground Package: None\nForeground Window ID: -1\nAvailable Windows: None\nSelected Target: None"
        }

        val service = AgentAccessibilityService.INSTANCE
        val isValidTarget = service?.isValidExternalApplicationPackage(diag.lastExternalPackage) ?: false
        val targetStatus = if (isValidTarget) "VALID_EXTERNAL_APP (${diag.lastExternalPackage})" else "NONE (${diag.lastExternalPackage})"

        val sb = StringBuilder()
        sb.append("Foreground Package: ${diag.foregroundPackage}\n")
        sb.append("Foreground Window ID: ${diag.foregroundWindowId} | Type: ${diag.foregroundWindowType}\n")
        sb.append("Target Application: $targetStatus\n\n")

        sb.append("Available Windows (${diag.availableWindows.size}):\n")
        if (diag.availableWindows.isEmpty()) {
            sb.append("- None listed\n")
        } else {
            diag.availableWindows.forEach { win ->
                val isValid = service?.isValidExternalApplicationPackage(win.packageName) ?: false
                val tag = if (isValid) "[TARGET_APP]" else "[SYSTEM/LOCAL]"
                sb.append("- id:${win.windowId} $tag ${win.windowTypeName} pkg:${if (win.packageName.isBlank()) "Unknown" else win.packageName} score:${win.score}\n")
            }
        }

        sb.append("\nLast External App: pkg:${diag.lastExternalPackage} id:${diag.lastExternalWindowId} type:${diag.lastExternalWindowType}")
        return sb.toString()
    }
}
