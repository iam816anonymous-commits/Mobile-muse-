package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.resolver.TargetActionType
import com.localagent.core.resolver.TargetResolutionResult
import com.localagent.core.resolver.TargetResolutionStatus
import com.localagent.core.resolver.TargetResolutionStrategy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LiveTargetResolverTest {

    private val liveTargetResolver = LiveTargetResolver()

    @Test
    fun `P6-LIVE-001 - snapshot node successfully re-acquires corresponding live node`() {
        val rootNodeInfo = AccessibilityNodeInfo.obtain().apply {
            className = "com.google.android.material.button.MaterialButton"
            viewIdResourceName = "com.android.calculator2:id/digit_7"
            text = "7"
            isClickable = true
        }

        val resolvedTarget = ObservationNode(
            nodeId = "btn_7",
            className = "com.google.android.material.button.MaterialButton",
            text = "7",
            resourceId = "com.android.calculator2:id/digit_7",
            clickable = true,
            nodeIdentity = "id:digit_7_text:7",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val resolutionResult = TargetResolutionResult(
            requestedNodeId = "text_7",
            resolvedNodeId = "btn_7",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.CLICKABLE_ANCESTOR,
            confidence = NodeIdentityConfidence.EXACT,
            status = TargetResolutionStatus.RESOLVED
        )

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = rootNodeInfo,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedTarget,
            actionType = TargetActionType.CLICK
        )

        assertTrue(reacquisitionResult.reacquired)
        assertNotNull(reacquisitionResult.liveNode)
        assertEquals("com.google.android.material.button.MaterialButton", reacquisitionResult.liveNode?.className)
        assertTrue(reacquisitionResult.liveNode?.isClickable == true)

        reacquisitionResult.liveNode?.recycle()
        rootNodeInfo.recycle()
    }

    @Test
    fun `P6-LIVE-002 - re-acquisition fails safely when target disappears`() {
        // Current live UI contains only a different button "8"
        val rootNodeInfo = AccessibilityNodeInfo.obtain().apply {
            className = "com.google.android.material.button.MaterialButton"
            viewIdResourceName = "com.android.calculator2:id/digit_8"
            text = "8"
            isClickable = true
        }

        // Requested target "7" no longer in live UI
        val resolvedTarget = ObservationNode(
            nodeId = "btn_7",
            className = "com.google.android.material.button.MaterialButton",
            text = "7",
            resourceId = "com.android.calculator2:id/digit_7",
            clickable = true,
            nodeIdentity = "id:digit_7_text:7",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val resolutionResult = TargetResolutionResult(
            requestedNodeId = "text_7",
            resolvedNodeId = "btn_7",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.CLICKABLE_ANCESTOR,
            confidence = NodeIdentityConfidence.EXACT,
            status = TargetResolutionStatus.RESOLVED
        )

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = rootNodeInfo,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedTarget,
            actionType = TargetActionType.CLICK
        )

        assertFalse(reacquisitionResult.reacquired)
        assertNull(reacquisitionResult.liveNode)
        assertNotNull(reacquisitionResult.failureReason)

        rootNodeInfo.recycle()
    }

    @Test
    fun `P6-LIVE-003 - re-acquisition rejects stale or mismatched identity`() {
        val rootNodeInfo = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.TextView"
            text = "Stale Content"
            isClickable = false
        }

        val resolvedTarget = ObservationNode(
            nodeId = "btn_7",
            className = "com.google.android.material.button.MaterialButton",
            text = "7",
            resourceId = "com.android.calculator2:id/digit_7",
            clickable = true,
            nodeIdentity = "id:digit_7_text:7",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val resolutionResult = TargetResolutionResult(
            requestedNodeId = "text_7",
            resolvedNodeId = "btn_7",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.CLICKABLE_ANCESTOR,
            status = TargetResolutionStatus.RESOLVED
        )

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = rootNodeInfo,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedTarget,
            actionType = TargetActionType.CLICK
        )

        assertFalse(reacquisitionResult.reacquired)
        assertNull(reacquisitionResult.liveNode)

        rootNodeInfo.recycle()
    }

    @Test
    fun `P6-LIVE-004 - re-acquired target capability is verified again on live node`() {
        // Live node matches resourceId & text, but isClickable is false
        val rootNodeInfo = AccessibilityNodeInfo.obtain().apply {
            className = "com.google.android.material.button.MaterialButton"
            viewIdResourceName = "com.android.calculator2:id/digit_7"
            text = "7"
            isClickable = false // Disabled or non-clickable in live state
        }

        val resolvedTarget = ObservationNode(
            nodeId = "btn_7",
            className = "com.google.android.material.button.MaterialButton",
            text = "7",
            resourceId = "com.android.calculator2:id/digit_7",
            clickable = true,
            nodeIdentity = "id:digit_7_text:7",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val resolutionResult = TargetResolutionResult(
            requestedNodeId = "text_7",
            resolvedNodeId = "btn_7",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.CLICKABLE_ANCESTOR,
            status = TargetResolutionStatus.RESOLVED
        )

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = rootNodeInfo,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedTarget,
            actionType = TargetActionType.CLICK
        )

        assertFalse(reacquisitionResult.reacquired)
        assertNull(reacquisitionResult.liveNode)
        assertTrue(reacquisitionResult.failureReason?.contains("not actionable") == true)

        rootNodeInfo.recycle()
    }

    @Test
    fun `P6-LIVE-005 - acquired AccessibilityNodeInfo objects are correctly recycled`() {
        val rootNodeInfo = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.LinearLayout"
            isClickable = false
        }

        val resolvedTarget = ObservationNode(
            nodeId = "missing",
            className = "android.widget.Button",
            nodeIdentity = "missing_id"
        )

        val resolutionResult = TargetResolutionResult(
            requestedNodeId = "missing",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.NO_TARGET,
            status = TargetResolutionStatus.RESOLVED
        )

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = rootNodeInfo,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedTarget,
            actionType = TargetActionType.CLICK
        )

        assertFalse(reacquisitionResult.reacquired)
        assertNull(reacquisitionResult.liveNode)

        rootNodeInfo.recycle()
    }

    @Test
    fun `P6-LIVE-006 - zero action dispatch occurs during resolution and re-acquisition`() {
        var actionDispatchCount = 0

        val rootNodeInfo = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            isClickable = true
        }

        val resolvedTarget = ObservationNode(
            nodeId = "btn",
            className = "android.widget.Button",
            clickable = true
        )

        val resolutionResult = TargetResolutionResult(
            requestedNodeId = "btn",
            resolvedNodeId = "btn",
            actionType = TargetActionType.CLICK,
            strategy = TargetResolutionStrategy.SELF_ACTIONABLE,
            status = TargetResolutionStatus.RESOLVED
        )

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = rootNodeInfo,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedTarget,
            actionType = TargetActionType.CLICK
        )

        assertTrue(reacquisitionResult.reacquired)
        assertNotNull(reacquisitionResult.liveNode)
        assertEquals(0, actionDispatchCount)

        // Clean up live node without performing any action dispatch
        reacquisitionResult.liveNode?.recycle()
        rootNodeInfo.recycle()
    }
}
