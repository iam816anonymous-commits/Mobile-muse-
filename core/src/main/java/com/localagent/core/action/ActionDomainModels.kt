package com.localagent.core.action

import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.observation.SnapshotDiffResult
import com.localagent.core.resolver.TargetResolutionResult
import com.localagent.core.result.ResultCode
import org.json.JSONObject
import java.util.UUID

enum class ActionType {
    GLOBAL_BACK,
    GLOBAL_HOME,
    GLOBAL_RECENTS,
    UI_CLICK,
    UI_LONG_CLICK,
    UI_TEXT_INPUT,
    UI_SCROLL_FORWARD,
    UI_SCROLL_BACKWARD
}

enum class VerificationStatus {
    EXECUTED_AND_VERIFIED,
    EXECUTED_BUT_NOT_VERIFIED,
    TARGET_NOT_FOUND,
    TARGET_NOT_ACTIONABLE,
    TARGET_STALE,
    ACTION_NOT_SUPPORTED,
    EXECUTION_FAILED,
    VERIFICATION_FAILED
}

data class ActionRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val actionType: ActionType,
    val targetNodeId: String? = null,
    val targetNodeIdentity: String? = null,
    val textInputPayload: String? = null,
    val sourceChannel: String = "CORE",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("requestId", requestId)
            put("actionType", actionType.name)
            targetNodeId?.let { put("targetNodeId", it) }
            targetNodeIdentity?.let { put("targetNodeIdentity", it) }
            textInputPayload?.let { put("textInputPayload", it) }
            put("sourceChannel", sourceChannel)
            put("timestamp", timestamp)
        }
    }
}

data class VerificationResult(
    val status: VerificationStatus,
    val resultCode: ResultCode,
    val reason: String? = null,
    val diffResult: SnapshotDiffResult? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("status", status.name)
            put("resultCode", resultCode.name)
            reason?.let { put("reason", it) }
            diffResult?.let { put("diffResult", it.toJsonString()) }
        }
    }
}

data class ActionExecutionResult(
    val executionId: String = UUID.randomUUID().toString(),
    val request: ActionRequest,
    val targetResolutionResult: TargetResolutionResult? = null,
    val dispatchSuccess: Boolean = false,
    val preSnapshot: ObservationSnapshot? = null,
    val postSnapshot: ObservationSnapshot? = null,
    val verificationResult: VerificationResult,
    val executionDurationMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("executionId", executionId)
            put("request", request.toJsonObject())
            targetResolutionResult?.let { put("targetResolutionResult", it.toJsonObject()) }
            put("dispatchSuccess", dispatchSuccess)
            preSnapshot?.let { put("preSnapshotId", it.snapshotId) }
            postSnapshot?.let { put("postSnapshotId", it.snapshotId) }
            put("verificationResult", verificationResult.toJsonObject())
            put("executionDurationMs", executionDurationMs)
            put("timestamp", timestamp)
        }
    }
}

data class ActionEvidence(
    val evidenceId: String = UUID.randomUUID().toString(),
    val executionId: String,
    val actionType: ActionType,
    val targetIdentity: String?,
    val targetConfidence: NodeIdentityConfidence?,
    val preSnapshotId: String?,
    val postSnapshotId: String?,
    val diffSummary: String,
    val status: VerificationStatus,
    val resultCode: ResultCode,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("evidenceId", evidenceId)
            put("executionId", executionId)
            put("actionType", actionType.name)
            targetIdentity?.let { put("targetIdentity", it) }
            targetConfidence?.let { put("targetConfidence", it.name) }
            preSnapshotId?.let { put("preSnapshotId", it) }
            postSnapshotId?.let { put("postSnapshotId", it) }
            put("diffSummary", diffSummary)
            put("status", status.name)
            put("resultCode", resultCode.name)
            put("timestamp", timestamp)
        }
    }
}
