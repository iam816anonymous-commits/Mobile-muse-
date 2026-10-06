package com.localagent.core.resolver

import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import org.json.JSONObject

enum class TargetActionType {
    CLICK,
    LONG_CLICK,
    SCROLL,
    EDITABLE
}

enum class TargetResolutionStrategy {
    SELF_ACTIONABLE,
    CLICKABLE_ANCESTOR,
    LONG_CLICKABLE_ANCESTOR,
    SCROLLABLE_ANCESTOR,
    EDITABLE_SELF,
    EDITABLE_ANCESTOR,
    NO_TARGET
}

enum class TargetResolutionStatus {
    RESOLVED,
    TARGET_NOT_FOUND,
    NO_ACTIONABLE_TARGET,
    STALE_SNAPSHOT,
    INVALID_REQUEST
}

data class TargetResolutionResult(
    val requestedNodeId: String,
    val requestedNodeIdentity: String? = null,
    val resolvedNodeId: String? = null,
    val resolvedNodeIdentity: String? = null,
    val resolvedClassName: String? = null,
    val actionType: TargetActionType,
    val strategy: TargetResolutionStrategy,
    val confidence: NodeIdentityConfidence = NodeIdentityConfidence.LOW,
    val ancestorDepth: Int = 0,
    val reacquired: Boolean = false,
    val status: TargetResolutionStatus,
    val failureReason: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val resolvedNode: ObservationNode? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("requestedNodeId", requestedNodeId)
            requestedNodeIdentity?.let { put("requestedNodeIdentity", it) }
            resolvedNodeId?.let { put("resolvedNodeId", it) }
            resolvedNodeIdentity?.let { put("resolvedNodeIdentity", it) }
            resolvedClassName?.let { put("resolvedClassName", it) }
            put("actionType", actionType.name)
            put("strategy", strategy.name)
            put("confidence", confidence.name)
            put("ancestorDepth", ancestorDepth)
            put("reacquired", reacquired)
            put("status", status.name)
            failureReason?.let { put("failureReason", it) }
            put("timestamp", timestamp)
            resolvedNode?.let { put("resolvedNode", it.toJsonObject()) }
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): TargetResolutionResult {
            val resolvedNodeObj = json.optJSONObject("resolvedNode")
            val resolvedNode = resolvedNodeObj?.let { ObservationNode.fromJsonObject(it) }

            val confidenceStr = json.optString("confidence", NodeIdentityConfidence.LOW.name)
            val confidence = try {
                NodeIdentityConfidence.valueOf(confidenceStr)
            } catch (_: Exception) {
                NodeIdentityConfidence.LOW
            }

            val actionTypeStr = json.optString("actionType", TargetActionType.CLICK.name)
            val actionType = try {
                TargetActionType.valueOf(actionTypeStr)
            } catch (_: Exception) {
                TargetActionType.CLICK
            }

            val strategyStr = json.optString("strategy", TargetResolutionStrategy.NO_TARGET.name)
            val strategy = try {
                TargetResolutionStrategy.valueOf(strategyStr)
            } catch (_: Exception) {
                TargetResolutionStrategy.NO_TARGET
            }

            val statusStr = json.optString("status", TargetResolutionStatus.TARGET_NOT_FOUND.name)
            val status = try {
                TargetResolutionStatus.valueOf(statusStr)
            } catch (_: Exception) {
                TargetResolutionStatus.TARGET_NOT_FOUND
            }

            return TargetResolutionResult(
                requestedNodeId = json.optString("requestedNodeId", ""),
                requestedNodeIdentity = if (json.has("requestedNodeIdentity")) json.optString("requestedNodeIdentity") else null,
                resolvedNodeId = if (json.has("resolvedNodeId")) json.optString("resolvedNodeId") else null,
                resolvedNodeIdentity = if (json.has("resolvedNodeIdentity")) json.optString("resolvedNodeIdentity") else null,
                resolvedClassName = if (json.has("resolvedClassName")) json.optString("resolvedClassName") else null,
                actionType = actionType,
                strategy = strategy,
                confidence = confidence,
                ancestorDepth = json.optInt("ancestorDepth", 0),
                reacquired = json.optBoolean("reacquired", false),
                status = status,
                failureReason = if (json.has("failureReason")) json.optString("failureReason") else null,
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                resolvedNode = resolvedNode
            )
        }
    }
}
