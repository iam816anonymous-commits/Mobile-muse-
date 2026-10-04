package com.localagent.app.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.databinding.ActivityObservationBinding
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import java.util.UUID

class ObservationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObservationBinding

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

        binding.btnObserveCurrentUi.setOnClickListener {
            triggerLiveObservation()
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
                sourceChannel = "OBSERVATION_SCREEN"
            )
        )

        if (AgentAccessibilityService.isBound) {
            AgentAccessibilityService.INSTANCE?.captureLiveSnapshot()
        }
        updateObservationUi()
    }

    private fun updateObservationUi() {
        val a11yBound = AgentAccessibilityService.isBound
        val activePkg = AgentAccessibilityService.INSTANCE?.activePackageName ?: "None"

        binding.tvAccessibilityStatus.text = "Status: ${if (a11yBound) "READY (BOUND)" else "SERVICE_UNBOUND"} | Active Pkg: $activePkg"

        val currentSnap = AgentAccessibilityService.INSTANCE?.currentObservationSnapshot
        val externalSnap = AgentAccessibilityService.INSTANCE?.lastExternalObservationSnapshot

        if (currentSnap != null) {
            binding.tvCurrentSnapshotMeta.text = "Pkg: ${currentSnap.packageName} | Nodes: ${currentSnap.nodeCount} | Depth: ${currentSnap.truncationInfo.maxDepthReached} | Truncated: ${currentSnap.truncationInfo.isTruncated}"
            binding.tvCurrentNodeTree.text = renderSnapshotNodeTree(currentSnap)
        } else {
            binding.tvCurrentSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false"
            binding.tvCurrentNodeTree.text = "No current tree captured."
        }

        if (externalSnap != null) {
            binding.tvExternalSnapshotMeta.text = "Pkg: ${externalSnap.packageName} | Nodes: ${externalSnap.nodeCount} | Depth: ${externalSnap.truncationInfo.maxDepthReached} | Truncated: ${externalSnap.truncationInfo.isTruncated}"
            binding.tvExternalNodeTree.text = renderSnapshotNodeTree(externalSnap)
        } else {
            binding.tvExternalSnapshotMeta.text = "Package: None | Nodes: 0 | Depth: 0 | Truncated: false"
            binding.tvExternalNodeTree.text = "No external tree captured."
        }
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
        val textStr = node.text?.let { " \"$it\"" } ?: ""
        val descStr = node.contentDescription?.let { " desc:\"$it\"" } ?: ""
        val resIdStr = node.resourceId?.let { " id:${it.substringAfterLast('/')}" } ?: ""
        val flagsStr = buildFlagsString(node)

        sb.append(indent).append(branch).append(classSimple).append(resIdStr).append(textStr).append(descStr).append(flagsStr).append("\n")

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
        return if (flags.isNotEmpty()) " [${flags.joinToString(",")}]" else ""
    }
}
