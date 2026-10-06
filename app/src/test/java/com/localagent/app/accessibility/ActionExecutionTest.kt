package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.app.LocalAgentApplication
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
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAccessibilityService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class ActionExecutionTest {

    private lateinit var service: AgentAccessibilityService
    private lateinit var shadowService: ShadowAccessibilityService
    private lateinit var globalExecutor: GlobalActionExecutor
    private var liveNodeSupplier: (() -> AccessibilityNodeInfo?)? = null

    @Before
    fun setUp() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        service = serviceController.get()
        service.onServiceConnectedForTest()
        service.externalObservationState = ObservationEngineState.OBSERVING
        shadowService = shadowOf(service as AccessibilityService)
        globalExecutor = GlobalActionExecutor(accessibilityService = service)
        liveNodeSupplier = null
    }

    private fun createUiExecutor(): UiActionExecutor {
        return UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = { liveNodeSupplier?.invoke() },
            snapshotProvider = { service.currentObservationSnapshot ?: ObservationSnapshot() }
        )
    }

    // --- GROUP B: P7-ACTION Contract Execution Tests ---

    @Test
    fun `P7-ACTION-001 - GLOBAL_BACK contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_BACK, result.request.actionType)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `P7-ACTION-002 - GLOBAL_HOME contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_HOME)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_HOME, result.request.actionType)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_HOME, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `P7-ACTION-003 - GLOBAL_RECENTS contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_RECENTS)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_RECENTS, result.request.actionType)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_RECENTS, shadowService.globalActionsPerformed[0])
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

        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        liveNodeSupplier = { liveBtn }

        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "submit"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_CLICK, result.request.actionType)
        assertEquals(0, shadowService.globalActionsPerformed.size)
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

        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        liveNodeSupplier = { liveCard }

        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_LONG_CLICK,
            targetNodeId = "card"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_LONG_CLICK, result.request.actionType)
        assertEquals(0, shadowService.globalActionsPerformed.size)
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

        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        liveNodeSupplier = { liveEdit }

        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_TEXT_INPUT,
            targetNodeId = "input_field",
            textInputPayload = "Hello Agent"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals("Hello Agent", result.request.textInputPayload)
        assertEquals(0, shadowService.globalActionsPerformed.size)
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

        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        liveNodeSupplier = { liveRecycler }

        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_FORWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_SCROLL_FORWARD, result.request.actionType)
        assertEquals(0, shadowService.globalActionsPerformed.size)
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

        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )
        liveNodeSupplier = { liveRecycler }

        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_BACKWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, result.request.actionType)
        assertEquals(0, shadowService.globalActionsPerformed.size)
        assertEquals(1, shadowOf(liveRecycler).performedActions.size)
        assertEquals(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, shadowOf(liveRecycler).performedActions[0])

        liveRecycler.recycle()
    }

    // --- GROUP C: P7-SAFE Zero-Action Safety Invariant Tests ---

    @Test
    fun `P7-SAFE-001 - Stale target yields zero framework action dispatch`() {
        // Target present in pre-snapshot but missing from live root node
        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeIdentity = "stale_identity_123"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-SAFE-002 - Missing target yields zero framework action dispatch`() {
        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "non_existent_node"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-SAFE-003 - Target not actionable yields zero framework action dispatch`() {
        // Explicitly construct non-actionable target node in pre-snapshot
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

        service.currentObservationSnapshot = nonActionableSnapshot
        val uiExecutor = createUiExecutor()

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "static_label"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_ACTIONABLE, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_ACTIONABLE, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-SAFE-004 - Invalid target request missing ID and identity yields zero action dispatch`() {
        val uiExecutor = createUiExecutor()
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = null,
            targetNodeIdentity = null
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-SAFE-005 - Global action dispatch is performed ONLY by GlobalActionExecutor and rejected by UiActionExecutor`() {
        val uiExecutor = createUiExecutor()
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)

        // 1. UiActionExecutor must reject global action without dispatching
        val uiResult = uiExecutor.execute(request)

        assertFalse(uiResult.dispatchSuccess)
        assertEquals(VerificationStatus.ACTION_NOT_SUPPORTED, uiResult.verificationResult.status)
        assertEquals(ResultCode.CAPABILITY_UNAVAILABLE, uiResult.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)

        // 2. GlobalActionExecutor must be the ONLY executor that dispatches global actions
        val globalResult = globalExecutor.execute(request)

        assertTrue(globalResult.dispatchSuccess)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, shadowService.globalActionsPerformed[0])
    }
}
