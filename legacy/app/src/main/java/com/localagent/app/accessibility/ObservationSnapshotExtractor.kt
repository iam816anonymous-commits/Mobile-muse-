package com.localagent.app.accessibility

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.core.observation.*
import java.util.concurrent.atomic.AtomicInteger

class ObservationSnapshotExtractor(
    val maxNodes: Int = 500,
    val maxDepth: Int = 30
) {

    fun extractSnapshot(
        rootNodeInfo: AccessibilityNodeInfo?,
        packageName: String = "",
        activityName: String? = null,
        windowId: Int = 0
    ): ObservationSnapshot {
        if (rootNodeInfo == null) {
            return ObservationSnapshot(
                packageName = packageName,
                activityName = activityName,
                windowId = windowId,
                nodeCount = 0,
                rootNode = null,
                truncationInfo = ObservationTruncationInfo(isTruncated = false, reason = "NONE")
            )
        }

        val nodeCounter = AtomicInteger(0)
        val maxDepthTracker = AtomicInteger(0)
        var isTruncatedByNodes = false
        var isTruncatedByDepth = false

        fun traverse(
            nodeInfo: AccessibilityNodeInfo,
            depth: Int,
            parentId: String?,
            parentIdentity: String?,
            childIndex: Int
        ): ObservationNode? {
            if (depth > maxDepthTracker.get()) {
                maxDepthTracker.set(depth)
            }

            if (depth > maxDepth) {
                isTruncatedByDepth = true
                return null
            }

            val currentCount = nodeCounter.incrementAndGet()
            if (currentCount > maxNodes) {
                isTruncatedByNodes = true
                return null
            }

            val nodeId = "node_${currentCount - 1}"

            // Bounds extraction
            val rect = Rect()
            nodeInfo.getBoundsInScreen(rect)
            val bounds = ObservationBounds(
                left = rect.left,
                top = rect.top,
                right = rect.right,
                bottom = rect.bottom
            )

            // Properties
            val nodeText = nodeInfo.text?.toString()
            val contentDesc = nodeInfo.contentDescription?.toString()
            val resId = nodeInfo.viewIdResourceName
            val classNameStr = nodeInfo.className?.toString() ?: ""
            val pkgNameStr = nodeInfo.packageName?.toString() ?: packageName

            // Identity & Confidence
            val (nodeIdentity, identityConfidence) = ObservationNode.computeIdentity(
                packageName = pkgNameStr,
                className = classNameStr,
                resourceId = resId,
                text = nodeText,
                contentDescription = contentDesc,
                childIndex = childIndex,
                parentIdentity = parentIdentity
            )

            val isClickable = nodeInfo.isClickable
            val isLongClickable = nodeInfo.isLongClickable
            val isScrollable = nodeInfo.isScrollable
            val isEditable = nodeInfo.isEditable
            val isEnabled = nodeInfo.isEnabled
            val isFocused = nodeInfo.isFocused
            val isFocusable = nodeInfo.isFocusable
            val isSelected = nodeInfo.isSelected
            val isCheckable = nodeInfo.isCheckable
            val isChecked = nodeInfo.isChecked
            val isVisible = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR_MR1) {
                nodeInfo.isVisibleToUser
            } else {
                true
            }

            val childrenList = mutableListOf<ObservationNode>()
            val childCount = nodeInfo.childCount

            for (i in 0 until childCount) {
                if (nodeCounter.get() >= maxNodes) {
                    isTruncatedByNodes = true
                    break
                }

                val childNode = nodeInfo.getChild(i)
                if (childNode != null) {
                    try {
                        val childDomainNode = traverse(
                            nodeInfo = childNode,
                            depth = depth + 1,
                            parentId = nodeId,
                            parentIdentity = nodeIdentity,
                            childIndex = i
                        )
                        if (childDomainNode != null) {
                            childrenList.add(childDomainNode)
                        }
                    } finally {
                        childNode.recycle()
                    }
                }
            }

            return ObservationNode(
                nodeId = nodeId,
                className = classNameStr,
                text = nodeText,
                contentDescription = contentDesc,
                resourceId = resId,
                packageName = pkgNameStr,
                bounds = bounds,
                enabled = isEnabled,
                visibleToUser = isVisible,
                clickable = isClickable,
                longClickable = isLongClickable,
                focusable = isFocusable,
                focused = isFocused,
                scrollable = isScrollable,
                selected = isSelected,
                checkable = isCheckable,
                checked = isChecked,
                editable = isEditable,
                parentInstanceId = parentId,
                nodeIdentity = nodeIdentity,
                identityConfidence = identityConfidence,
                children = childrenList
            )
        }

        val domainRoot = try {
            traverse(rootNodeInfo, depth = 0, parentId = null, parentIdentity = null, childIndex = 0)
        } catch (e: Exception) {
            System.err.println("ObservationSnapshotExtractor traversal exception: ${e.message}")
            null
        }

        val truncationReason = when {
            isTruncatedByNodes -> "MAX_NODES_EXCEEDED"
            isTruncatedByDepth -> "MAX_DEPTH_EXCEEDED"
            else -> "NONE"
        }

        return ObservationSnapshot(
            packageName = domainRoot?.packageName ?: packageName,
            activityName = activityName,
            windowId = windowId,
            nodeCount = nodeCounter.get(),
            rootNode = domainRoot,
            truncationInfo = ObservationTruncationInfo(
                isTruncated = isTruncatedByNodes || isTruncatedByDepth,
                reason = truncationReason,
                totalNodesTraversed = nodeCounter.get(),
                maxDepthReached = maxDepthTracker.get()
            )
        )
    }
}
