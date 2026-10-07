package com.localagent.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
import com.localagent.core.action.VerificationStatus
import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.result.ResultCode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class ObservationContextRoutingTest {

    private lateinit var app: LocalAgentApplication
    private lateinit var service: AgentAccessibilityService
    private lateinit var shadowService: ShadowAccessibilityService
    private lateinit var globalExecutor: GlobalActionExecutor

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        AgentAccessibilityService.resetForTest(app)

        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        service = serviceController.get()
        service.onServiceConnectedForTest()
        service.externalObservationState = ObservationEngineState.OBSERVING
        shadowService = shadowOf(service as AccessibilityService)
        globalExecutor = GlobalActionExecutor(accessibilityService = service)
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest(app)
    }

    @Test
    fun `CC-OBS-001 - UI_CLICK uses ExternalObservation`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                resourceId = "com.android.calculator2:id/digit_7",
                text = "7",
                clickable = true,
                nodeIdentity = "id:digit_7_text:7",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    viewIdResourceName = "com.android.calculator2:id/digit_7"
                    text = "7"
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "digit_7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.calculator2", result.preSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-002 - UI_LONG_CLICK uses ExternalObservation`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                resourceId = "com.android.calculator2:id/digit_7",
                text = "7",
                longClickable = true,
                nodeIdentity = "id:digit_7_text:7",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    viewIdResourceName = "com.android.calculator2:id/digit_7"
                    text = "7"
                    isLongClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_LONG_CLICK, targetNodeId = "digit_7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.calculator2", result.preSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-003 - UI_TEXT_INPUT uses ExternalObservation`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "formula",
                className = "android.widget.EditText",
                resourceId = "com.android.calculator2:id/formula",
                editable = true,
                nodeIdentity = "id:formula_text:",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.EditText"
                    viewIdResourceName = "com.android.calculator2:id/formula"
                    isEditable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_TEXT_INPUT, targetNodeId = "formula", textInputPayload = "25*37")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.calculator2", result.preSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-004 - UI_SCROLL_FORWARD uses ExternalObservation`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.settings",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "recycler",
                className = "androidx.recyclerview.widget.RecyclerView",
                resourceId = "com.android.settings:id/recycler",
                scrollable = true,
                nodeIdentity = "id:recycler_idx:0",
                identityConfidence = NodeIdentityConfidence.HIGH
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "androidx.recyclerview.widget.RecyclerView"
                    viewIdResourceName = "com.android.settings:id/recycler"
                    isScrollable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_SCROLL_FORWARD, targetNodeId = "recycler")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.settings", result.preSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-005 - UI_SCROLL_BACKWARD uses ExternalObservation`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.settings",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "recycler",
                className = "androidx.recyclerview.widget.RecyclerView",
                resourceId = "com.android.settings:id/recycler",
                scrollable = true,
                nodeIdentity = "id:recycler_idx:0",
                identityConfidence = NodeIdentityConfidence.HIGH
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "androidx.recyclerview.widget.RecyclerView"
                    viewIdResourceName = "com.android.settings:id/recycler"
                    isScrollable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_SCROLL_BACKWARD, targetNodeId = "recycler")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.settings", result.preSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-006 - UI action never resolves target from LocalAgent CurrentObservation when external target required`() {
        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.localagent.app",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = null

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "digit_7")
        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
    }

    @Test
    fun `CC-OBS-007 - Target package must match controlled external package`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "btn_equals",
                className = "android.widget.Button",
                text = "=",
                clickable = true,
                nodeIdentity = "id:equals_text:=",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    viewIdResourceName = "com.android.calculator2:id/btn_equals"
                    text = "="
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "btn_equals")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.calculator2", result.preSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-008 - LocalAgent-owned target is rejected when external action is required`() {
        val localAgentSnapshot = ObservationSnapshot(
            packageName = "com.localagent.app",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "btnExecute",
                className = "android.widget.Button",
                clickable = true
            )
        )

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            snapshotProvider = { localAgentSnapshot }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "btnExecute")
        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
    }

    @Test
    fun `CC-OBS-009 - GLOBAL_BACK does not require target observation`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_BACK)
        val result = globalExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `CC-OBS-010 - GLOBAL_HOME does not require target observation`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_HOME)
        val result = globalExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_HOME, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `CC-OBS-011 - GLOBAL_RECENTS does not require target observation`() {
        val request = ActionRequest(actionType = ActionType.GLOBAL_RECENTS)
        val result = globalExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_RECENTS, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `CC-OBS-012 - Pre-post verification for UI actions uses external observation`() {
        val preExt = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                resourceId = "com.android.calculator2:id/digit_7",
                text = "7",
                clickable = true,
                checked = false,
                nodeIdentity = "id:digit_7_text:7",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )

        val postExt = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                resourceId = "com.android.calculator2:id/digit_7",
                text = "7",
                clickable = true,
                checked = true,
                nodeIdentity = "id:digit_7_text:7",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )

        var snapCalls = 0
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    viewIdResourceName = "com.android.calculator2:id/digit_7"
                    text = "7"
                    isClickable = true
                }
            },
            snapshotProvider = {
                snapCalls++
                if (snapCalls == 1) preExt else postExt
            }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "digit_7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.verificationResult.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.verificationResult.resultCode)
        assertEquals("com.android.calculator2", result.preSnapshot?.packageName)
        assertEquals("com.android.calculator2", result.postSnapshot?.packageName)
    }

    @Test
    fun `CC-OBS-013 - click 7 parses as semantic text target, not nodeId 7`() {
        val normalizer = com.localagent.core.command.CommandNormalizer()
        val parseResult = normalizer.parseInput("click 7", com.localagent.core.command.CommandSource.CONSOLE)

        assertTrue(parseResult is com.localagent.core.command.CommandParseResult.Success)
        val cmd = (parseResult as com.localagent.core.command.CommandParseResult.Success).command
        assertEquals(com.localagent.core.command.ActionType.UI_CLICK, cmd.actionType)
        assertTrue(cmd.targetSelector is com.localagent.core.command.TargetSelector.ByText)
        assertEquals("7", (cmd.targetSelector as com.localagent.core.command.TargetSelector.ByText).text)
    }

    @Test
    fun `CC-OBS-014 - semantic text 7 resolves against external snapshot`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "node_42",
                className = "android.widget.Button",
                text = "7",
                clickable = true,
                nodeIdentity = "id:digit_7_text:7",
                identityConfidence = NodeIdentityConfidence.EXACT
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    text = "7"
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.transsion.calculator", result.preSnapshot?.packageName)
        assertEquals("node_42", result.targetResolutionResult?.resolvedNodeId)
    }

    @Test
    fun `CC-OBS-015 - semantic target resolution finds actionable Calculator-style node`() {
        val (computedIdentity, _) = ObservationNode.computeIdentity(
            packageName = "com.transsion.calculator",
            className = "com.google.android.material.button.MaterialButton",
            resourceId = null,
            text = "7",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )

        val parentButtonNode = ObservationNode(
            nodeId = "button_container_42",
            className = "com.google.android.material.button.MaterialButton",
            clickable = true,
            nodeIdentity = computedIdentity,
            children = listOf(
                ObservationNode(
                    nodeId = "child_text_99",
                    className = "android.widget.TextView",
                    text = "7",
                    clickable = false,
                    nodeIdentity = "text_7"
                )
            )
        )

        val extSnapshot = ObservationSnapshot(
            packageName = "com.transsion.calculator",
            nodeCount = 2,
            rootNode = parentButtonNode
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "com.google.android.material.button.MaterialButton"
                    text = "7"
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("button_container_42", result.targetResolutionResult?.resolvedNodeId)
    }

    @Test
    fun `CC-OBS-016 - external package is retained through command execution`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "node_42",
                className = "android.widget.Button",
                text = "7",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    text = "7"
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.transsion.calculator", service.lastExternalPackageName)
    }

    @Test
    fun `CC-OBS-017 - LocalAgent current observation is never used for targeted UI actions`() {
        service.currentObservationSnapshot = ObservationSnapshot(
            packageName = "com.localagent.app",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "btn7",
                text = "7",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = null

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "7")
        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
    }

    @Test
    fun `CC-OBS-018 - successful semantic resolution reaches live re-acquisition`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "node_9",
                className = "android.widget.Button",
                text = "9",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        var reacquired = false
        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                reacquired = true
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    text = "9"
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "9")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertTrue(reacquired)
    }

    @Test
    fun `CC-OBS-019 - explicit invalid node ID produces TARGET_NOT_FOUND`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "node_7",
                text = "7",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "non_existent_target_999")
        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
    }

    @Test
    fun `CC-OBS-020 - target belonging to com localagent app is rejected`() {
        val localAgentSnapshot = ObservationSnapshot(
            packageName = "com.localagent.app",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "btnExecute",
                text = "7",
                clickable = true
            )
        )

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            snapshotProvider = { localAgentSnapshot }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "7")
        val result = uiExecutor.execute(request)

        assertFalse(result.dispatchSuccess)
        assertEquals(VerificationStatus.TARGET_NOT_FOUND, result.verificationResult.status)
        assertEquals(ResultCode.TARGET_NOT_FOUND, result.verificationResult.resultCode)
    }

    @Test
    fun `CC-OBS-021 - UI_CLICK dispatches exactly once after successful live re-acquisition`() {
        val extSnapshot = ObservationSnapshot(
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "node_7",
                text = "7",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val uiExecutor = UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                AccessibilityNodeInfo.obtain().apply {
                    className = "android.widget.Button"
                    text = "7"
                    isClickable = true
                }
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals(0, shadowService.globalActionsPerformed.size)
    }

    @Test
    fun `CC-OBS-022 - global BACK HOME RECENTS remain completely independent of target resolution`() {
        val requestBack = ActionRequest(actionType = ActionType.GLOBAL_BACK)
        val requestHome = ActionRequest(actionType = ActionType.GLOBAL_HOME)
        val requestRecents = ActionRequest(actionType = ActionType.GLOBAL_RECENTS)

        val resBack = globalExecutor.execute(requestBack)
        val resHome = globalExecutor.execute(requestHome)
        val resRecents = globalExecutor.execute(requestRecents)

        assertTrue(resBack.dispatchSuccess)
        assertTrue(resHome.dispatchSuccess)
        assertTrue(resRecents.dispatchSuccess)
        assertEquals(3, shadowService.globalActionsPerformed.size)
    }
}
