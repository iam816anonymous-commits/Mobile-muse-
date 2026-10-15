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

    @Before
    fun setUp() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        service = serviceController.get()
        service.onServiceConnectedForTest()
        service.externalObservationState = ObservationEngineState.OBSERVING
        shadowService = shadowOf(service as AccessibilityService)
        globalExecutor = GlobalActionExecutor(accessibilityService = service)
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
    fun `P7-ACTION-UNCHANGED-TREE-EVALUATES-TO-DISPATCHED-BUT-NOT-VERIFIED - Action on unchanged tree returns DISPATCHED_BUT_NOT_VERIFIED`() {
        val calcButton = ObservationNode(
            nodeId = "digit7",
            className = "android.widget.Button",
            resourceId = "com.android.calculator2:id/digit_7",
            text = "7",
            clickable = true,
            nodeIdentity = "id:digit_7_text:7",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = calcButton,
            generation = 29L
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = calcButton,
            generation = 30L
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.android.calculator2",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.android.calculator2:id/digit_7",
                    text = "7"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "digit7"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(VerificationStatus.EXECUTED_BUT_NOT_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.DISPATCHED_BUT_NOT_VERIFIED, result.verificationResult.resultCode)
        assertTrue(result.verificationResult.reason?.contains("no observable accessibility state change occurred") == true)
    }

    @Test
    fun `P7-ACTION-MULTI-STEP-FRESHNESS - Multi-step execution guarantees post-action capture occurs post-dispatch with fresh generation`() {
        val launcherNode = ObservationNode(
            nodeId = "camera_icon",
            className = "android.widget.TextView",
            resourceId = "com.transsion.hilauncher:id/camera_icon",
            text = "Camera",
            clickable = true,
            nodeIdentity = "id:camera_icon_text:Camera",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.transsion.hilauncher",
            nodeCount = 1,
            rootNode = launcherNode,
            generation = 100L,
            timestamp = 1000L
        )

        val cameraNode = ObservationNode(
            nodeId = "shutter",
            className = "android.widget.ImageView",
            resourceId = "com.transsion.camera:id/shutter",
            clickable = true,
            nodeIdentity = "id:shutter",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.transsion.camera",
            nodeCount = 1,
            rootNode = cameraNode,
            generation = 101L,
            timestamp = 1500L
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.transsion.hilauncher",
                    className = "android.widget.TextView",
                    viewIdResourceName = "com.transsion.hilauncher:id/camera_icon",
                    text = "Camera"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        service.lastExternalObservationSnapshot = preSnap
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "camera_icon"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(100L, result.preSnapshot?.generation)
        assertEquals(101L, result.postSnapshot?.generation)
        assertTrue(result.postSnapshot!!.generation > result.preSnapshot!!.generation)
        assertEquals("com.transsion.hilauncher", result.preSnapshot?.packageName)
        assertEquals("com.transsion.camera", result.postSnapshot?.packageName)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
    }

    @Test
    fun `P7-ACTION-004-PACKAGE-TRANSITION - UI_CLICK across package boundary records distinct pre and post snapshots`() {
        val launcherNode = ObservationNode(
            nodeId = "camera_icon",
            className = "android.widget.TextView",
            resourceId = "com.transsion.hilauncher:id/camera_icon",
            text = "Camera",
            clickable = true,
            nodeIdentity = "id:camera_icon_text:Camera",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val preLauncherSnap = ObservationSnapshot(
            packageName = "com.transsion.hilauncher",
            nodeCount = 1,
            rootNode = launcherNode,
            generation = 10L
        )

        val cameraNode = ObservationNode(
            nodeId = "shutter_button",
            className = "android.widget.ImageView",
            resourceId = "com.transsion.camera:id/shutter",
            clickable = true,
            nodeIdentity = "id:shutter",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val postCameraSnap = ObservationSnapshot(
            packageName = "com.transsion.camera",
            nodeCount = 1,
            rootNode = cameraNode,
            generation = 11L
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.transsion.hilauncher",
                    className = "android.widget.TextView",
                    viewIdResourceName = "com.transsion.hilauncher:id/camera_icon",
                    text = "Camera"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preLauncherSnap else postCameraSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "camera_icon"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_CLICK, result.request.actionType)
        assertEquals("com.transsion.hilauncher", result.preSnapshot?.packageName)
        assertEquals("com.transsion.camera", result.postSnapshot?.packageName)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertNotNull(service.lastActionExecutionResult)
        assertEquals(result.executionId, service.lastActionExecutionResult?.executionId)
    }

    @Test
    fun `P7-ACTION-004 - UI_CLICK contract execution against live re-acquired target`() {
        val obsNode = ObservationNode(
            nodeId = "submit",
            className = "android.widget.Button",
            resourceId = "com.app:id/submit",
            text = "Submit",
            clickable = true,
            checked = false,
            nodeIdentity = "id:submit_text:Submit",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode.copy(checked = true)
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.app",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.app:id/submit",
                    text = "Submit"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "submit"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_CLICK, result.request.actionType)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-ACTION-005 - UI_LONG_CLICK contract execution against live re-acquired target`() {
        val obsNode = ObservationNode(
            nodeId = "card",
            className = "androidx.cardview.widget.CardView",
            resourceId = "com.app:id/card",
            text = "Card Item",
            longClickable = true,
            selected = false,
            nodeIdentity = "id:card_text:Card Item",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode.copy(selected = true)
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createLongClickableNode(
                    packageName = "com.app",
                    className = "androidx.cardview.widget.CardView",
                    viewIdResourceName = "com.app:id/card",
                    text = "Card Item"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_LONG_CLICK,
            targetNodeId = "card"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_LONG_CLICK, result.request.actionType)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-ACTION-006 - UI_TEXT_INPUT contract execution against live editable target`() {
        val (computedIdentity, confidence) = ObservationNode.computeIdentity(
            packageName = "com.app",
            className = "android.widget.EditText",
            resourceId = "com.app:id/input_field",
            text = "",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )

        val obsNode = ObservationNode(
            nodeId = "input_field",
            className = "android.widget.EditText",
            resourceId = "com.app:id/input_field",
            text = "",
            editable = true,
            nodeIdentity = computedIdentity,
            identityConfidence = confidence
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode.copy(text = "Hello Agent")
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createEditableNode(
                    packageName = "com.app",
                    className = "android.widget.EditText",
                    viewIdResourceName = "com.app:id/input_field",
                    text = ""
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_TEXT_INPUT,
            targetNodeId = "input_field",
            textInputPayload = "Hello Agent"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals("Hello Agent", result.request.textInputPayload)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-ACTION-007 - UI_SCROLL_FORWARD contract execution against live scrollable target`() {
        val obsNode = ObservationNode(
            nodeId = "recycler_view",
            className = "androidx.recyclerview.widget.RecyclerView",
            resourceId = "com.app:id/recycler_view",
            text = "Item 0",
            scrollable = true,
            nodeIdentity = "id:recycler_view_idx:0",
            identityConfidence = NodeIdentityConfidence.HIGH
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode.copy(text = "Item 10")
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createScrollableNode(
                    packageName = "com.app",
                    className = "androidx.recyclerview.widget.RecyclerView",
                    viewIdResourceName = "com.app:id/recycler_view",
                    text = "Item 0"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_FORWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_SCROLL_FORWARD, result.request.actionType)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-ACTION-008 - UI_SCROLL_BACKWARD contract execution against live scrollable target`() {
        val obsNode = ObservationNode(
            nodeId = "recycler_view",
            className = "androidx.recyclerview.widget.RecyclerView",
            resourceId = "com.app:id/recycler_view",
            text = "Item 10",
            scrollable = true,
            nodeIdentity = "id:recycler_view_idx:0",
            identityConfidence = NodeIdentityConfidence.HIGH
        )

        val preSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = obsNode.copy(text = "Item 0")
        )

        var snapshotCallCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createScrollableNode(
                    packageName = "com.app",
                    className = "androidx.recyclerview.widget.RecyclerView",
                    viewIdResourceName = "com.app:id/recycler_view",
                    text = "Item 10"
                )
            },
            snapshotProvider = {
                snapshotCallCount++
                if (snapshotCallCount == 1) preSnap else postSnap
            }
        )

        val request = ActionRequest(
            actionType = ActionType.UI_SCROLL_BACKWARD,
            targetNodeId = "recycler_view"
        )

        val result = uiExecutor.execute(request)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, result.request.actionType)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    // --- GROUP C: P7-SAFE Zero-Action Safety Invariant Tests ---

    @Test
    fun `P7-SAFE-000-BLOCKING-OVERLAY - Blocking system overlay window prevents click dispatch`() {
        val launcherNode = ObservationNode(
            nodeId = "camera_icon",
            className = "android.widget.TextView",
            resourceId = "com.transsion.hilauncher:id/camera_icon",
            text = "Camera",
            clickable = true,
            nodeIdentity = "id:camera_icon_text:Camera"
        )
        val snap = ObservationSnapshot(
            packageName = "com.transsion.hilauncher",
            nodeCount = 1,
            rootNode = launcherNode
        )

        val windowDiagnostics = ObservationWindowDiagnostics(
            availableWindows = listOf(
                WindowCandidateInfo(
                    windowId = 99,
                    windowType = android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM,
                    windowTypeName = "TYPE_SYSTEM",
                    packageName = "com.android.systemui",
                    isActive = true,
                    isFocused = true,
                    score = 90
                )
            )
        )
        service.latestDiagnostics = windowDiagnostics

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = { null },
            snapshotProvider = { snap }
        )
        val request = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "camera_icon"
        )

        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.EXECUTION_FAILED, result.verificationResult.status)
        assertTrue(result.verificationResult.reason?.contains("blocking system dialog or overlay") == true)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `P7-SAFE-001 - Stale target yields zero framework action dispatch`() {
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = { null },
            snapshotProvider = { service.currentObservationSnapshot ?: ObservationSnapshot() }
        )
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
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = { null },
            snapshotProvider = { service.currentObservationSnapshot ?: ObservationSnapshot() }
        )
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

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    packageName = "com.app"
                    className = "android.widget.TextView"
                    text = "Static Label"
                    isClickable = false
                    isLongClickable = false
                    isScrollable = false
                }
            },
            snapshotProvider = { nonActionableSnapshot }
        )

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
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = { null },
            snapshotProvider = { service.currentObservationSnapshot ?: ObservationSnapshot() }
        )
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
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = { null },
            snapshotProvider = { service.currentObservationSnapshot ?: ObservationSnapshot() }
        )
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)

        val uiResult = uiExecutor.execute(request)

        assertFalse(uiResult.dispatchSuccess)
        assertEquals(VerificationStatus.ACTION_NOT_SUPPORTED, uiResult.verificationResult.status)
        assertEquals(ResultCode.CAPABILITY_UNAVAILABLE, uiResult.verificationResult.resultCode)
        assertEquals(0, shadowService.globalActionsPerformed.size)

        val globalResult = globalExecutor.execute(request)

        assertTrue(globalResult.dispatchSuccess)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, shadowService.globalActionsPerformed[0])
    }

    // --- GROUP D: Regression & Boundary Lifecycle Tests ---

    @Test
    fun `REGRESSION-001 - Same app action with valid fresh post snapshot verifies state change`() {
        val calcButtonPre = ObservationNode(
            nodeId = "digit7",
            className = "android.widget.Button",
            resourceId = "com.android.calculator2:id/digit_7",
            text = "7",
            clickable = true,
            nodeIdentity = "id:digit_7"
        )
        val formulaPre = ObservationNode(
            nodeId = "formula",
            className = "android.widget.TextView",
            resourceId = "com.android.calculator2:id/formula",
            text = "",
            nodeIdentity = "id:formula"
        )
        val formulaPost = formulaPre.copy(text = "7")

        val preSnap = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 2,
            rootNode = ObservationNode(
                nodeId = "root",
                className = "android.widget.LinearLayout",
                children = listOf(calcButtonPre, formulaPre)
            ),
            generation = 10L
        )

        val postSnap = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 2,
            rootNode = ObservationNode(
                nodeId = "root",
                className = "android.widget.LinearLayout",
                children = listOf(calcButtonPre, formulaPost)
            ),
            generation = 11L
        )

        var snapCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.android.calculator2",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.android.calculator2:id/digit_7",
                    text = "7"
                )
            },
            snapshotProvider = {
                snapCount++
                if (snapCount == 1) preSnap else postSnap
            }
        )

        val req = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "digit7"
        )

        val result = uiExecutor.execute(req)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertNotNull(result.postSnapshot)
        assertEquals("com.android.calculator2", result.postSnapshot?.packageName)
        assertEquals(11L, result.postSnapshot?.generation)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
    }

    @Test
    fun `REGRESSION-002 - Same app action where post snapshot capture returns null retains null postSnapshot and FailureStage`() {
        val calcButton = ObservationNode(
            nodeId = "digit7",
            className = "android.widget.Button",
            resourceId = "com.android.calculator2:id/digit_7",
            text = "7",
            clickable = true,
            nodeIdentity = "id:digit_7"
        )
        val preSnap = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = calcButton,
            generation = 10L
        )

        var snapCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.android.calculator2",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.android.calculator2:id/digit_7",
                    text = "7"
                )
            },
            snapshotProvider = {
                snapCount++
                if (snapCount == 1) preSnap else null
            }
        )

        val req = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "digit7"
        )

        val result = uiExecutor.execute(req)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertNull(result.postSnapshot)
        assertEquals(VerificationStatus.VERIFICATION_FAILED, result.verificationResult.status)
        assertEquals(com.localagent.core.action.FailureStage.POST_ACTION_CAPTURE, result.verificationResult.failureStage)
        assertTrue(result.verificationResult.reason?.contains("Post-action snapshot capture failed") == true)
    }

    @Test
    fun `REGRESSION-003 - Launcher click to camera destination captures camera postSnapshot without waiting for hilauncher`() {
        val launcherNode = ObservationNode(
            nodeId = "camera_icon",
            className = "android.widget.TextView",
            resourceId = "com.transsion.hilauncher:id/camera_icon",
            text = "Camera",
            clickable = true,
            nodeIdentity = "id:camera_icon"
        )
        val preLauncherSnap = ObservationSnapshot(
            packageName = "com.transsion.hilauncher",
            nodeCount = 1,
            rootNode = launcherNode,
            generation = 20L
        )

        val cameraShutterNode = ObservationNode(
            nodeId = "shutter",
            className = "android.widget.ImageView",
            resourceId = "com.mediatek.camera:id/shutter",
            clickable = true,
            nodeIdentity = "id:shutter"
        )
        val postCameraSnap = ObservationSnapshot(
            packageName = "com.mediatek.camera",
            nodeCount = 1,
            rootNode = cameraShutterNode,
            generation = 21L
        )

        var snapCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.transsion.hilauncher",
                    className = "android.widget.TextView",
                    viewIdResourceName = "com.transsion.hilauncher:id/camera_icon",
                    text = "Camera"
                )
            },
            snapshotProvider = {
                snapCount++
                if (snapCount == 1) preLauncherSnap else postCameraSnap
            }
        )

        val req = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "camera_icon"
        )

        val result = uiExecutor.execute(req)

        assertNotNull(result)
        assertTrue(result.dispatchSuccess)
        assertEquals("com.transsion.hilauncher", result.preSnapshot?.packageName)
        assertEquals("com.mediatek.camera", result.postSnapshot?.packageName)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
    }

    @Test
    fun `REGRESSION-004 - Action result records exact pre and post snapshot association in service execution history`() {
        val btnNode = ObservationNode(
            nodeId = "btn",
            className = "android.widget.Button",
            resourceId = "com.app:id/btn",
            text = "Click",
            clickable = true,
            nodeIdentity = "id:btn"
        )
        val preSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = btnNode,
            generation = 50L
        )
        val postSnap = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = btnNode.copy(text = "Clicked"),
            generation = 51L
        )

        var snapCount = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.app",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.app:id/btn",
                    text = "Click"
                )
            },
            snapshotProvider = {
                snapCount++
                if (snapCount == 1) preSnap else postSnap
            }
        )

        val req = ActionRequest(
            actionType = ActionType.UI_CLICK,
            targetNodeId = "btn"
        )

        val result = uiExecutor.execute(req)

        assertNotNull(result)
        val history = service.actionExecutionHistory
        assertTrue(history.isNotEmpty())
        val recorded = history.last()
        assertEquals(result.executionId, recorded.executionId)
        assertEquals(50L, recorded.preSnapshot?.generation)
        assertEquals(51L, recorded.postSnapshot?.generation)
        assertEquals("com.app", recorded.preSnapshot?.packageName)
        assertEquals("com.app", recorded.postSnapshot?.packageName)
    }
}
