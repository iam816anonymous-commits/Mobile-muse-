package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
import com.localagent.core.action.VerificationStatus
import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.result.ResultCode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ActionExecutionTest {

    private class TestableAgentAccessibilityService : AgentAccessibilityService() {
        var mockSnapshot: ObservationSnapshot? = null
        var mockRootNode: AccessibilityNodeInfo? = null
        var globalActionCallCount = 0
        var lastGlobalActionExecuted = -1

        override fun captureLiveSnapshot(): ObservationSnapshot {
            return mockSnapshot ?: super.captureLiveSnapshot()
        }

        override fun getRootInActiveWindow(): AccessibilityNodeInfo? {
            return mockRootNode ?: super.getRootInActiveWindow()
        }

        override fun performGlobalAction(action: Int): Boolean {
            globalActionCallCount++
            lastGlobalActionExecuted = action
            return super.performGlobalAction(action)
        }
    }

    private lateinit var service: TestableAgentAccessibilityService
    private lateinit var globalExecutor: GlobalActionExecutor
    private lateinit var uiExecutor: UiActionExecutor

    @Before
    fun setUp() {
        service = TestableAgentAccessibilityService()
        service.onServiceConnectedForTest()
        service.externalObservationState = ObservationEngineState.OBSERVING
        globalExecutor = GlobalActionExecutor(accessibilityService = service)
        uiExecutor = UiActionExecutor(accessibilityService = service)
    }

    // --- GROUP B: P7-ACTION Contract Execution Tests ---

    @Test
    fun `P7-ACTION-001 - GLOBAL_BACK contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_BACK, result.request.actionType)
        assertEquals(1, service.globalActionCallCount)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, service.lastGlobalActionExecuted)
    }

    @Test
    fun `P7-ACTION-002 - GLOBAL_HOME contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_HOME)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_HOME, result.request.actionType)
        assertEquals(1, service.globalActionCallCount)
        assertEquals(AccessibilityService.GLOBAL_ACTION_HOME, service.lastGlobalActionExecuted)
    }

    @Test
    fun `P7-ACTION-003 - GLOBAL_RECENTS contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_RECENTS)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_RECENTS, result.request.actionType)
        assertEquals(1, service.globalActionCallCount)
        assertEquals(AccessibilityService.GLOBAL_ACTION_RECENTS, service.lastGlobalActionExecuted)
    }

    @Test
    fun `P7-ACTION-004 - UI_CLICK contract execution against live re-acquired target`() {
        val liveBtn = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            viewIdResourceName = "com.app:id/submit"
            text = "Submit"
            isClickable = true
        }

        val obsNode = ObservationNode(
            nodeId = "submit",
            className = "android.widget.Button",
            resourceId = "com.app:id/submit",
            text = "Submit",
            clickable = true,
            nodeIdentity = "id:submit_text:Submit",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        service.mockSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        service.mockRootNode = liveBtn

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "submit"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_CLICK, result.request.actionType)
        assertEquals(0, service.globalActionCallCount)
        assertEquals(1, shadowOf(liveBtn).performedActions.size)
        assertEquals(AccessibilityNodeInfo.ACTION_CLICK, shadowOf(liveBtn).performedActions[0])

        liveBtn.recycle()
    }

    @Test
    fun `P7-ACTION-005 - UI_LONG_CLICK contract execution against live re-acquired target`() {
        val liveCard = AccessibilityNodeInfo.obtain().apply {
            className = "androidx.cardview.widget.CardView"
            viewIdResourceName = "com.app:id/card"
            text = "Card Item"
            isLongClickable = true
        }

        val obsNode = ObservationNode(
            nodeId = "card",
            className = "androidx.cardview.widget.CardView",
            resourceId = "com.app:id/card",
            text = "Card Item",
            longClickable = true,
            nodeIdentity = "id:card_text:Card Item",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        service.mockSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        service.mockRootNode = liveCard

        val request = ActionRequest(
            actionType = ActionType.UI_LONG_CLICK,
            targetNodeId = "card"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_LONG_CLICK, result.request.actionType)
        assertEquals(0, service.globalActionCallCount)
        assertEquals(1, shadowOf(liveCard).performedActions.size)
        assertEquals(AccessibilityNodeInfo.ACTION_LONG_CLICK, shadowOf(liveCard).performedActions[0])

        liveCard.recycle()
    }

    @Test
    fun `P7-ACTION-006 - UI_TEXT_INPUT contract execution against live editable target`() {
        val liveEdit = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.EditText"
            viewIdResourceName = "com.app:id/input_field"
            text = ""
            isEditable = true
        }

        val obsNode = ObservationNode(
            nodeId = "input_field",
            className = "android.widget.EditText",
            resourceId = "com.app:id/input_field",
            editable = true,
            nodeIdentity = "id:input_field_text:",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        service.mockSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        service.mockRootNode = liveEdit

        val request = ActionRequest(
            actionType = ActionType.UI_TEXT_INPUT,
            targetNodeId = "input_field",
            textInputPayload = "Hello Agent"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals("Hello Agent", result.request.textInputPayload)
        assertEquals(0, service.globalActionCallCount)
        assertEquals(1, shadowOf(liveEdit).performedActions.size)
        assertEquals(AccessibilityNodeInfo.ACTION_SET_TEXT, shadowOf(liveEdit).performedActions[0])

        liveEdit.recycle()
    }

    @Test
    fun `P7-ACTION-007 - UI_SCROLL_FORWARD contract execution against live scrollable target`() {
        val liveRecycler = AccessibilityNodeInfo.obtain().apply {
            className = "androidx.recyclerview.widget.RecyclerView"
            viewIdResourceName = "com.app:id/recycler_view"
            isScrollable = true
        }

        val obsNode = ObservationNode(
            nodeId = "recycler_view",
            className = "androidx.recyclerview.widget.RecyclerView",
            resourceId = "com.app:id/recycler_view",
            scrollable = true,
            nodeIdentity = "id:recycler_view_idx:0",
            identityConfidence = NodeIdentityConfidence.HIGH
        )

        service.mockSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        service.mockRootNode = liveRecycler

        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_FORWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_SCROLL_FORWARD, result.request.actionType)
        assertEquals(0, service.globalActionCallCount)
        assertEquals(1, shadowOf(liveRecycler).performedActions.size)
        assertEquals(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, shadowOf(liveRecycler).performedActions[0])

        liveRecycler.recycle()
    }

    @Test
    fun `P7-ACTION-008 - UI_SCROLL_BACKWARD contract execution against live scrollable target`() {
        val liveRecycler = AccessibilityNodeInfo.obtain().apply {
            className = "androidx.recyclerview.widget.RecyclerView"
            viewIdResourceName = "com.app:id/recycler_view"
            isScrollable = true
        }

        val obsNode = ObservationNode(
            nodeId = "recycler_view",
            className = "androidx.recyclerview.widget.RecyclerView",
            resourceId = "com.app:id/recycler_view",
            scrollable = true,
            nodeIdentity = "id:recycler_view_idx:0",
            identityConfidence = NodeIdentityConfidence.HIGH
        )

        service.mockSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        service.mockRootNode = liveRecycler

        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_BACKWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, result.request.actionType)
        assertEquals(0, service.globalActionCallCount)
        assertEquals(1, shadowOf(liveRecycler).performedActions.size)
        assertEquals(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, shadowOf(liveRecycler).performedActions[0])

        liveRecycler.recycle()
    }

    // --- GROUP C: P7-SAFE Zero-Action Safety Invariant Tests ---

    @Test
    fun `P7-SAFE-001 - Stale target yields zero framework action dispatch`() {
        // Target present in pre-snapshot but missing from live root node
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeIdentity = "stale_identity_123"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
        assertEquals(0, service.globalActionCallCount)
    }

    @Test
    fun `P7-SAFE-002 - Missing target yields zero framework action dispatch`() {
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "non_existent_node"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
        assertEquals(0, service.globalActionCallCount)
    }

    @Test
    fun `P7-SAFE-003 - Target not actionable yields zero framework action dispatch`() {
        // Explicitly non-actionable target node in pre-snapshot
        val nonActionableSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "static_label",
                className = "android.widget.TextView",
                text = "Static Label",
                clickable = false,
                longClickable = false,
                scrollable = false,
                editable = false,
                nodeIdentity = "cls:TextView_lbl:Static Label_parent:root"
            )
        )

        service.mockSnapshot = nonActionableSnapshot

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "static_label"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_ACTIONABLE, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_ACTIONABLE, result.verificationResult.resultCode)
        assertEquals(0, service.globalActionCallCount)
    }

    @Test
    fun `P7-SAFE-004 - Invalid target request missing ID and identity yields zero action dispatch`() {
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = null,
            targetNodeIdentity = null
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
        assertEquals(0, service.globalActionCallCount)
    }

    @Test
    fun `P7-SAFE-005 - Global action dispatch is performed ONLY by GlobalActionExecutor and rejected by UiActionExecutor`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)

        // 1. UiActionExecutor must reject global action without dispatching
        val uiResult = uiExecutor.execute(request)

        assertFalse(uiResult.dispatchSuccess)
        assertEquals(VerificationStatus.ACTION_NOT_SUPPORTED, uiResult.verificationResult.status)
        assertEquals(ResultCode.CAPABILITY_UNAVAILABLE, uiResult.verificationResult.resultCode)
        assertEquals(0, service.globalActionCallCount)

        // 2. GlobalActionExecutor must be the ONLY executor that dispatches global actions
        val globalResult = globalExecutor.execute(request)

        assertTrue(globalResult.dispatchSuccess)
        assertEquals(1, service.globalActionCallCount)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, service.lastGlobalActionExecuted)
    }
}
