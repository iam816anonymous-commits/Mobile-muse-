package com.localagent.core.observation

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class NodeIdentityConfidence {
    EXACT,      // Resource ID present + non-blank text or content description
    HIGH,       // Resource ID present without text OR unique class + non-blank text in parent scope
    MEDIUM,     // Class name + non-blank text/description without Resource ID
    LOW,        // Structural index / Class name only without text or Resource ID
    EPHEMERAL   // Dynamic / synthetic un-indexed node
}

data class ObservationBounds(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("left", left)
            put("top", top)
            put("right", right)
            put("bottom", bottom)
            put("width", width)
            put("height", height)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ObservationBounds {
            return ObservationBounds(
                left = json.optInt("left", 0),
                top = json.optInt("top", 0),
                right = json.optInt("right", 0),
                bottom = json.optInt("bottom", 0)
            )
        }
    }
}

data class ObservationNode(
    val nodeId: String,
    val className: String = "",
    val text: String? = null,
    val contentDescription: String? = null,
    val resourceId: String? = null,
    val packageName: String = "",
    val bounds: ObservationBounds = ObservationBounds(),
    val enabled: Boolean = true,
    val visibleToUser: Boolean = true,
    val clickable: Boolean = false,
    val longClickable: Boolean = false,
    val focusable: Boolean = false,
    val focused: Boolean = false,
    val scrollable: Boolean = false,
    val selected: Boolean = false,
    val checkable: Boolean = false,
    val checked: Boolean = false,
    val editable: Boolean = false,
    val parentInstanceId: String? = null,
    val nodeIdentity: String = "",
    val identityConfidence: NodeIdentityConfidence = NodeIdentityConfidence.LOW,
    val children: List<ObservationNode> = emptyList()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("nodeId", nodeId)
            put("className", className)
            text?.let { put("text", it) }
            contentDescription?.let { put("contentDescription", it) }
            resourceId?.let { put("resourceId", it) }
            put("packageName", packageName)
            put("bounds", bounds.toJsonObject())
            put("enabled", enabled)
            put("visibleToUser", visibleToUser)
            put("clickable", clickable)
            put("longClickable", longClickable)
            put("focusable", focusable)
            put("focused", focused)
            put("scrollable", scrollable)
            put("selected", selected)
            put("checkable", checkable)
            put("checked", checked)
            put("editable", editable)
            parentInstanceId?.let { put("parentInstanceId", it) }
            put("nodeIdentity", nodeIdentity)
            put("identityConfidence", identityConfidence.name)

            val childrenArray = JSONArray()
            children.forEach { childrenArray.put(it.toJsonObject()) }
            put("children", childrenArray)
        }
    }

    companion object {
        fun computeIdentity(
            packageName: String,
            className: String,
            resourceId: String?,
            text: String?,
            contentDescription: String?,
            childIndex: Int,
            parentIdentity: String?
        ): Pair<String, NodeIdentityConfidence> {
            val resIdStr = resourceId?.trim()?.ifBlank { null }
            val textStr = text?.trim()?.ifBlank { null }
            val descStr = contentDescription?.trim()?.ifBlank { null }
            val labelText = textStr ?: descStr
            val classSimple = className.substringAfterLast('.')

            return when {
                resIdStr != null && labelText != null -> {
                    val id = "id:${resIdStr.substringAfterLast('/')}_text:${labelText.take(30)}"
                    Pair(id, NodeIdentityConfidence.EXACT)
                }
                resIdStr != null -> {
                    val id = "id:${resIdStr.substringAfterLast('/')}_idx:$childIndex"
                    Pair(id, NodeIdentityConfidence.HIGH)
                }
                labelText != null -> {
                    val label = labelText.take(30)
                    val parentContext = if (parentIdentity != null) parentIdentity.take(20) else "root"
                    val id = "cls:${classSimple}_lbl:${label}_parent:$parentContext"
                    Pair(id, NodeIdentityConfidence.MEDIUM)
                }
                parentIdentity != null -> {
                    val id = "cls:${classSimple}_idx:${childIndex}_parent:${parentIdentity.take(20)}"
                    Pair(id, NodeIdentityConfidence.LOW)
                }
                else -> {
                    val id = "ephemeral_cls:${classSimple}_idx:$childIndex"
                    Pair(id, NodeIdentityConfidence.EPHEMERAL)
                }
            }
        }

        fun fromJsonObject(json: JSONObject): ObservationNode {
            val childrenList = mutableListOf<ObservationNode>()
            val childrenArray = json.optJSONArray("children")
            if (childrenArray != null) {
                for (i in 0 until childrenArray.length()) {
                    val childObj = childrenArray.optJSONObject(i)
                    if (childObj != null) {
                        childrenList.add(fromJsonObject(childObj))
                    }
                }
            }

            val confidenceStr = json.optString("identityConfidence", NodeIdentityConfidence.LOW.name)
            val confidence = try {
                NodeIdentityConfidence.valueOf(confidenceStr)
            } catch (_: Exception) {
                NodeIdentityConfidence.LOW
            }

            return ObservationNode(
                nodeId = json.getString("nodeId"),
                className = json.optString("className", ""),
                text = if (json.has("text")) json.optString("text") else null,
                contentDescription = if (json.has("contentDescription")) json.optString("contentDescription") else null,
                resourceId = if (json.has("resourceId")) json.optString("resourceId") else null,
                packageName = json.optString("packageName", ""),
                bounds = ObservationBounds.fromJsonObject(json.optJSONObject("bounds") ?: JSONObject()),
                enabled = json.optBoolean("enabled", true),
                visibleToUser = json.optBoolean("visibleToUser", true),
                clickable = json.optBoolean("clickable", false),
                longClickable = json.optBoolean("longClickable", false),
                focusable = json.optBoolean("focusable", false),
                focused = json.optBoolean("focused", false),
                scrollable = json.optBoolean("scrollable", false),
                selected = json.optBoolean("selected", false),
                checkable = json.optBoolean("checkable", false),
                checked = json.optBoolean("checked", false),
                editable = json.optBoolean("editable", false),
                parentInstanceId = if (json.has("parentInstanceId")) json.optString("parentInstanceId") else null,
                nodeIdentity = json.optString("nodeIdentity", ""),
                identityConfidence = confidence,
                children = childrenList
            )
        }
    }
}

