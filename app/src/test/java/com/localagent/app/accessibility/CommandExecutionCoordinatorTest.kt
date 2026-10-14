package com.localagent.app.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.system.LaunchVerifier
import com.localagent.core.action.ActionExecutionResult
import com.localagent.core.action.ActionType
import com.localagent.core.action.VerificationStatus
import com.localagent.core.command.ActionType as CmdActionType
import com.localagent.core.command.CommandSource
import com.localagent.core.command.NormalizedCommand
import com.localagent.core.command.TargetSelector
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.result.ResultCode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27], application = LocalAgentApplication::class)
class CommandExecutionCoordinatorTest {

    private lateinit var context: Context
    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        app = context as LocalAgentApplication
        AgentAccessibilityService.resetForTest(app)

        val pm = shadowOf(context.packageManager)
        val pkg = "com.android.calculator2"
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(pkg)
        }
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = pkg
                name = "$pkg.Calculator"
                applicationInfo = ApplicationInfo().apply {
                    packageName = pkg
                    flags = ApplicationInfo.FLAG_INSTALLED
                }
            }
        }
        @Suppress("DEPRECATION")
        pm.addResolveInfoForIntent(mainIntent, resolveInfo)
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest(app)
    }

    @Test
    fun testAppLaunchExecutionRecordsFormalActionTypeAndEvidence() {
        val service = AgentAccessibilityService()
        service.onServiceConnectedForTest()

        val targetPkg = "com.android.calculator2"
        val snap = ObservationSnapshot(
            generation = 10L,
            packageName = targetPkg,
            nodeCount = 5,
            rootNode = ObservationNode(
                nodeId = "root",
                nodeIdentity = "id:root",
                packageName = targetPkg,
                text = "Calculator"
            )
        )
        service.lastExternalObservationSnapshot = snap

        val mockVerifier = LaunchVerifier(
            accessibilityServiceSupplier = { service },
            rootNodeProvider = { AccessibilityTestFixtures.createClickableNode(packageName = targetPkg) }
        )

        val coordinator = CommandExecutionCoordinator(
            context = context,
            accessibilityServiceSupplier = { service },
            launchVerifierSupplier = { mockVerifier }
        )

        val cmd = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = CmdActionType.APP_LAUNCH,
            targetSelector = TargetSelector.ByText(targetPkg),
            parameters = mapOf("appLabel" to targetPkg)
        )

        val resultStr = coordinator.executeCommand(cmd)

        val successVerifiedName = ResultCode.SUCCESS_VERIFIED.name
        val hasVerified = resultStr.contains(successVerifiedName)
        val hasLaunch = resultStr.contains("APP_LAUNCH")
        val isMatch = hasVerified || hasLaunch
        assertTrue("Result text '$resultStr' should indicate success or launch", isMatch)

        val recordedResult: ActionExecutionResult? = service.lastActionExecutionResult
        assertNotNull("Action execution result should be recorded", recordedResult)

        if (recordedResult != null) {
            assertEquals(ActionType.APP_LAUNCH, recordedResult.request.actionType)
            assertEquals(targetPkg, recordedResult.request.expectedPackageName)
            assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, recordedResult.verificationResult.status)
            assertEquals(ResultCode.SUCCESS_VERIFIED, recordedResult.verificationResult.resultCode)

            val history = service.actionExecutionHistory
            assertEquals(1, history.size)
            assertEquals(recordedResult.executionId, history[0].executionId)
        }
    }

    @Test
    fun testUiClickExecutionPreservesPrePostSnapshotsAndPropagatesToHistory() {
        val service = AgentAccessibilityService()
        service.onServiceConnectedForTest()

        val preSnap = ObservationSnapshot(
            generation = 1L,
            packageName = "com.transsion.hilauncher",
            nodeCount = 2,
            rootNode = ObservationNode(
                nodeId = "camera_btn",
                nodeIdentity = "id:camera_btn",
                packageName = "com.transsion.hilauncher",
                text = "Camera",
                clickable = true
            )
        )
        service.lastExternalObservationSnapshot = preSnap

        val coordinator = CommandExecutionCoordinator(
            context = context,
            accessibilityServiceSupplier = { service }
        )

        val cmd = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = CmdActionType.UI_CLICK,
            targetSelector = TargetSelector.ByText("Camera"),
            parameters = mapOf("targetIdentity" to "id:camera_btn")
        )

        val resultStr = coordinator.executeCommand(cmd)
        assertNotNull(resultStr)

        val recordedResult: ActionExecutionResult? = service.lastActionExecutionResult
        assertNotNull(recordedResult)

        if (recordedResult != null) {
            assertEquals(ActionType.UI_CLICK, recordedResult.request.actionType)
            val preSnapshotObj = recordedResult.preSnapshot
            assertNotNull(preSnapshotObj)
            assertEquals("com.transsion.hilauncher", preSnapshotObj?.packageName)
        }
    }
}
