package com.localagent.core.resolver

import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot

class TargetResolver(
    private val ancestorResolver: ActionableAncestorResolver = ActionableAncestorResolver()
) {
    fun resolveTargetById(
        snapshot: ObservationSnapshot,
        nodeId: String,
        actionType: TargetActionType
    ): TargetResolutionResult {
        if (snapshot.rootNode == null) {
            return TargetResolutionResult(
                requestedNodeId = nodeId,
                actionType = actionType,
                strategy = TargetResolutionStrategy.NO_TARGET,
                status = TargetResolutionStatus.STALE_SNAPSHOT,
                failureReason = "Observation snapshot root node is null"
            )
        }

        val nodeMap = ActionableAncestorResolver.buildNodeMap(snapshot.rootNode)
        val allNodes = nodeMap.values.toList()
        val query = nodeId.trim()

        // Multi-strategy semantic target lookup
        // Priority 1: Exact internal nodeId match
        var matchingNode: ObservationNode? = nodeMap[nodeId]

        // Priority 2: Explicit node identity key match
        if (matchingNode == null && query.isNotBlank()) {
            matchingNode = allNodes.firstOrNull { it.nodeIdentity == query }
        }

        // Priority 3: Exact visible text match
        if (matchingNode == null && query.isNotBlank()) {
            matchingNode = allNodes.firstOrNull { it.text?.trim()?.equals(query, ignoreCase = true) == true }
        }

        // Priority 4: Exact content description match
        if (matchingNode == null && query.isNotBlank()) {
            matchingNode = allNodes.firstOrNull { it.contentDescription?.trim()?.equals(query, ignoreCase = true) == true }
        }

        // Priority 5: View ID / Resource Name match
        if (matchingNode == null && query.isNotBlank()) {
            matchingNode = allNodes.firstOrNull { node ->
                val res = node.resourceId?.trim() ?: ""
                res == query || res.endsWith(":id/$query") || res.endsWith("/$query")
            }
        }

        if (matchingNode == null) {
            return TargetResolutionResult(
                requestedNodeId = nodeId,
                actionType = actionType,
                strategy = TargetResolutionStrategy.NO_TARGET,
                status = TargetResolutionStatus.TARGET_NOT_FOUND,
                failureReason = "Target selector '$nodeId' not found in snapshot by ID, text, description, or resource name"
            )
        }

        return ancestorResolver.resolve(snapshot, matchingNode, actionType)
    }

    fun resolveTargetByIdentity(
        snapshot: ObservationSnapshot,
        nodeIdentity: String,
        actionType: TargetActionType
    ): TargetResolutionResult {
        return resolveTargetById(snapshot, nodeIdentity, actionType)
    }

    fun resolveTarget(
        snapshot: ObservationSnapshot,
        requestedNode: ObservationNode,
        actionType: TargetActionType
    ): TargetResolutionResult {
        return ancestorResolver.resolve(snapshot, requestedNode, actionType)
    }
}
