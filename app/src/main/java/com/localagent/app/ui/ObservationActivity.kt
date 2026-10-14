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

enum class ObservationViewMode {
    LIVE_SCREEN,
    LAST_EXTERNAL,
    ACTION_PAIR
}

class ObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObservationBinding
    private var isDisplayCleared: Boolean = false
    private var selectedViewMode: ObservationViewMode = ObservationViewMode.LIVE_SCREEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateObservationUi()
    }

    private var lastObservedGen: Long = -1L
    private val uiUpdateHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            val service = AgentAccessibilityService.INSTANCE
            if (service != null) {
                val currentGen = service.snapshotGenerationCounter
                if (currentGen != lastObservedGen) {
                    lastObservedGen = currentGen
                    updateObservationUi()
                }
            }
            uiUpdateHandler.postDelayed(this, 500L)
        }
    }

    override fun onResume() {
        super.onResume()
        updateObservationUi()
        uiUpdateHandler.post(pollRunnable)
    }

    override fun onPause() {
        super.onPause()
        uiUpdateHandler.removeCallbacks(pollRunnable)
    }

    private fun setupListeners() {
        binding.btnViewLiveScreen.setOnClickListener {
            selectedViewMode = ObservationViewMode.LIVE_SCREEN
            isDisplayCleared = false
            updateObservationUi()
        }

        binding.btnViewActionPair.setOnClickListener {
            selectedViewMode = ObservationViewMode.ACTION_PAIR
            isDisplayCleared = false
            updateObservationUi()
        }

        binding.btnViewExternalScreen.setOnClickListener {
            selectedViewMode = ObservationViewMode.LAST_EXTERNAL
            isDisplayCleared = false
            updateObservationUi()
        }

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

        binding.tvAccessibilityStatus.text = "Status: ${if (a11yBound) "BOUND" else "UNBOUND"} | Target App: $targetPkg | FgMatch: $isForegroundMatch [Foreground vs Target App Match]"

        val diag = service?.latestDiagnostics
        binding.tvDiagnosticsContent.text = formatDiagnosticsContent(diag)

        if (isDisplayCleared) {
            binding.tvSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false | Timestamp: None"
            binding.tvNodeTree.text = "Observation display cleared."
            return
        }

        if (selectedViewMode == ObservationViewMode.ACTION_PAIR) {
            binding.scrollSingleTree.visibility = android.view.View.GONE
            binding.layoutActionPairContainer.visibility = android.view.View.VISIBLE
            binding.tvDiagnosticsHeader.visibility = android.view.View.GONE
            binding.tvDiagnosticsContent.visibility = android.view.View.GONE

            binding.tvSnapshotHeader.text = "VIEW C: ACTION COMPARISON PAIR (PRE vs POST):"
            val actionRes = service?.lastActionExecutionResult
            if (actionRes != null) {
                val preSnap = actionRes.preSnapshot
                val postSnap = actionRes.postSnapshot
                val verRes = actionRes.verificationResult
                val preGen = preSnap?.generation ?: 0L
                val postGen = postSnap?.generation ?: 0L
                binding.tvSnapshotMeta.text = "Exec ID: ${actionRes.executionId.take(8)} | Pre Gen: $preGen (${preSnap?.packageName ?: "None"}) | Post Gen: $postGen (${postSnap?.packageName ?: "None"}) | Result: ${verRes.resultCode}"

                binding.tvPreHeader.text = "PANEL A — PRE-ACTION TREE (Gen $preGen | ${preSnap?.packageName ?: "N/A"}):"
                binding.tvPreNodeTree.text = if (preSnap != null) {
                    NodeTreeRenderer.renderSnapshotTree(preSnap)
                } else {
                    "[PRE-ACTION TREE NOT REQUIRED OR UNAVAILABLE FOR THIS ACTION POLICY]"
                }

                binding.tvPostHeader.text = "PANEL B — POST-ACTION TREE (Gen $postGen | ${postSnap?.packageName ?: "N/A"}):"
                binding.tvPostNodeTree.text = if (postSnap != null) {
                    NodeTreeRenderer.renderSnapshotTree(postSnap)
                } else {
                    "[POST-ACTION SNAPSHOT UNAVAILABLE / UNVERIFIED]"
                }
            } else {
                binding.tvSnapshotMeta.text = "Exec ID: None | Status: NO_ACTIONS_EXECUTED"
                binding.tvPreNodeTree.text = "No UI actions executed yet."
                binding.tvPostNodeTree.text = "Execute an action from the Command Console to view pre/post comparison pair."
            }
            return
        }

        // View A or View B: Single Tree layout
        binding.scrollSingleTree.visibility = android.view.View.VISIBLE
        binding.layoutActionPairContainer.visibility = android.view.View.GONE
        binding.tvDiagnosticsHeader.visibility = android.view.View.VISIBLE
        binding.tvDiagnosticsContent.visibility = android.view.View.VISIBLE

        val snap = if (selectedViewMode == ObservationViewMode.LIVE_SCREEN) {
            service?.currentObservationSnapshot ?: service?.captureLiveSnapshot()
        } else {
            service?.lastExternalObservationSnapshot
        }

        val modeTitle = if (selectedViewMode == ObservationViewMode.LIVE_SCREEN) "VIEW A: LIVE SCREEN NODES" else "VIEW B: LAST EXTERNAL SCREEN"
        binding.tvSnapshotHeader.text = "$modeTitle:"

        if (snap != null) {
            val dateStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(snap.timestamp))
            binding.tvSnapshotMeta.text = "Gen: ${snap.generation} | Package: ${snap.packageName} | Nodes: ${snap.nodeCount} | Depth: ${snap.truncationInfo.maxDepthReached} | Truncated: ${snap.truncationInfo.isTruncated} | Time: $dateStr"
            binding.tvNodeTree.text = NodeTreeRenderer.renderSnapshotTree(snap)
        } else {
            binding.tvSnapshotMeta.text = "Gen: 0 | Package: None | Nodes: 0 | Depth: 0 | Truncated: false | Timestamp: None"
            if (selectedViewMode == ObservationViewMode.LAST_EXTERNAL) {
                binding.tvNodeTree.text = "No external application observation captured yet. Launch an external app (e.g. Calculator) to capture."
            } else {
                binding.tvNodeTree.text = if (a11yBound) "No live observation captured yet. Tap Refresh Capture." else "Accessibility Service unbound. Open A11y Settings to enable service."
            }
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
