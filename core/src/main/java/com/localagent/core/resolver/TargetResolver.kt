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
        val requestedNode = nodeMap[nodeId]

        if (requestedNode == null) {
            return TargetResolutionResult(
                requestedNodeId = nodeId,
                actionType = actionType,
                strategy = TargetResolutionStrategy.NO_TARGET,
                status = TargetResolutionStatus.TARGET_NOT_FOUND,
                failureReason = "Requested node ID $nodeId not found in snapshot"
            )
        }

        return ancestorResolver.resolve(snapshot, requestedNode, actionType)
    }

    fun resolveTargetByIdentity(
        snapshot: ObservationSnapshot,
        nodeIdentity: String,
        actionType: TargetActionType
    ): TargetResolutionResult {
        if (snapshot.rootNode == null) {
            return TargetResolutionResult(
                requestedNodeId = "",
                requestedNodeIdentity = nodeIdentity,
                actionType = actionType,
                strategy = TargetResolutionStrategy.NO_TARGET,
                status = TargetResolutionStatus.STALE_SNAPSHOT,
                failureReason = "Observation snapshot root node is null"
            )
        }

        val nodeMap = ActionableAncestorResolver.buildNodeMap(snapshot.rootNode)
        val matchingNode = nodeMap.values.firstOrNull { it.nodeIdentity == nodeIdentity }

        if (matchingNode == null) {
            return TargetResolutionResult(
                requestedNodeId = "",
                requestedNodeIdentity = nodeIdentity,
                actionType = actionType,
                strategy = TargetResolutionStrategy.NO_TARGET,
                status = TargetResolutionStatus.TARGET_NOT_FOUND,
                failureReason = "Requested node identity $nodeIdentity not found in snapshot"
            )
        }

        return ancestorResolver.resolve(snapshot, matchingNode, actionType)
    }

    fun resolveTarget(
        snapshot: ObservationSnapshot,
        requestedNode: ObservationNode,
        actionType: TargetActionType
    ): TargetResolutionResult {
        return ancestorResolver.resolve(snapshot, requestedNode, actionType)
    }
}
