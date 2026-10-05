package com.localagent.core.evidence

import com.localagent.core.observation.ObservationBounds
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import java.security.MessageDigest
import java.util.UUID

enum class EvidenceSource {
    CURRENT_UI,
    EXTERNAL_APP
}

data class EvidenceNodePrimitive(
    val nodeId: String,
    val className: String,
    val packageName: String,
    val resourceId: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val bounds: ObservationBounds = ObservationBounds(),
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEditable: Boolean = false,
    val isEnabled: Boolean = true,
    val isFocused: Boolean = false,
    val isVisibleToUser: Boolean = true,
    val parentInstanceId: String? = null,
    val depth: Int = 0
)

data class EvidenceProvenance(
    val snapshotId: String,
    val captureTimestamp: Long,
    val provenanceHash: String
)

data class ObservationEvidence(
    val evidenceId: String = UUID.randomUUID().toString(),
    val snapshotId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val activityName: String? = null,
    val windowId: Int = 0,
    val windowType: Int = 0,
    val sourceChannel: EvidenceSource = EvidenceSource.CURRENT_UI,
    val nodeCount: Int = 0,
    val maxDepth: Int = 0,
    val isTruncated: Boolean = false,
    val truncationReason: String = "NONE",
    val provenance: EvidenceProvenance,
    val primitives: List<EvidenceNodePrimitive> = emptyList()
) {

    fun toJsonString(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"evidenceId\":\"$evidenceId\",")
        sb.append("\"snapshotId\":\"$snapshotId\",")
        sb.append("\"timestamp\":$timestamp,")
        sb.append("\"packageName\":\"$packageName\",")
        sb.append("\"activityName\":${activityName?.let { "\"$it\"" } ?: "null"},")
        sb.append("\"windowId\":$windowId,")
        sb.append("\"windowType\":$windowType,")
        sb.append("\"sourceChannel\":\"${sourceChannel.name}\",")
        sb.append("\"nodeCount\":$nodeCount,")
        sb.append("\"maxDepth\":$maxDepth,")
        sb.append("\"isTruncated\":$isTruncated,")
        sb.append("\"truncationReason\":\"$truncationReason\",")
        sb.append("\"provenance\":{")
        sb.append("\"snapshotId\":\"${provenance.snapshotId}\",")
        sb.append("\"captureTimestamp\":${provenance.captureTimestamp},")
        sb.append("\"provenanceHash\":\"${provenance.provenanceHash}\"")
        sb.append("},")
        sb.append("\"primitives\":[")
        primitives.forEachIndexed { index, p ->
            sb.append("{")
            sb.append("\"nodeId\":\"${p.nodeId}\",")
            sb.append("\"className\":\"${p.className}\",")
            sb.append("\"packageName\":\"${p.packageName}\",")
            sb.append("\"resourceId\":${p.resourceId?.let { "\"$it\"" } ?: "null"},")
            sb.append("\"text\":${p.text?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            sb.append("\"contentDescription\":${p.contentDescription?.let { "\"${escapeJson(it)}\"" } ?: "null"},")
            sb.append("\"bounds\":{\"left\":${p.bounds.left},\"top\":${p.bounds.top},\"right\":${p.bounds.right},\"bottom\":${p.bounds.bottom}},")
            sb.append("\"isClickable\":${p.isClickable},")
            sb.append("\"isLongClickable\":${p.isLongClickable},")
            sb.append("\"isScrollable\":${p.isScrollable},")
            sb.append("\"isEditable\":${p.isEditable},")
            sb.append("\"isEnabled\":${p.isEnabled},")
            sb.append("\"isFocused\":${p.isFocused},")
            sb.append("\"isVisibleToUser\":${p.isVisibleToUser},")
            sb.append("\"parentInstanceId\":${p.parentInstanceId?.let { "\"$it\"" } ?: "null"},")
            sb.append("\"depth\":${p.depth}")
            sb.append("}")
            if (index < primitives.size - 1) sb.append(",")
        }
        sb.append("]")
        sb.append("}")
        return sb.toString()
    }

    companion object {
        private fun escapeJson(input: String): String {
            return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        fun fromSnapshot(
            snapshot: ObservationSnapshot,
            source: EvidenceSource,
            windowType: Int = 0,
            evidenceId: String = UUID.randomUUID().toString()
        ): ObservationEvidence {
            val primitivesList = mutableListOf<EvidenceNodePrimitive>()

            fun collectPrimitives(node: ObservationNode, depth: Int, parentId: String?) {
                primitivesList.add(
                    EvidenceNodePrimitive(
                        nodeId = node.nodeId,
                        className = node.className,
                        packageName = node.packageName.ifBlank { snapshot.packageName },
                        resourceId = node.resourceId,
                        text = node.text,
                        contentDescription = node.contentDescription,
                        bounds = node.bounds,
                        isClickable = node.clickable,
                        isLongClickable = node.longClickable,
                        isScrollable = node.scrollable,
                        isEditable = node.editable,
                        isEnabled = node.enabled,
                        isFocused = node.focused,
                        isVisibleToUser = node.visibleToUser,
                        parentInstanceId = parentId,
                        depth = depth
                    )
                )

                node.children.forEach { child ->
                    collectPrimitives(child, depth + 1, node.nodeId)
                }
            }

            snapshot.rootNode?.let { root ->
                collectPrimitives(root, depth = 0, parentId = null)
            }

            val provenanceHash = computeProvenanceHash(snapshot.packageName, snapshot.snapshotId, primitivesList)

            return ObservationEvidence(
                evidenceId = evidenceId,
                snapshotId = snapshot.snapshotId,
                timestamp = snapshot.timestamp,
                packageName = snapshot.packageName,
                activityName = snapshot.activityName,
                windowId = snapshot.windowId,
                windowType = windowType,
                sourceChannel = source,
                nodeCount = snapshot.nodeCount,
                maxDepth = snapshot.truncationInfo.maxDepthReached,
                isTruncated = snapshot.truncationInfo.isTruncated,
                truncationReason = snapshot.truncationInfo.reason,
                provenance = EvidenceProvenance(
                    snapshotId = snapshot.snapshotId,
                    captureTimestamp = snapshot.timestamp,
                    provenanceHash = provenanceHash
                ),
                primitives = primitivesList
            )
        }

        fun computeProvenanceHash(
            packageName: String,
            snapshotId: String,
            primitives: List<EvidenceNodePrimitive>
        ): String {
            val sb = StringBuilder()
            sb.append(packageName).append(":").append(snapshotId).append(":")
            primitives.forEach { p ->
                sb.append(p.nodeId).append(";")
                    .append(p.className).append(";")
                    .append(p.resourceId ?: "").append(";")
                    .append(p.text ?: "").append(";")
                    .append(p.bounds.left).append(",").append(p.bounds.top).append(",")
                    .append(p.bounds.right).append(",").append(p.bounds.bottom).append("|")
            }

            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sb.toString().toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
