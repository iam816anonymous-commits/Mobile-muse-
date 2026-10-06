package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import com.localagent.app.LocalAgentApplication
import com.localagent.core.action.ActionEvidence
import com.localagent.core.action.ActionExecutionResult
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
import com.localagent.core.action.NavigationAwareVerificationStrategy
import com.localagent.core.action.VerificationStatus
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.result.ResultCode
import java.util.UUID

class GlobalActionExecutor(
    private val accessibilityService: AccessibilityService? = AgentAccessibilityService.INSTANCE,
    private val verificationStrategy: NavigationAwareVerificationStrategy = NavigationAwareVerificationStrategy()
) {
    fun execute(request: ActionRequest): ActionExecutionResult {
        val startTime = System.currentTimeMillis()
        val service = accessibilityService ?: AgentAccessibilityService.INSTANCE

        if (service == null) {
            val verResult = com.localagent.core.action.VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACCESSIBILITY_UNAVAILABLE,
                reason = "AccessibilityService is unbound or unavailable"
            )
            return buildResultAndLog(request, null, false, null, null, verResult, startTime)
        }

        val globalActionInt = when (request.actionType) {
            ActionType.GLOBAL_BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            ActionType.GLOBAL_HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            ActionType.GLOBAL_RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
            else -> {
                val verResult = com.localagent.core.action.VerificationResult(
                    status = VerificationStatus.ACTION_NOT_SUPPORTED,
                    resultCode = ResultCode.CAPABILITY_UNAVAILABLE,
                    reason = "Action ${request.actionType} is not a global action"
                )
                return buildResultAndLog(request, null, false, null, null, verResult, startTime)
            }
        }

        // Capture pre-action observation snapshot
        val agentService = service as? AgentAccessibilityService
        val preSnapshot = agentService?.captureLiveSnapshot()

        // Execute global framework action
        val dispatchSuccess = service.performGlobalAction(globalActionInt)

        // Capture post-action observation snapshot
        val postSnapshot = agentService?.captureLiveSnapshot()

        val verification = if (!dispatchSuccess) {
            com.localagent.core.action.VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACTION_FAILED,
                reason = "performGlobalAction($globalActionInt) returned false from framework"
            )
        } else {
            verificationStrategy.verify(
                actionType = request.actionType,
                targetNodeId = null,
                preSnapshot = preSnapshot,
                postSnapshot = postSnapshot
            )
        }

        return buildResultAndLog(request, null, dispatchSuccess, preSnapshot, postSnapshot, verification, startTime)
    }

    private fun buildResultAndLog(
        request: ActionRequest,
        targetRes: com.localagent.core.resolver.TargetResolutionResult?,
        dispatchSuccess: Boolean,
        preSnap: ObservationSnapshot?,
        postSnap: ObservationSnapshot?,
        verification: com.localagent.core.action.VerificationResult,
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
            targetIdentity = result.request.targetNodeIdentity ?: result.request.targetNodeId,
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
