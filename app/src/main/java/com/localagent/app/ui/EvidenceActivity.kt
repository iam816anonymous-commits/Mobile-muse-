package com.localagent.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.databinding.ActivityEvidenceBinding
import com.localagent.core.evidence.EvidenceNodePrimitive
import com.localagent.core.evidence.ObservationEvidence
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSubsystem
import java.util.UUID

class EvidenceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEvidenceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEvidenceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateEvidenceUi()
    }

    override fun onResume() {
        super.onResume()
        updateEvidenceUi()
    }

    private fun setupListeners() {
        binding.btnOpenAccessibilitySettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                System.err.println("EvidenceActivity: Error opening Settings: ${e.message}")
            }
        }

        binding.btnCaptureEvidence.setOnClickListener {
            triggerLiveEvidenceCapture()
        }
    }

    private fun triggerLiveEvidenceCapture() {
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""

        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.OBSERVATION,
                eventType = "EVIDENCE_REQUESTED",
                sourceChannel = "EVIDENCE_SCREEN"
            )
        )

        if (AgentAccessibilityService.isBound) {
            AgentAccessibilityService.INSTANCE?.captureLiveSnapshot()
        }
        updateEvidenceUi()
    }

    private fun updateEvidenceUi() {
        val a11yBound = AgentAccessibilityService.isBound
        val activePkg = AgentAccessibilityService.INSTANCE?.activePackageName ?: "None"

        binding.tvAccessibilityStatus.text = "Status: ${if (a11yBound) "READY (BOUND)" else "SERVICE_UNBOUND"} | Active Pkg: $activePkg"

        val currentEv = AgentAccessibilityService.INSTANCE?.currentEvidence
        val externalEv = AgentAccessibilityService.INSTANCE?.lastExternalEvidence

        if (currentEv != null) {
            binding.tvCurrentEvidenceMeta.text = formatMetaText(currentEv)
            binding.tvCurrentEvidencePrimitives.text = renderEvidencePrimitives(currentEv)
        } else {
            binding.tvCurrentEvidenceMeta.text = "Evidence ID: None\nSnapshot ID: None\nProvenance Hash: None\nPackage: None | Primitives: 0"
            binding.tvCurrentEvidencePrimitives.text = "No current evidence generated."
        }

        if (externalEv != null) {
            binding.tvExternalEvidenceMeta.text = formatMetaText(externalEv)
            binding.tvExternalEvidencePrimitives.text = renderEvidencePrimitives(externalEv)
        } else {
            binding.tvExternalEvidenceMeta.text = "Evidence ID: None\nSnapshot ID: None\nProvenance Hash: None\nPackage: None | Primitives: 0"
            binding.tvExternalEvidencePrimitives.text = "No external evidence generated."
        }
    }

    private fun formatMetaText(evidence: ObservationEvidence): String {
        val prov = evidence.provenance
        return "Evidence ID: ${evidence.evidenceId.take(18)}...\nSnapshot ID: ${evidence.snapshotId.take(18)}...\nProvenance Hash: ${prov.provenanceHash.take(24)}...\nPackage: ${evidence.packageName} | Nodes: ${evidence.nodeCount} | Depth: ${evidence.maxDepth} | Truncated: ${evidence.isTruncated}"
    }

    private fun renderEvidencePrimitives(evidence: ObservationEvidence): String {
        if (evidence.primitives.isEmpty()) return "No evidence primitives captured."
        val sb = StringBuilder()
        sb.append("EVIDENCE PRIMITIVES [${evidence.packageName}]\n")
        evidence.primitives.forEach { p ->
            val indent = "  ".repeat(p.depth)
            val resIdStr = p.resourceId?.let { " id:${it.substringAfterLast('/')}" } ?: ""
            val textStr = p.text?.let { " text:\"$it\"" } ?: ""
            val descStr = p.contentDescription?.let { " desc:\"$it\"" } ?: ""
            val flags = buildFlagsString(p)
            val boundsStr = " [${p.bounds.left},${p.bounds.top},${p.bounds.right},${p.bounds.bottom}]"

            sb.append(indent).append("- ").append(p.nodeId).append(" ").append(p.className.substringAfterLast('.')).append(resIdStr).append(textStr).append(descStr).append(boundsStr).append(flags).append("\n")
        }
        return sb.toString()
    }

    private fun buildFlagsString(p: EvidenceNodePrimitive): String {
        val flags = mutableListOf<String>()
        if (p.isClickable) flags.add("clickable")
        if (p.isLongClickable) flags.add("longClickable")
        if (p.isScrollable) flags.add("scrollable")
        if (p.isEditable) flags.add("editable")
        if (p.isFocused) flags.add("focused")
        if (p.isEnabled) flags.add("enabled")
        if (p.isVisibleToUser) flags.add("visible")
        return if (flags.isNotEmpty()) " (${flags.joinToString(",")})" else ""
    }
}
