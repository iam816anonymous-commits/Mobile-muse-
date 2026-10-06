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

        // Rule 1: Identical pre/post snapshots (no changes)
        if (!diffResult.hasChanges) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_BUT_NOT_VERIFIED,
                resultCode = ResultCode.DISPATCHED_BUT_NOT_VERIFIED,
                reason = "Pre and post observation snapshots are identical. UI state did not visibly change.",
                diffResult = diffResult
            )
        }

        if (targetNodeId == null) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "UI state changed visibly after action dispatch",
                diffResult = diffResult
            )
        }

        // Target-specific checks
        val preTargetNode = findNodeInSnapshot(preSnapshot.rootNode, targetNodeId)
        val postTargetNode = findNodeInSnapshot(postSnapshot.rootNode, targetNodeId)

        // Case A: Target disappeared after action
        if (preTargetNode != null && postTargetNode == null) {
            val removedEntry = diffResult.diffEntries.firstOrNull {
                it.diffType == NodeDiffType.REMOVED && (it.beforeNode?.nodeId == targetNodeId || it.nodeIdentity == preTargetNode.nodeIdentity)
            }
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

            val changedEntry = diffResult.diffEntries.firstOrNull {
                it.diffType == NodeDiffType.CHANGED && (it.afterNode?.nodeId == targetNodeId || it.beforeNode?.nodeId == targetNodeId)
            }
            if (changedEntry != null) {
                return VerificationResult(
                    status = VerificationStatus.EXECUTED_AND_VERIFIED,
                    resultCode = ResultCode.SUCCESS_VERIFIED,
                    reason = "Target node $targetNodeId attributes changed: ${changedEntry.changedAttributes}",
                    diffResult = diffResult
                )
            }
        }

        // Case C: Overall UI changes present
        if (diffResult.totalAdded > 0 || diffResult.totalRemoved > 0 || diffResult.totalChanged > 0) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "UI changed visibly with ${diffResult.totalAdded} added, ${diffResult.totalRemoved} removed, ${diffResult.totalChanged} changed nodes",
                diffResult = diffResult
            )
        }

        return VerificationResult(
            status = VerificationStatus.VERIFICATION_FAILED,
            resultCode = ResultCode.ACTION_FAILED,
            reason = "No target-aware or UI state change confirmed for node $targetNodeId",
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
