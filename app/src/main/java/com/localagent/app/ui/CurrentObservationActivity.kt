package com.localagent.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.accessibility.ObservationEngineState
import com.localagent.app.databinding.ActivityCurrentObservationBinding
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import java.util.UUID

class CurrentObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrentObservationBinding
    private var lastKnownA11yBound = false
    private var currentObservationState: ObservationEngineState = ObservationEngineState.IDLE
    private var isDisplayCleared: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCurrentObservationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateObservationUi()
    }

    override fun onResume() {
        super.onResume()
        checkPassiveDegradationAndRefresh()
    }

    private fun setupListeners() {
        binding.btnObserveCurrentUi.setOnClickListener {
            isDisplayCleared = false
            currentObservationState = ObservationEngineState.OBSERVING
            triggerLiveObservation()
        }

        binding.btnStopCurrentObservation.setOnClickListener {
            stopCurrentObservation()
        }

        binding.btnClearCurrentObservation.setOnClickListener {
            clearCurrentObservation()
        }
    }

    private fun clearCurrentObservation() {
        isDisplayCleared = true
        Toast.makeText(this, "Current observation display cleared.", Toast.LENGTH_SHORT).show()
        updateObservationUi()
    }

    private fun stopCurrentObservation() {
        currentObservationState = ObservationEngineState.STOPPED
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "OBSERVATION_STOPPED",
                sourceChannel = "CURRENT_OBSERVATION_SCREEN",
                severity = EventSeverity.INFO,
                metadataJson = "{\"mode\":\"CURRENT_UI\",\"reason\":\"USER_STOP_REQUESTED\"}"
            )
        )

        Toast.makeText(this, "Current observation stopped. Service remains active.", Toast.LENGTH_SHORT).show()
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
                    sourceChannel = "CURRENT_OBSERVATION_SCREEN",
                    severity = EventSeverity.WARNING,
                    metadataJson = "{\"reason\":\"Accessibility Service unbound while screen active\"}"
                )
            )
        }
        lastKnownA11yBound = currentA11yBound
        updateObservationUi()
    }

    private fun triggerLiveObservation() {
        if (currentObservationState == ObservationEngineState.STOPPED) {
            Toast.makeText(this, "Observation is STOPPED. Tap Start Current Observation to resume.", Toast.LENGTH_SHORT).show()
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
                sourceChannel = "CURRENT_OBSERVATION_SCREEN"
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
                    sourceChannel = "CURRENT_OBSERVATION_SCREEN",
                    severity = EventSeverity.WARNING,
                    metadataJson = "{\"reason\":\"Observation requested while Accessibility Service unbound\"}"
                )
            )
        }
        updateObservationUi()
    }

    private fun updateObservationUi() {
        val a11yBound = AgentAccessibilityService.isBound
        val activePkg = AgentAccessibilityService.INSTANCE?.activePackageName ?: "None"

        val statusText = if (a11yBound) {
            "Mode: CURRENT_UI | Engine State: $currentObservationState | Active Pkg: $activePkg"
        } else {
            "Engine State: UNAVAILABLE (Passive Degradation) — Open Permission Center from Home to Enable | Active Pkg: $activePkg"
        }
        binding.tvAccessibilityStatus.text = statusText

        if (isDisplayCleared) {
            binding.tvSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false"
            binding.tvCurrentNodeTree.text = "No current observation captured."
            return
        }

        val currentSnap = AgentAccessibilityService.INSTANCE?.currentObservationSnapshot

        if (currentSnap != null) {
            binding.tvSnapshotMeta.text = "Package: ${currentSnap.packageName} | Nodes: ${currentSnap.nodeCount} | Depth: ${currentSnap.truncationInfo.maxDepthReached} | Truncated: ${currentSnap.truncationInfo.isTruncated}"
            binding.tvCurrentNodeTree.text = NodeTreeRenderer.renderSnapshotTree(currentSnap)
        } else {
            binding.tvSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false"
            binding.tvCurrentNodeTree.text = if (a11yBound) "No current observation captured." else "Accessibility Service unbound. Open Permission Center from Home to enable service."
        }
    }
}
