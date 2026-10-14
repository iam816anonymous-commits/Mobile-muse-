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

        // Build frequency-indexed maps so duplicate node Identities/resource IDs do not overwrite each other
        val beforeMap = buildUniqueNodeMap(beforeNodes)
        val afterMap = buildUniqueNodeMap(afterNodes)

        val allKeys = (beforeMap.keys + afterMap.keys).distinct()
        val entries = mutableListOf<NodeDiffEntry>()

        val unmappedBefore = mutableMapOf<String, ObservationNode>()
        val unmappedAfter = mutableMapOf<String, ObservationNode>()

        var addedCount = 0
        var removedCount = 0
        var changedCount = 0
        var unchangedCount = 0

        for (key in allKeys) {
            val before = beforeMap[key]
            val after = afterMap[key]

            when {
                before == null && after != null -> unmappedAfter[key] = after
                before != null && after == null -> unmappedBefore[key] = before
                before != null && after != null -> {
                    val changedAttrs = compareNodeAttributes(before, after)
                    if (changedAttrs.isNotEmpty()) {
                        changedCount++
                        entries.add(
                            NodeDiffEntry(
                                nodeIdentity = before.nodeIdentity.ifBlank { key },
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
                                nodeIdentity = before.nodeIdentity.ifBlank { key },
                                diffType = NodeDiffType.UNCHANGED,
                                beforeNode = before,
                                afterNode = after
                            )
                        )
                    }
                }
            }
        }

        // Structural positional matching for remaining unmapped nodes with matching nodeId or resourceId
        val resolvedAfterKeys = mutableSetOf<String>()
        for ((bKey, before) in unmappedBefore) {
            val matchKey = unmappedAfter.entries.firstOrNull { (aKey, after) ->
                !resolvedAfterKeys.contains(aKey) &&
                        (after.nodeId == before.nodeId ||
                                (!before.resourceId.isNullOrBlank() && before.resourceId == after.resourceId && after.className == before.className))
            }?.key

            if (matchKey != null) {
                val after = unmappedAfter[matchKey]!!
                resolvedAfterKeys.add(matchKey)
                val changedAttrs = compareNodeAttributes(before, after)
                if (changedAttrs.isNotEmpty()) {
                    changedCount++
                    entries.add(
                        NodeDiffEntry(
                            nodeIdentity = before.nodeIdentity.ifBlank { bKey },
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
                            nodeIdentity = before.nodeIdentity.ifBlank { bKey },
                            diffType = NodeDiffType.UNCHANGED,
                            beforeNode = before,
                            afterNode = after
                        )
                    )
                }
            } else {
                removedCount++
                entries.add(
                    NodeDiffEntry(
                        nodeIdentity = before.nodeIdentity.ifBlank { bKey },
                        diffType = NodeDiffType.REMOVED,
                        beforeNode = before
                    )
                )
            }
        }

        for ((aKey, after) in unmappedAfter) {
            if (!resolvedAfterKeys.contains(aKey)) {
                addedCount++
                entries.add(
                    NodeDiffEntry(
                        nodeIdentity = after.nodeIdentity.ifBlank { aKey },
                        diffType = NodeDiffType.ADDED,
                        afterNode = after
                    )
                )
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

    private fun buildUniqueNodeMap(nodes: List<ObservationNode>): Map<String, ObservationNode> {
        val map = mutableMapOf<String, ObservationNode>()
        val counts = mutableMapOf<String, Int>()

        for (node in nodes) {
            val baseKey = node.nodeIdentity.ifBlank { node.resourceId?.ifBlank { null } ?: node.nodeId }
            val count = counts.getOrDefault(baseKey, 0)
            counts[baseKey] = count + 1
            val uniqueKey = if (count == 0) baseKey else "$baseKey#$count"
            map[uniqueKey] = node
        }
        return map
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
        if (before.resourceId != after.resourceId) changes.add("resourceId")
        if (before.enabled != after.enabled) changes.add("enabled")
        if (before.editable != after.editable) changes.add("editable")
        if (before.checked != after.checked) changes.add("checked")
        if (before.selected != after.selected) changes.add("selected")
        if (before.visibleToUser != after.visibleToUser) changes.add("visibleToUser")

        // Significant bounds shift (viewport scroll or container resize > 30px)
        val dx = kotlin.math.abs(before.bounds.left - after.bounds.left)
        val dy = kotlin.math.abs(before.bounds.top - after.bounds.top)
        val dw = kotlin.math.abs(before.bounds.width - after.bounds.width)
        val dh = kotlin.math.abs(before.bounds.height - after.bounds.height)
        if (dx > 30 || dy > 30 || dw > 30 || dh > 30) {
            changes.add("bounds")
        }

        if (before.children.size != after.children.size) changes.add("childrenCount")
        return changes
    }
}
