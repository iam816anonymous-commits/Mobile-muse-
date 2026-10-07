package com.localagent.app.ui

import com.localagent.core.evidence.EvidenceNodePrimitive
import com.localagent.core.evidence.ObservationEvidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot

object NodeTreeRenderer {

    fun renderSnapshotTree(snapshot: ObservationSnapshot): String {
        val root = snapshot.rootNode ?: return "Empty root node"
        val sb = StringBuilder()
        sb.append("ROOT [Package: ${snapshot.packageName}]\n")
        sb.append("========================================\n")
        renderNodeCard(root, depth = 0, sb = sb, indent = "")
        return sb.toString()
    }

    private fun renderNodeCard(
        node: ObservationNode,
        depth: Int,
        sb: StringBuilder,
        indent: String
    ) {
        val indentPrefix = "  ".repeat(depth)
        val connector = if (depth == 0) "ROOT" else "└─ "

        sb.append(indentPrefix).append(connector).append("[").append(node.nodeId).append("] ")
            .append(node.className.substringAfterLast('.')).append("\n")

        val detailIndent = indentPrefix + if (depth == 0) "  " else "   "

        node.text?.takeIf { it.isNotBlank() }?.let {
            val textClean = it.replace("\r", "").replace("\n", "\\n")
            sb.append(detailIndent).append("Text: \"").append(textClean).append("\"\n")
        }

        node.contentDescription?.takeIf { it.isNotBlank() }?.let {
            val descClean = it.replace("\r", "").replace("\n", "\\n")
            sb.append(detailIndent).append("Desc: \"").append(descClean).append("\"\n")
        }

        node.resourceId?.takeIf { it.isNotBlank() }?.let {
            sb.append(detailIndent).append("ResID: ").append(it).append("\n")
        }

        if (node.nodeIdentity.isNotBlank()) {
            sb.append(detailIndent).append("Identity: ").append(node.nodeIdentity)
                .append(" (conf: ").append(node.identityConfidence).append(")\n")
        }

        sb.append(detailIndent).append("Bounds: [")
            .append(node.bounds.left).append(", ").append(node.bounds.top).append(", ")
            .append(node.bounds.right).append(", ").append(node.bounds.bottom)
            .append("] (").append(node.bounds.width).append("x").append(node.bounds.height).append(")\n")

        val flags = buildFlagsString(node)
        if (flags.isNotBlank()) {
            sb.append(detailIndent).append("Flags: ").append(flags).append("\n")
        }

        sb.append(detailIndent).append("----------------------------------------\n")

        for (child in node.children) {
            renderNodeCard(child, depth + 1, sb, indent = detailIndent)
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
        return flags.joinToString(", ")
    }

    fun renderEvidenceTree(evidence: ObservationEvidence): String {
        if (evidence.primitives.isEmpty()) return "No evidence primitives captured."
        val sb = StringBuilder()
        sb.append("EVIDENCE PRIMITIVES [Package: ${evidence.packageName}]\n")
        sb.append("========================================\n")
        evidence.primitives.forEach { p ->
            val indentPrefix = "  ".repeat(p.depth)
            sb.append(indentPrefix).append("└─ [").append(p.nodeId).append("] ")
                .append(p.className.substringAfterLast('.')).append("\n")

            val detailIndent = indentPrefix + "   "
            p.text?.takeIf { it.isNotBlank() }?.let {
                val textClean = it.replace("\r", "").replace("\n", "\\n")
                sb.append(detailIndent).append("Text: \"").append(textClean).append("\"\n")
            }
            p.contentDescription?.takeIf { it.isNotBlank() }?.let {
                val descClean = it.replace("\r", "").replace("\n", "\\n")
                sb.append(detailIndent).append("Desc: \"").append(descClean).append("\"\n")
            }
            p.resourceId?.takeIf { it.isNotBlank() }?.let {
                sb.append(detailIndent).append("ResID: ").append(it).append("\n")
            }
            sb.append(detailIndent).append("Bounds: [")
                .append(p.bounds.left).append(", ").append(p.bounds.top).append(", ")
                .append(p.bounds.right).append(", ").append(p.bounds.bottom)
                .append("] (").append(p.bounds.width).append("x").append(p.bounds.height).append(")\n")

            val flags = buildPrimitiveFlagsString(p)
            if (flags.isNotBlank()) {
                sb.append(detailIndent).append("Flags: ").append(flags).append("\n")
            }
            sb.append(detailIndent).append("----------------------------------------\n")
        }
        return sb.toString()
    }

    private fun buildPrimitiveFlagsString(p: EvidenceNodePrimitive): String {
        val flags = mutableListOf<String>()
        if (p.isClickable) flags.add("clickable")
        if (p.isLongClickable) flags.add("longClickable")
        if (p.isScrollable) flags.add("scrollable")
        if (p.isEditable) flags.add("editable")
        if (p.isFocused) flags.add("focused")
        if (p.isEnabled) flags.add("enabled")
        if (p.isVisibleToUser) flags.add("visible")
        return flags.joinToString(", ")
    }
}
