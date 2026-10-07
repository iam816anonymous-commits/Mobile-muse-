package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.resolver.TargetActionType
import com.localagent.core.resolver.TargetResolutionResult

data class LiveReacquisitionResult(
    val targetResolutionResult: TargetResolutionResult,
    val liveNode: AccessibilityNodeInfo? = null,
    val reacquired: Boolean = false,
    val failureReason: String? = null
)

class LiveTargetResolver {

    /**
     * Re-acquires a live [AccessibilityNodeInfo] from [rootLiveNode] matching [resolvedTarget].
     * Verify that the live target exists, matches node identity/attributes, and satisfies [actionType].
     * Note: The caller is responsible for recycling [liveNode] when ownership ends!
     */
    fun reacquireLiveTarget(
        rootLiveNode: AccessibilityNodeInfo?,
        targetResolutionResult: TargetResolutionResult,
        resolvedTarget: ObservationNode,
        actionType: TargetActionType
    ): LiveReacquisitionResult {
        if (rootLiveNode == null) {
            return LiveReacquisitionResult(
                targetResolutionResult = targetResolutionResult.copy(
                    reacquired = false,
                    failureReason = "Root live node is null"
                ),
                liveNode = null,
                reacquired = false,
                failureReason = "Root live node is null"
            )
        }

        val targetIdentity = resolvedTarget.nodeIdentity
        var matchedLiveNode: AccessibilityNodeInfo? = null

        // Search live hierarchy for matching node
        try {
            matchedLiveNode = findMatchingLiveNode(rootLiveNode, resolvedTarget)
        } catch (e: Exception) {
            return LiveReacquisitionResult(
                targetResolutionResult = targetResolutionResult.copy(
                    reacquired = false,
                    failureReason = "Exception during live node search: ${e.message}"
                ),
                liveNode = null,
                reacquired = false,
                failureReason = "Exception during live node search: ${e.message}"
            )
        }

        if (matchedLiveNode == null) {
            return LiveReacquisitionResult(
                targetResolutionResult = targetResolutionResult.copy(
                    reacquired = false,
                    failureReason = "Target node identity '$targetIdentity' not found in current live UI hierarchy"
                ),
                liveNode = null,
                reacquired = false,
                failureReason = "Target node identity '$targetIdentity' not found in current live UI hierarchy"
            )
        }

        // Verify capability on live node
        var isActionable = when (actionType) {
            TargetActionType.CLICK -> matchedLiveNode.isClickable
            TargetActionType.LONG_CLICK -> matchedLiveNode.isLongClickable
            TargetActionType.SCROLL -> matchedLiveNode.isScrollable
            TargetActionType.EDITABLE -> matchedLiveNode.isEditable
        }

        // Ascend to clickable ancestor on live hierarchy if child node is matched (e.g. Calculator TextView inside MaterialButton)
        if (!isActionable && actionType == TargetActionType.CLICK) {
            var parent = matchedLiveNode.parent
            while (parent != null) {
                if (parent.isClickable) {
                    matchedLiveNode.recycle()
                    matchedLiveNode = parent
                    isActionable = true
                    break
                }
                val prev = parent
                parent = parent.parent
                prev.recycle()
            }
        }

        if (!isActionable) {
            matchedLiveNode.recycle()
            return LiveReacquisitionResult(
                targetResolutionResult = targetResolutionResult.copy(
                    reacquired = false,
                    failureReason = "Matched live node is not actionable for capability $actionType"
                ),
                liveNode = null,
                reacquired = false,
                failureReason = "Matched live node is not actionable for capability $actionType"
            )
        }

        val updatedResult = targetResolutionResult.copy(
            reacquired = true,
            resolvedNodeId = resolvedTarget.nodeId,
            resolvedNodeIdentity = targetIdentity,
            resolvedClassName = matchedLiveNode.className?.toString()
        )

        return LiveReacquisitionResult(
            targetResolutionResult = updatedResult,
            liveNode = matchedLiveNode,
            reacquired = true,
            failureReason = null
        )
    }

    private fun findMatchingLiveNode(
        root: AccessibilityNodeInfo,
        target: ObservationNode
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(AccessibilityNodeInfo.obtain(root))

        val targetResId = target.resourceId?.trim()?.ifBlank { null }
        val targetText = target.text?.trim()?.ifBlank { null }
        val targetDesc = target.contentDescription?.trim()?.ifBlank { null }
        val targetClass = target.className

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            val currentResId = current.viewIdResourceName?.trim()?.ifBlank { null }
            val currentText = current.text?.toString()?.trim()?.ifBlank { null }
            val currentDesc = current.contentDescription?.toString()?.trim()?.ifBlank { null }
            val currentClass = current.className?.toString() ?: ""

            var matches = false

            // Strategy 1: Resource ID + Text/Desc match
            if (targetResId != null && currentResId == targetResId) {
                if (targetText != null && currentText == targetText) {
                    matches = true
                } else if (targetDesc != null && currentDesc == targetDesc) {
                    matches = true
                } else if (targetText == null && targetDesc == null) {
                    matches = true
                }
            }

            // Strategy 2: Identity string match
            if (!matches && target.nodeIdentity.isNotBlank()) {
                val (currentIdentity, _) = ObservationNode.computeIdentity(
                    packageName = current.packageName?.toString() ?: "",
                    className = currentClass,
                    resourceId = currentResId,
                    text = currentText,
                    contentDescription = currentDesc,
                    childIndex = 0,
                    parentIdentity = null
                )
                if (currentIdentity == target.nodeIdentity) {
                    matches = true
                }
            }

            // Strategy 3: Text match (case-insensitive)
            if (!matches && targetText != null && currentText?.equals(targetText, ignoreCase = true) == true) {
                matches = true
            }

            // Strategy 4: Content description match (case-insensitive)
            if (!matches && targetDesc != null && currentDesc?.equals(targetDesc, ignoreCase = true) == true) {
                matches = true
            }

            if (matches) {
                // Clear remaining queued nodes to avoid leak
                while (queue.isNotEmpty()) {
                    queue.removeFirst().recycle()
                }
                return current
            }

            // Queue children
            for (i in 0 until current.childCount) {
                val child = current.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }

            current.recycle()
        }

        return null
    }
}
