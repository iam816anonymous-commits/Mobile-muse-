package com.localagent.core.observation

import org.json.JSONArray
import org.json.JSONObject

enum class NodeDiffType {
    ADDED,
    REMOVED,
    CHANGED,
    UNCHANGED
}

data class NodeDiffEntry(
    val nodeIdentity: String,
    val diffType: NodeDiffType,
    val beforeNode: ObservationNode? = null,
    val afterNode: ObservationNode? = null,
    val changedAttributes: List<String> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("nodeIdentity", nodeIdentity)
            put("diffType", diffType.name)
            beforeNode?.let { put("beforeNode", it.toJsonObject()) }
            afterNode?.let { put("afterNode", it.toJsonObject()) }
            if (changedAttributes.isNotEmpty()) {
                put("changedAttributes", JSONArray(changedAttributes))
            }
        }
    }
}

data class SnapshotDiffResult(
    val snapshotBeforeId: String?,
    val snapshotAfterId: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAdded: Int,
    val totalRemoved: Int,
    val totalChanged: Int,
    val totalUnchanged: Int,
    val diffEntries: List<NodeDiffEntry>
) {
    val hasChanges: Boolean get() = totalAdded > 0 || totalRemoved > 0 || totalChanged > 0

    fun toJsonString(): String {
        return JSONObject().apply {
            snapshotBeforeId?.let { put("snapshotBeforeId", it) }
            snapshotAfterId?.let { put("snapshotAfterId", it) }
            put("timestamp", timestamp)
            put("totalAdded", totalAdded)
            put("totalRemoved", totalRemoved)
            put("totalChanged", totalChanged)
            put("totalUnchanged", totalUnchanged)
            put("hasChanges", hasChanges)
            val entriesArray = JSONArray()
            diffEntries.forEach { entriesArray.put(it.toJsonObject()) }
            put("diffEntries", entriesArray)
        }.toString(2)
    }
}

class SnapshotDiffEngine {

    fun computeDiff(beforeSnapshot: ObservationSnapshot?, afterSnapshot: ObservationSnapshot?): SnapshotDiffResult {
        val beforeNodes = flattenSnapshotNodes(beforeSnapshot)
        val afterNodes = flattenSnapshotNodes(afterSnapshot)

        val beforeMap = beforeNodes.associateBy { it.nodeIdentity.ifBlank { it.nodeId } }
        val afterMap = afterNodes.associateBy { it.nodeIdentity.ifBlank { it.nodeId } }

        val allIdentities = (beforeMap.keys + afterMap.keys).distinct()
        val entries = mutableListOf<NodeDiffEntry>()

        var addedCount = 0
        var removedCount = 0
        var changedCount = 0
        var unchangedCount = 0

        for (id in allIdentities) {
            val before = beforeMap[id]
            val after = afterMap[id]

            when {
                before == null && after != null -> {
                    addedCount++
                    entries.add(
                        NodeDiffEntry(
                            nodeIdentity = id,
                            diffType = NodeDiffType.ADDED,
                            afterNode = after
                        )
                    )
                }
                before != null && after == null -> {
                    removedCount++
                    entries.add(
                        NodeDiffEntry(
                            nodeIdentity = id,
                            diffType = NodeDiffType.REMOVED,
                            beforeNode = before
                        )
                    )
                }
                before != null && after != null -> {
                    val changedAttrs = compareNodeAttributes(before, after)
                    if (changedAttrs.isNotEmpty()) {
                        changedCount++
                        entries.add(
                            NodeDiffEntry(
                                nodeIdentity = id,
                                diffType = NodeDiffType.CHANGED,
                                beforeNode = before,
                                afterNode = after,
                                changedAttributes = changedAttrs
                            )
                        )
                    } else {
                        unchangedCount++
                        entries.add(
                            NodeDiffEntry(
                                nodeIdentity = id,
                                diffType = NodeDiffType.UNCHANGED,
                                beforeNode = before,
                                afterNode = after
                            )
                        )
                    }
                }
            }
        }

        return SnapshotDiffResult(
            snapshotBeforeId = beforeSnapshot?.snapshotId,
            snapshotAfterId = afterSnapshot?.snapshotId,
            totalAdded = addedCount,
            totalRemoved = removedCount,
            totalChanged = changedCount,
            totalUnchanged = unchangedCount,
            diffEntries = entries
        )
    }

    private fun flattenSnapshotNodes(snapshot: ObservationSnapshot?): List<ObservationNode> {
        val root = snapshot?.rootNode ?: return emptyList()
        val list = mutableListOf<ObservationNode>()
        fun collect(node: ObservationNode) {
            list.add(node)
            node.children.forEach { collect(it) }
        }
        collect(root)
        return list
    }

    private fun compareNodeAttributes(before: ObservationNode, after: ObservationNode): List<String> {
        val changes = mutableListOf<String>()
        if (before.text != after.text) changes.add("text")
        if (before.contentDescription != after.contentDescription) changes.add("contentDescription")
        if (before.bounds != after.bounds) changes.add("bounds")
        if (before.enabled != after.enabled) changes.add("enabled")
        if (before.visibleToUser != after.visibleToUser) changes.add("visibleToUser")
        if (before.focused != after.focused) changes.add("focused")
        if (before.selected != after.selected) changes.add("selected")
        if (before.checked != after.checked) changes.add("checked")
        if (before.children.size != after.children.size) changes.add("childrenCount")
        return changes
    }
}