data class ObservationTruncationInfo(
    val isTruncated: Boolean = false,
    val reason: String = "NONE", // NONE, MAX_NODES_EXCEEDED, MAX_DEPTH_EXCEEDED
    val totalNodesTraversed: Int = 0,
    val maxDepthReached: Int = 0
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("isTruncated", isTruncated)
            put("reason", reason)
            put("totalNodesTraversed", totalNodesTraversed)
            put("maxDepthReached", maxDepthReached)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ObservationTruncationInfo {
            return ObservationTruncationInfo(
                isTruncated = json.optBoolean("isTruncated", false),
                reason = json.optString("reason", "NONE"),
                totalNodesTraversed = json.optInt("totalNodesTraversed", 0),
                maxDepthReached = json.optInt("maxDepthReached", 0)
            )
        }
    }
}

data class ObservationSnapshot(
    val snapshotId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val activityName: String? = null,
    val windowId: Int = 0,
    val nodeCount: Int = 0,
    val rootNode: ObservationNode? = null,
    val truncationInfo: ObservationTruncationInfo = ObservationTruncationInfo(),
    val metadataJson: String = "{}"
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("snapshotId", snapshotId)
            put("timestamp", timestamp)
            put("packageName", packageName)
            activityName?.let { put("activityName", it) }
            put("windowId", windowId)
            put("nodeCount", nodeCount)
            rootNode?.let { put("rootNode", it.toJsonObject()) }
            put("truncationInfo", truncationInfo.toJsonObject())
            put("metadataJson", metadataJson)
        }.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): ObservationSnapshot {
            val json = JSONObject(jsonStr)
            val rootNodeObj = json.optJSONObject("rootNode")
            val rootNode = rootNodeObj?.let { ObservationNode.fromJsonObject(it) }

            return ObservationSnapshot(
                snapshotId = json.optString("snapshotId", UUID.randomUUID().toString()),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                packageName = json.optString("packageName", ""),
                activityName = if (json.has("activityName")) json.optString("activityName") else null,
                windowId = json.optInt("windowId", 0),
                nodeCount = json.optInt("nodeCount", 0),
                rootNode = rootNode,
                truncationInfo = ObservationTruncationInfo.fromJsonObject(json.optJSONObject("truncationInfo") ?: JSONObject()),
                metadataJson = json.optString("metadataJson", "{}")
            )
        }
    }
}
