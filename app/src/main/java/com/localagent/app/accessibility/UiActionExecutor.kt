package com.localagent.app.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.app.LocalAgentApplication
import com.localagent.core.action.ActionEvidence
import com.localagent.core.action.ActionExecutionResult
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
import com.localagent.core.action.TargetAwareVerificationStrategy
import com.localagent.core.action.VerificationResult
import com.localagent.core.action.VerificationStatus
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.resolver.TargetActionType
import com.localagent.core.resolver.TargetResolutionResult
import com.localagent.core.resolver.TargetResolutionStatus
import com.localagent.core.resolver.TargetResolver
import com.localagent.core.result.ResultCode
import java.util.UUID

class UiActionExecutor(
    private val accessibilityService: AgentAccessibilityService? = AgentAccessibilityService.INSTANCE,
    private val targetResolver: TargetResolver = TargetResolver(),
    private val liveTargetResolver: LiveTargetResolver = LiveTargetResolver(),
    private val verificationStrategy: TargetAwareVerificationStrategy = TargetAwareVerificationStrategy(),
    private val resolutionLogger: TargetResolutionLogger? = null,
    private val liveRootNodeProvider: () -> AccessibilityNodeInfo? = { (accessibilityService ?: AgentAccessibilityService.INSTANCE)?.rootInActiveWindow }
) {
    fun execute(request: ActionRequest): ActionExecutionResult {
        val startTime = System.currentTimeMillis()
        val service = accessibilityService ?: AgentAccessibilityService.INSTANCE

        if (service == null || !AgentAccessibilityService.isBound) {
            val verResult = VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACCESSIBILITY_UNAVAILABLE,
                reason = "AccessibilityService is unbound or unavailable"
            )
            return buildResultAndLog(request, null, false, null, null, verResult, startTime)
        }

        // Validate action type FIRST
        val targetActionType = when (request.actionType) {
            ActionType.UI_CLICK -> TargetActionType.CLICK
            ActionType.UI_LONG_CLICK -> TargetActionType.LONG_CLICK
            ActionType.UI_TEXT_INPUT -> TargetActionType.EDITABLE
            ActionType.UI_SCROLL_FORWARD, ActionType.UI_SCROLL_BACKWARD -> TargetActionType.SCROLL
            else -> {
                val verResult = VerificationResult(
                    status = VerificationStatus.ACTION_NOT_SUPPORTED,
                    resultCode = ResultCode.CAPABILITY_UNAVAILABLE,
                    reason = "Action ${request.actionType} is not a UI action"
                )
                return buildResultAndLog(request, null, false, null, null, verResult, startTime)
            }
        }

        // Capture pre-action observation snapshot
        val preSnapshot = service.captureLiveSnapshot()

        val targetNodeId = request.targetNodeId
        val targetNodeIdentity = request.targetNodeIdentity

        if (targetNodeId == null && targetNodeIdentity == null) {
            val verResult = VerificationResult(
                status = VerificationStatus.TARGET_NOT_FOUND,
                resultCode = ResultCode.TARGET_NOT_FOUND,
                reason = "Action request lacks targetNodeId and targetNodeIdentity"
            )
            return buildResultAndLog(request, null, false, preSnapshot, null, verResult, startTime)
        }

        // 1. Resolve Target
        resolutionLogger?.beginRequest()
        val resolutionResult = if (targetNodeId != null) {
            targetResolver.resolveTargetById(preSnapshot, targetNodeId, targetActionType)
        } else {
            targetResolver.resolveTargetByIdentity(preSnapshot, targetNodeIdentity!!, targetActionType)
        }
        resolutionLogger?.logTargetResolution(resolutionResult)

        if (resolutionResult.status != TargetResolutionStatus.RESOLVED || resolutionResult.resolvedNode == null) {
            resolutionLogger?.endRequest()
            val verResult = VerificationResult(
                status = if (resolutionResult.status == TargetResolutionStatus.NO_ACTIONABLE_TARGET) VerificationStatus.TARGET_NOT_ACTIONABLE else VerificationStatus.TARGET_NOT_FOUND,
                resultCode = if (resolutionResult.status == TargetResolutionStatus.NO_ACTIONABLE_TARGET) ResultCode.TARGET_NOT_ACTIONABLE else ResultCode.TARGET_NOT_FOUND,
                reason = resolutionResult.failureReason ?: "Target resolution failed"
            )
            return buildResultAndLog(request, resolutionResult, false, preSnapshot, null, verResult, startTime)
        }

        val resolvedNode = resolutionResult.resolvedNode!!

        // 2. Live Re-acquisition
        val liveRoot = liveRootNodeProvider()
        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = liveRoot,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedNode,
            actionType = targetActionType
        )
        liveRoot?.recycle()
        resolutionLogger?.logTargetResolution(reacquisitionResult.targetResolutionResult)
        resolutionLogger?.endRequest()

        if (!reacquisitionResult.reacquired || reacquisitionResult.liveNode == null) {
            val verResult = VerificationResult(
                status = VerificationStatus.TARGET_STALE,
                resultCode = ResultCode.TARGET_STALE,
                reason = reacquisitionResult.failureReason ?: "Live target re-acquisition failed"
            )
            return buildResultAndLog(request, reacquisitionResult.targetResolutionResult, false, preSnapshot, null, verResult, startTime)
        }

        val liveNode = reacquisitionResult.liveNode!!
        var dispatchSuccess = false

        // 3. Dispatch Action against Live Node
        try {
            dispatchSuccess = when (request.actionType) {
                ActionType.UI_CLICK -> liveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                ActionType.UI_LONG_CLICK -> liveNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                ActionType.UI_TEXT_INPUT -> {
                    val payload = request.textInputPayload ?: ""
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, payload)
                    }
                    liveNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
                ActionType.UI_SCROLL_FORWARD -> liveNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                ActionType.UI_SCROLL_BACKWARD -> liveNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                else -> false
            }
        } finally {
            liveNode.recycle()
        }

        // 4. Capture Post-Action Snapshot
        val postSnapshot = service.captureLiveSnapshot()

        // 5. Verify Target-Aware Result
        val verification = if (!dispatchSuccess) {
            VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACTION_FAILED,
                reason = "Framework performAction returned false"
            )
        } else {
            verificationStrategy.verify(
                actionType = request.actionType,
                targetNodeId = resolvedNode.nodeId,
                preSnapshot = preSnapshot,
                postSnapshot = postSnapshot
            )
        }

        return buildResultAndLog(request, reacquisitionResult.targetResolutionResult, dispatchSuccess, preSnapshot, postSnapshot, verification, startTime)
    }

    private fun buildResultAndLog(
        request: ActionRequest,
        targetRes: TargetResolutionResult?,
        dispatchSuccess: Boolean,
        preSnap: ObservationSnapshot?,
        postSnap: ObservationSnapshot?,
        verification: VerificationResult,
        startTime: Long
    ): ActionExecutionResult {
        val duration = System.currentTimeMillis() - startTime
        val result = ActionExecutionResult(
            request = request,
            targetResolutionResult = targetRes,
            dispatchSuccess = dispatchSuccess,
            preSnapshot = preSnap,
            postSnapshot = postSnap,
            verificationResult = verification,
            executionDurationMs = duration
        )

        logActionEvidence(result)
        return result
    }

    private fun logActionEvidence(result: ActionExecutionResult) {
        val app = (accessibilityService as? AgentAccessibilityService)?.application as? LocalAgentApplication ?: return
        val activeSessionId = app.eventLogger.getActiveSession().sessionId

        val evidence = ActionEvidence(
            executionId = result.executionId,
            actionType = result.request.actionType,
            targetIdentity = result.targetResolutionResult?.resolvedNodeIdentity ?: result.request.targetNodeIdentity ?: result.request.targetNodeId,
            targetConfidence = result.targetResolutionResult?.confidence,
            preSnapshotId = result.preSnapshot?.snapshotId,
            postSnapshotId = result.postSnapshot?.snapshotId,
            diffSummary = result.verificationResult.diffResult?.toJsonString() ?: "No diff",
            status = result.verificationResult.status,
            resultCode = result.verificationResult.resultCode
        )

        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACTION,
                eventType = "ACTION_EVIDENCE_GENERATED",
                actionType = result.request.actionType.name,
                sourceChannel = result.request.sourceChannel,
                resultCode = result.verificationResult.resultCode,
                durationMs = result.executionDurationMs,
                severity = if (result.verificationResult.status == VerificationStatus.EXECUTED_AND_VERIFIED) EventSeverity.INFO else EventSeverity.WARNING,
                errorCode = result.verificationResult.reason,
                metadataJson = evidence.toJsonObject().toString()
            )
        )
    }
}
