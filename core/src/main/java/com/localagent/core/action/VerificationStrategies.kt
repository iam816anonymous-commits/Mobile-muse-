package com.localagent.core.action

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

        val diff = diffEngine.computeDiff(preSnapshot, postSnapshot)

        // Rule 1: Identical pre/post snapshots
        if (diff.isIdentical) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_BUT_NOT_VERIFIED,
                resultCode = ResultCode.DISPATCHED_BUT_NOT_VERIFIED,
                reason = "Pre and post observation snapshots are identical. UI state did not visibly change.",
                diff = diff
            )
        }

        if (targetNodeId == null) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "UI state changed visibly after action dispatch",
                diff = diff
            )
        }

        // Target-specific checks
        val preTargetNode = SnapshotDiffEngine.findNodeById(preSnapshot.rootNode, targetNodeId)
        val postTargetNode = SnapshotDiffEngine.findNodeById(postSnapshot.rootNode, targetNodeId)

        // Case A: Target disappeared after action
        if (preTargetNode != null && postTargetNode == null) {
            val removedEntry = diff.removedNodes.firstOrNull { it.nodeId == targetNodeId }
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "Target node $targetNodeId was removed/navigated away from UI",
                diff = diff
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
                    diff = diff
                )
            }

            val changedEntry = diff.changedNodes.firstOrNull { it.nodeId == targetNodeId }
            if (changedEntry != null) {
                return VerificationResult(
                    status = VerificationStatus.EXECUTED_AND_VERIFIED,
                    resultCode = ResultCode.SUCCESS_VERIFIED,
                    reason = "Target node $targetNodeId attributes changed: ${changedEntry.attributeChanges}",
                    diff = diff
                )
            }
        }

        // Case C: Unrelated UI change
        if (diff.addedNodes.isNotEmpty() || diff.removedNodes.isNotEmpty() || diff.changedNodes.isNotEmpty()) {
            return VerificationResult(
                status = VerificationStatus.EXECUTED_AND_VERIFIED,
                resultCode = ResultCode.SUCCESS_VERIFIED,
                reason = "UI changed visibly with ${diff.addedNodes.size} added, ${diff.removedNodes.size} removed, ${diff.changedNodes.size} changed nodes",
                diff = diff
            )
        }

        return VerificationResult(
            status = VerificationStatus.VERIFICATION_FAILED,
            resultCode = ResultCode.ACTION_FAILED,
            reason = "No target-aware or UI state change confirmed for node $targetNodeId",
            diff = diff
        )
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

        val diff = diffEngine.computeDiff(preSnapshot, postSnapshot)

        val packageChanged = preSnapshot.packageName != postSnapshot.packageName
        val activityChanged = preSnapshot.activityName != null &&
                postSnapshot.activityName != null &&
                preSnapshot.activityName != postSnapshot.activityName
        val windowChanged = preSnapshot.windowId != postSnapshot.windowId

        if (packageChanged || activityChanged || windowChanged || !diff.isIdentical) {
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
                diff = diff
            )
        }

        return VerificationResult(
            status = VerificationStatus.EXECUTED_BUT_NOT_VERIFIED,
            resultCode = ResultCode.DISPATCHED_BUT_NOT_VERIFIED,
            reason = "Navigation action $actionType dispatched but no package, activity, window, or UI change occurred",
            diff = diff
        )
    }
}
