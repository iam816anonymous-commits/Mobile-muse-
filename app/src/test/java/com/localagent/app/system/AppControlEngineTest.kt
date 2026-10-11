package com.localagent.app.system

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class AppControlEngineTest {

    private lateinit var app: LocalAgentApplication
    private lateinit var resolver: AppResolver
    private lateinit var launcher: AppLauncher

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        AgentAccessibilityService.resetForTest(app)

        resolver = AppResolver(app)
        launcher = AppLauncher(app)
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest(app)
    }

    @Test
    fun `PHASE8-RESOLVE-001 - resolve empty query returns PackageNotInstalled`() {
        val result = resolver.resolvePackage("")
        assertTrue(result is AppResolutionResult.PackageNotInstalled)
    }

    @Test
    fun `PHASE8-RESOLVE-002 - resolve unknown package query returns PackageNotInstalled`() {
        val result = resolver.resolvePackage("com.nonexistent.app.xyz")
        assertTrue(result is AppResolutionResult.PackageNotInstalled)
    }

    @Test
    fun `PHASE8-LAUNCH-001 - launch non-existent package returns LaunchFailed`() {
        val result = launcher.launchApp("com.nonexistent.app.xyz")
        assertTrue(result is AppLaunchResult.LaunchFailed)
    }

    @Test
    fun `PHASE8-VERIFY-001 - LaunchVerifier returns AccessibilityUnavailable when service unbound`() {
        val verifier = LaunchVerifier { null }
        val result = verifier.verifyForeground("com.android.calculator2", timeoutMs = 200)
        assertTrue(result is LaunchVerificationResult.AccessibilityUnavailable)
    }

    @Test
    fun `PHASE8-VERIFY-002 - LaunchVerifier detects active foreground package`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val event = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        event.packageName = "com.android.calculator2"
        service.onAccessibilityEvent(event)

        val verifier = LaunchVerifier { service }
        val result = verifier.verifyForeground("com.android.calculator2", timeoutMs = 500)

        assertTrue(result is LaunchVerificationResult.Success)
        assertEquals("com.android.calculator2", (result as LaunchVerificationResult.Success).packageName)
    }

    @Test
    fun `PHASE8-VERIFY-003 - LaunchVerifier returns Timeout when package does not match`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val verifier = LaunchVerifier { service }
        val result = verifier.verifyForeground("com.android.settings", timeoutMs = 200)

        assertTrue(result is LaunchVerificationResult.Timeout)
    }

    @Test
    fun `PHASE8-VERIFY-004 - LaunchVerifier ignores transient SystemUI event when target package matches last external`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        // Calculator set as last external
        service.lastExternalObservationSnapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1
        )

        // SystemUI emits a transient event
        val sysUiEvent = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        sysUiEvent.packageName = "com.android.systemui"
        service.onAccessibilityEvent(sysUiEvent)

        val verifier = LaunchVerifier { service }
        val result = verifier.verifyForeground("com.android.calculator2", timeoutMs = 300)

        assertTrue(result is LaunchVerificationResult.Success)
        assertEquals("com.android.calculator2", (result as LaunchVerificationResult.Success).packageName)
    }

    @Test
    fun `PHASE8-PIPELINE-001 - UiActionExecutor executes UI_CLICK when foreground package matches`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val (computedIdentity, confidence) = ObservationNode.computeIdentity(
            packageName = "com.android.calculator2",
            className = "android.widget.Button",
            resourceId = "com.android.calculator2:id/digit_7",
            text = "7",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )

        val extSnapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                resourceId = "com.android.calculator2:id/digit_7",
                text = "7",
                clickable = true,
                nodeIdentity = computedIdentity,
                identityConfidence = confidence
            )
        )
        service.lastExternalObservationSnapshot = extSnapshot

        val event = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        event.packageName = "com.android.calculator2"
        service.onAccessibilityEvent(event)

        val uiExecutor = com.localagent.app.accessibility.UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                com.localagent.app.accessibility.AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.android.calculator2",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.android.calculator2:id/digit_7",
                    text = "7"
                )
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val request = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "digit_7")
        val result = uiExecutor.execute(request)

        assertTrue(result.dispatchSuccess)
        assertEquals("com.android.calculator2", result.preSnapshot?.packageName)
    }

    @Test
    fun `PHASE8-E2E-001 - Sequential APP_LAUNCH then UI_CLICK command execution`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        // 1. Launch verification
        val event = android.view.accessibility.AccessibilityEvent.obtain(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
        event.packageName = "com.android.calculator2"
        service.onAccessibilityEvent(event)

        val verifier = LaunchVerifier { service }
        val launchVerifyRes = verifier.verifyForeground("com.android.calculator2", timeoutMs = 300)
        assertTrue(launchVerifyRes is LaunchVerificationResult.Success)

        // 2. Fresh snapshot observation setup
        val (computedIdentity, confidence) = ObservationNode.computeIdentity(
            packageName = "com.android.calculator2",
            className = "android.widget.Button",
            resourceId = "com.android.calculator2:id/digit_7",
            text = "7",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )

        val freshSnap = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = ObservationNode(
                nodeId = "digit_7",
                className = "android.widget.Button",
                resourceId = "com.android.calculator2:id/digit_7",
                text = "7",
                clickable = true,
                nodeIdentity = computedIdentity,
                identityConfidence = confidence
            )
        )
        service.lastExternalObservationSnapshot = freshSnap

        // 3. UI Click dispatch
        val uiExecutor = com.localagent.app.accessibility.UiActionExecutor(
            accessibilityService = service,
            liveRootNodeProvider = {
                com.localagent.app.accessibility.AccessibilityTestFixtures.createClickableNode(
                    packageName = "com.android.calculator2",
                    className = "android.widget.Button",
                    viewIdResourceName = "com.android.calculator2:id/digit_7",
                    text = "7"
                )
            },
            snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: ObservationSnapshot() }
        )

        val clickReq = ActionRequest(actionType = ActionType.UI_CLICK, targetNodeId = "digit_7")
        val clickRes = uiExecutor.execute(clickReq)

        assertTrue(clickRes.dispatchSuccess)
        assertEquals("com.android.calculator2", clickRes.preSnapshot?.packageName)
    }
}
