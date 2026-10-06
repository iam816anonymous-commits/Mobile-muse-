package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
import com.localagent.core.action.VerificationStatus
import com.localagent.core.result.ResultCode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ActionExecutionTest {

    private lateinit var service: AgentAccessibilityService
    private lateinit var globalExecutor: GlobalActionExecutor
    private lateinit var uiExecutor: UiActionExecutor

    @Before
    fun setUp() {
        service = AgentAccessibilityService()
        service.onServiceConnectedForTest()
        service.externalObservationState = ObservationEngineState.OBSERVING
        globalExecutor = GlobalActionExecutor(accessibilityService = service)
        uiExecutor = UiActionExecutor(accessibilityService = service)
    }

    // --- P7-ACTION Contracts ---

    @Test
    fun `P7-ACTION-001 - GLOBAL_BACK contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_BACK, result.request.actionType)
    }

    @Test
    fun `P7-ACTION-002 - GLOBAL_HOME contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_HOME)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_HOME, result.request.actionType)
    }

    @Test
    fun `P7-ACTION-003 - GLOBAL_RECENTS contract execution`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_RECENTS)
        val result = globalExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.GLOBAL_RECENTS, result.request.actionType)
    }

    @Test
    fun `P7-ACTION-004 - UI_CLICK contract execution`() {
        val liveNode = AccessibilityNodeInfo.obtain().apply {
            className = "android.widget.Button"
            viewIdResourceName = "com.app:id/submit"
            text = "Submit"
            isClickable = true
        }

        // Populate live snapshot on service
        service.lastExternalObservationSnapshot = com.localagent.core.observation.ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = com.localagent.core.observation.ObservationNode(
                nodeId = "submit",
                className = "android.widget.Button",
                resourceId = "com.app:id/submit",
                text = "Submit",
                clickable = true,
                nodeIdentity = "id:submit_text:Submit"
            )
        )

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "submit"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertEquals(ActionType.UI_CLICK, result.request.actionType)

        liveNode.recycle()
    }

    @Test
    fun `P7-ACTION-005 - UI_LONG_CLICK contract execution`() {
        val request = ActionRequest(
            actionType = ActionType.UI_LONG_CLICK,
            targetNodeId = "missing"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
    }

    @Test
    fun `P7-ACTION-006 - UI_TEXT_INPUT contract execution`() {
        val request = ActionRequest(
            actionType = ActionType.UI_TEXT_INPUT,
            targetNodeId = "input_field",
            textInputPayload = "Hello Agent"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertEquals("Hello Agent", result.request.textInputPayload)
    }

    @Test
    fun `P7-ACTION-007 - UI_SCROLL_FORWARD contract execution`() {
        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_FORWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertEquals(ActionType.UI_SCROLL_FORWARD, result.request.actionType)
    }

    @Test
    fun `P7-ACTION-008 - UI_SCROLL_BACKWARD contract execution`() {
        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_BACKWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, result.request.actionType)
    }

    // --- P7-SAFE Zero-Action Safety Tests ---

    @Test
    fun `P7-SAFE-001 - Target resolution failure yields zero framework action dispatch`() {
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "non_existent_node"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
    }

    @Test
    fun `P7-SAFE-002 - Target stale or re-acquisition failure yields zero framework action dispatch`() {
        // Target present in pre-snapshot but missing from live root node
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeIdentity = "stale_identity_123"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
    }

    @Test
    fun `P7-SAFE-003 - Target not actionable yields zero framework action dispatch`() {
        // Target is non-clickable TextView without clickable ancestor
        val nonActionableSnapshot = com.localagent.core.observation.ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = com.localagent.core.observation.ObservationNode(
                nodeId = "static_label",
                className = "android.widget.TextView",
                text = "Static Label",
                clickable = false,
                nodeIdentity = "cls:TextView_lbl:Static Label_parent:root"
            )
        )

        // Custom test service returning nonActionableSnapshot for captureLiveSnapshot
        val mockService = object : AgentAccessibilityService() {
            override fun captureLiveSnapshot(): com.localagent.core.observation.ObservationSnapshot {
                return nonActionableSnapshot
            }
        }
        mockService.onServiceConnectedForTest()
        mockService.externalObservationState = ObservationEngineState.OBSERVING

        val executor = UiActionExecutor(accessibilityService = mockService)

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "static_label"
        )

        val result = executor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_ACTIONABLE, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_ACTIONABLE, result.verificationResult.resultCode)
    }

    @Test
    fun `P7-SAFE-004 - Invalid target request missing both ID and Identity yields zero action dispatch`() {
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = null,
            targetNodeIdentity = null
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
    }

    @Test
    fun `P7-SAFE-005 - Global action dispatch is only performed by global action executor`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.ACTION_NOT_SUPPORTED, result.verificationResult.status)
        assertEquals(ResultCode.CAPABILITY_UNAVAILABLE, result.verificationResult.resultCode)
    }
}
