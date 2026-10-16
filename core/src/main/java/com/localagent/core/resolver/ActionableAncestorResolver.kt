package com.localagent.core.resolver

import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot

class ActionableAncestorResolver(
    private val maxDepth: Int = 30
) {
    fun resolve(
        snapshot: ObservationSnapshot,
        requestedNode: ObservationNode,
        actionType: TargetActionType
    ): TargetResolutionResult {
        val parentMap = buildParentMap(snapshot.rootNode)

        var current: ObservationNode? = requestedNode
        var depth = 0

        while (current != null && depth <= maxDepth) {
            val isActionable = when (actionType) {
                TargetActionType.CLICK -> current.clickable
                TargetActionType.LONG_CLICK -> current.longClickable
                TargetActionType.SCROLL -> current.scrollable
                TargetActionType.EDITABLE -> current.editable
            }

            if (isActionable) {
                val strategy = when {
                    depth == 0 && actionType == TargetActionType.EDITABLE -> TargetResolutionStrategy.EDITABLE_SELF
                    depth == 0 -> TargetResolutionStrategy.SELF_ACTIONABLE
                    actionType == TargetActionType.CLICK -> TargetResolutionStrategy.CLICKABLE_ANCESTOR
                    actionType == TargetActionType.LONG_CLICK -> TargetResolutionStrategy.LONG_CLICKABLE_ANCESTOR
                    actionType == TargetActionType.SCROLL -> TargetResolutionStrategy.SCROLLABLE_ANCESTOR
                    actionType == TargetActionType.EDITABLE -> TargetResolutionStrategy.EDITABLE_ANCESTOR
                    else -> TargetResolutionStrategy.SELF_ACTIONABLE
                }

                val confidence = computeConfidence(requestedNode, current, depth)

                return TargetResolutionResult(
                    requestedNodeId = requestedNode.nodeId,
                    requestedNodeIdentity = requestedNode.nodeIdentity.ifBlank { null },
                    resolvedNodeId = current.nodeId,
                    resolvedNodeIdentity = current.nodeIdentity.ifBlank { null },
                    resolvedClassName = current.className,
                    actionType = actionType,
                    strategy = strategy,
                    confidence = confidence,
                    ancestorDepth = depth,
                    reacquired = false,
                    status = TargetResolutionStatus.RESOLVED,
                    resolvedNode = current
                )
            }

            current = parentMap[current.nodeId]
            depth++
        }

        return TargetResolutionResult(
            requestedNodeId = requestedNode.nodeId,
            requestedNodeIdentity = requestedNode.nodeIdentity.ifBlank { null },
            actionType = actionType,
            strategy = TargetResolutionStrategy.NO_TARGET,
            confidence = NodeIdentityConfidence.LOW,
            ancestorDepth = depth.coerceAtMost(maxDepth),
            reacquired = false,
            status = TargetResolutionStatus.NO_ACTIONABLE_TARGET,
            failureReason = "No actionable ancestor found for $actionType within max depth $maxDepth"
        )
    }

    private fun computeConfidence(
        requestedNode: ObservationNode,
        resolvedNode: ObservationNode,
        ancestorDepth: Int
    ): NodeIdentityConfidence {
        if (ancestorDepth == 0) {
            return requestedNode.identityConfidence
        }

        // If either requested node or resolved ancestor has high/exact confidence, target resolution is HIGH or EXACT
        val bestConfidence = listOf(
            requestedNode.identityConfidence,
            resolvedNode.identityConfidence
        ).minByOrNull { it.ordinal } ?: NodeIdentityConfidence.LOW

        return when (bestConfidence) {
            NodeIdentityConfidence.EXACT -> NodeIdentityConfidence.EXACT
            NodeIdentityConfidence.HIGH -> NodeIdentityConfidence.HIGH
            NodeIdentityConfidence.MEDIUM -> if (ancestorDepth <= 2) NodeIdentityConfidence.HIGH else NodeIdentityConfidence.MEDIUM
            NodeIdentityConfidence.LOW -> if (ancestorDepth <= 1) NodeIdentityConfidence.MEDIUM else NodeIdentityConfidence.LOW
            NodeIdentityConfidence.EPHEMERAL -> NodeIdentityConfidence.LOW
        }
    }

    companion object {
        fun buildParentMap(root: ObservationNode?): Map<String, ObservationNode> {
            val map = mutableMapOf<String, ObservationNode>()
            if (root == null) return map

            fun traverse(node: ObservationNode) {
                for (child in node.children) {
                    map[child.nodeId] = node
                    traverse(child)
                }
            }

            traverse(root)
            return map
        }

        fun buildNodeMap(root: ObservationNode?): Map<String, ObservationNode> {
            val map = mutableMapOf<String, ObservationNode>()
            if (root == null) return map

            fun traverse(node: ObservationNode) {
                map[node.nodeId] = node
                for (child in node.children) {
                    traverse(child)
                }
            }

            traverse(root)
            return map
        }
    }
}
