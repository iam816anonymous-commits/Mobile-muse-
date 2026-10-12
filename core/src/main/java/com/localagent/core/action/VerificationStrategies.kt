package com.localagent.core.action

import com.localagent.core.observation.NodeDiffType
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.observation.SnapshotDiffEngine
import com.localagent.core.result.ResultCode

interface VerificationStrategy {
    fun verify(
        actionType: ActionType,
        targetNodeId: String?,
        preSnapshot: ObservationSnapshot?,
        postSnapshot: ObservationSnapshot?
    ): VerificationResult
}

class TargetAwareVerificationStrategy(
    private val diffEngine: SnapshotDiffEngine = SnapshotDiffEngine()
) : VerificationStrategy {

    fun getSettleMillis(actionType: ActionType): Long {
        return when (actionType) {
            ActionType.UI_CLICK, ActionType.UI_LONG_CLICK -> 600L
            ActionType.UI_SCROLL_FORWARD, ActionType.UI_SCROLL_BACKWARD -> 800L
            ActionType.UI_TEXT_INPUT -> 400L
            else -> 300L
        }
    }

    override fun verify(
        actionType: ActionType,
        targetNodeId: String?,
        preSnapshot: ObservationSnapshot?,
        postSnapshot: ObservationSnapshot?
    ): VerificationResult {
        if (preSnapshot == null || postSnapshot == null) {
            return VerificationResult(
                status = VerificationStatus.VERIFICATION_FAILED,
                resultCode = ResultCode.ACTION_FAILED,
                reason = "Pre or post observation snapshot is null"
            )
        }

        val diffResult = diffEngine.computeDiff(preSnapshot, postSnapshot)

        val packageChanged = preSnapshot.packageName.isNotBlank() &&
                postSnapshot.packageName.isNotBlank() &&
                preSnapshot.packageName != postSnapshot.packageName

        if (packageChanged) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "Target action caused package transition from '${preSnapshot.packageName}' to '${postSnapshot.packageName}'",
                diffResult = diffResult
            )
        }

        // Target-specific node checks
        val preTargetNode = targetNodeId?.let { findNodeInSnapshot(preSnapshot.rootNode, it) }
        val postTargetNode = targetNodeId?.let { findNodeInSnapshot(postSnapshot.rootNode, it) }

        // Case A: Target disappeared after action (navigated away or dismissed)
        if (targetNodeId != null && preTargetNode != null && postTargetNode == null) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "Target node $targetNodeId was removed/navigated away from UI",
                diffResult = diffResult
            )
        }

        // Case B: Target identity changed
        if (preTargetNode != null && postTargetNode != null) {
            if (preTargetNode.nodeIdentity.isNotBlank() &&
                postTargetNode.nodeIdentity.isNotBlank() &&
                preTargetNode.nodeIdentity != postTargetNode.nodeIdentity
            ) {
                return VerificationResult(
                    status = VerificationStatus.VERIFICATION_FAILED,
                    resultCode = ResultCode.TARGET_STALE,
                    reason = "Target node identity changed unexpectedly after action from '${preTargetNode.nodeIdentity}' to '${postTargetNode.nodeIdentity}'",
                    diffResult = diffResult
                )
            }
        }

        // Case C: UI diff changes present
        if (diffResult.hasChanges || diffResult.totalAdded > 0 || diffResult.totalRemoved > 0 || diffResult.totalChanged > 0) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "UI changed visibly with ${diffResult.totalAdded} added, ${diffResult.totalRemoved} removed, ${diffResult.totalChanged} changed nodes",
                diffResult = diffResult
            )
        }

        // Case D: Fresh post-action observation captured after successful dispatch
        if (postSnapshot.generation > preSnapshot.generation || postSnapshot.timestamp > preSnapshot.timestamp) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "Action '$actionType' on '${targetNodeId ?: "screen"}' dispatched successfully and verified against fresh post-action observation (Gen ${postSnapshot.generation}).",
                diffResult = diffResult
            )
        }

        // Case E: Stale post-action snapshot (generation and timestamp did not advance)
        return VerificationResult(
            status = VerificationStatus.EXECUTED_BUT_NOT_VERIFIED,
            resultCode = ResultCode.DISPATCHED_BUT_NOT_VERIFIED,
            reason = "Action '$actionType' dispatched, but post-action observation is stale (Gen ${preSnapshot.generation}, PreTime ${preSnapshot.timestamp}, PostTime ${postSnapshot.timestamp}).",
            diffResult = diffResult
        )
    }

    private fun findNodeInSnapshot(root: ObservationNode?, nodeId: String): ObservationNode? {
        if (root == null) return null
        if (root.nodeId == nodeId) return root
        for (child in root.children) {
            val match = findNodeInSnapshot(child, nodeId)
            if (match != null) return match
        }
        return null
    }
}

class NavigationAwareVerificationStrategy(
    private val diffEngine: SnapshotDiffEngine = SnapshotDiffEngine()
) : VerificationStrategy {

    override fun verify(
        actionType: ActionType,
        targetNodeId: String?,
        preSnapshot: ObservationSnapshot?,
        postSnapshot: ObservationSnapshot?
    ): VerificationResult {
        if (preSnapshot == null || postSnapshot == null) {
            return VerificationResult(
                status = VerificationStatus.VERIFICATION_FAILED,
                resultCode = ResultCode.ACTION_FAILED,
                reason = "Pre or post observation snapshot is null for navigation action"
            )
        }

        val diffResult = diffEngine.computeDiff(preSnapshot, postSnapshot)

        val packageChanged = preSnapshot.packageName != postSnapshot.packageName
        val activityChanged = preSnapshot.activityName != null &&
                postSnapshot.activityName != null &&
                preSnapshot.activityName != postSnapshot.activityName
        val windowChanged = preSnapshot.windowId != postSnapshot.windowId

        if (packageChanged || activityChanged || windowChanged || diffResult.hasChanges) {
            val navReason = when {
                packageChanged -> "Package navigated from ${preSnapshot.packageName} to ${postSnapshot.packageName}"
                activityChanged -> "Activity navigated from ${preSnapshot.activityName} to ${postSnapshot.activityName}"
                windowChanged -> "Window navigated from ID ${preSnapshot.windowId} to ${postSnapshot.windowId}"
                else -> "Navigation UI content changed visibly"
            }

            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = navReason,
                diffResult = diffResult
            )
        }

        return VerificationResult(
            status = VerificationStatus.EXECUTED_BUT_NOT_VERIFIED,
            resultCode = ResultCode.DISPATCHED_BUT_NOT_VERIFIED,
            reason = "Navigation action $actionType dispatched but no package, activity, window, or UI change occurred",
            diffResult = diffResult
        )
    }
}
