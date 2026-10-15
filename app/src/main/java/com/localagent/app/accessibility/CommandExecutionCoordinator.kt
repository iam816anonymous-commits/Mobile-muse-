package com.localagent.app.accessibility

import android.content.Context
import com.localagent.app.system.AppLaunchResult
import com.localagent.app.system.AppLauncher
import com.localagent.app.system.AppResolutionResult
import com.localagent.app.system.AppResolver
import com.localagent.app.system.LaunchVerificationResult
import com.localagent.app.system.LaunchVerifier
import com.localagent.core.action.ActionExecutionResult
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType as CoreActionType
import com.localagent.core.action.VerificationResult
import com.localagent.core.action.VerificationStatus
import com.localagent.core.command.ActionType as CommandActionType
import com.localagent.core.command.NormalizedCommand
import com.localagent.core.command.TargetSelector
import com.localagent.core.result.ResultCode

class CommandExecutionCoordinator(
    private val context: Context,
    private val accessibilityServiceSupplier: () -> AgentAccessibilityService? = { AgentAccessibilityService.INSTANCE },
    private val launchVerifierSupplier: (service: AgentAccessibilityService?) -> LaunchVerifier = { service -> LaunchVerifier(accessibilityServiceSupplier = { service }) }
) {
    fun executeCommand(command: NormalizedCommand): String {
        val service = accessibilityServiceSupplier()
        val isA11yBound = AgentAccessibilityService.isBound && service != null

        return when (command.actionType) {
            CommandActionType.OBSERVE -> {
                if (isA11yBound && service != null) {
                    val snapshot = service.captureLiveSnapshot()
                    "Command: OBSERVE | Status: ${ResultCode.SUCCESS_VERIFIED} | Pkg: ${snapshot.packageName} | Nodes: ${snapshot.nodeCount} | Truncated: ${snapshot.truncationInfo.isTruncated}"
                } else {
                    "Command: OBSERVE | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }

            CommandActionType.GLOBAL_BACK, CommandActionType.GLOBAL_HOME, CommandActionType.GLOBAL_RECENTS -> {
                if (isA11yBound && service != null) {
                    val globalExecutor = GlobalActionExecutor(accessibilityService = service)
                    val coreActionType = when (command.actionType) {
                        CommandActionType.GLOBAL_BACK -> CoreActionType.GLOBAL_BACK
                        CommandActionType.GLOBAL_HOME -> CoreActionType.GLOBAL_HOME
                        CommandActionType.GLOBAL_RECENTS -> CoreActionType.GLOBAL_RECENTS
                        else -> CoreActionType.GLOBAL_BACK
                    }
                    val request = ActionRequest(
                        actionType = coreActionType,
                        sourceChannel = command.source.name
                    )
                    val result = globalExecutor.execute(request)
                    "Command: ${command.actionType} | Status: ${result.verificationResult.resultCode} | Reason: ${result.verificationResult.reason ?: "Executed"}"
                } else {
                    "Command: ${command.actionType} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }

            CommandActionType.UI_CLICK, CommandActionType.UI_LONG_CLICK, CommandActionType.UI_TEXT_INPUT, CommandActionType.UI_SCROLL_FORWARD, CommandActionType.UI_SCROLL_BACKWARD -> {
                if (isA11yBound && service != null) {
                    val uiExecutor = UiActionExecutor(
                        accessibilityService = service,
                        snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: service.captureLiveSnapshot() },
                        requireExternalContext = true
                    )
                    val coreActionType = when (command.actionType) {
                        CommandActionType.UI_CLICK -> CoreActionType.UI_CLICK
                        CommandActionType.UI_LONG_CLICK -> CoreActionType.UI_LONG_CLICK
                        CommandActionType.UI_TEXT_INPUT -> CoreActionType.UI_TEXT_INPUT
                        CommandActionType.UI_SCROLL_FORWARD -> CoreActionType.UI_SCROLL_FORWARD
                        CommandActionType.UI_SCROLL_BACKWARD -> CoreActionType.UI_SCROLL_BACKWARD
                        else -> CoreActionType.UI_CLICK
                    }
                    val targetIdStr = when (val selector = command.targetSelector) {
                        is TargetSelector.ByViewId -> selector.viewIdResourceName
                        is TargetSelector.ByText -> selector.text
                        is TargetSelector.ByContentDescription -> selector.contentDescription
                        is TargetSelector.ByNodeIdentityKey -> selector.nodeIdentityKey
                        is TargetSelector.ByCoordinates -> "coords:${selector.x},${selector.y}"
                        TargetSelector.None -> null
                    }
                    val expectedPkg = command.parameters["expectedPackageName"]
                    val request = ActionRequest(
                        actionType = coreActionType,
                        targetNodeId = targetIdStr,
                        targetNodeIdentity = command.parameters["targetIdentity"],
                        textInputPayload = command.parameters["text"],
                        expectedPackageName = expectedPkg,
                        preActionMinGeneration = service.currentGeneration,
                        sourceChannel = command.source.name
                    )
                    val result = uiExecutor.execute(request)
                    "Command: ${command.actionType} | Target: ${targetIdStr ?: "None"} | Status: ${result.verificationResult.resultCode} | Reason: ${result.verificationResult.reason ?: "Executed"}"
                } else {
                    "Command: ${command.actionType} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }

            CommandActionType.AGENT_STATUS -> "Command: AGENT_STATUS | Status: ${ResultCode.NO_EFFECT_EXPECTED} | Info: Agent ACTIVE, A11y ${if (isA11yBound) "BOUND" else "DISCONNECTED"}, Core Ready"

            CommandActionType.APP_LAUNCH -> {
                val appQuery = command.parameters["appLabel"]
                    ?: command.parameters["appQuery"]
                    ?: when (val sel = command.targetSelector) {
                        is TargetSelector.ByText -> sel.text
                        else -> ""
                    }

                val resolver = AppResolver(context)
                val startTime = System.currentTimeMillis()

                when (val resolveRes = resolver.resolvePackage(appQuery)) {
                    is AppResolutionResult.Success -> {
                        val targetPkg = resolveRes.packageName
                        val launcher = AppLauncher(context)
                        val preSnap = service?.getSnapshotForContext(isExternal = true)
                        val preGen = service?.currentGeneration ?: 0L

                        when (val launchRes = launcher.launchApp(targetPkg)) {
                            is AppLaunchResult.Dispatched,
                            is AppLaunchResult.AlreadyForeground -> {
                                val verifier = launchVerifierSupplier(service)
                                val postSnapshot = verifier.awaitUsableWindowAndSnapshot(targetPkg, preGen)

                                if (postSnapshot != null) {
                                    val req = ActionRequest(
                                        actionType = CoreActionType.APP_LAUNCH,
                                        targetNodeId = targetPkg,
                                        expectedPackageName = targetPkg,
                                        preActionMinGeneration = preGen,
                                        sourceChannel = command.source.name
                                    )
                                    val verResult = VerificationResult(
                                        status = VerificationStatus.EXECUTED_AND_VERIFIED,
                                        resultCode = ResultCode.SUCCESS_VERIFIED,
                                        reason = "Application '${resolveRes.appLabel}' ($targetPkg) launched and foreground verified"
                                    )
                                    val execResult = ActionExecutionResult(
                                        request = req,
                                        dispatchSuccess = true,
                                        preSnapshot = preSnap,
                                        postSnapshot = postSnapshot,
                                        verificationResult = verResult,
                                        executionDurationMs = System.currentTimeMillis() - startTime
                                    )
                                    service?.recordActionExecution(execResult)
                                    "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} ($targetPkg) | Status: ${ResultCode.SUCCESS_VERIFIED} | Pkg: ${postSnapshot.packageName} | Nodes: ${postSnapshot.nodeCount}"
                                } else {
                                    val verification = verifier.verifyForeground(targetPkg, timeoutMs = 100L)
                                    val reasonStr = when (verification) {
                                        is LaunchVerificationResult.Timeout -> verification.detailedReason
                                        is LaunchVerificationResult.PackageMismatch -> "Expected package '$targetPkg' but actual foreground package was '${verification.actualPackageName}'"
                                        is LaunchVerificationResult.AccessibilityUnavailable -> verification.reason
                                        is LaunchVerificationResult.Success -> "Target window timeout waiting for snapshot publication of '$targetPkg'"
                                    }
                                    val req = ActionRequest(
                                        actionType = CoreActionType.APP_LAUNCH,
                                        targetNodeId = targetPkg,
                                        expectedPackageName = targetPkg,
                                        preActionMinGeneration = preGen,
                                        sourceChannel = command.source.name
                                    )
                                    val verResult = VerificationResult(
                                        status = VerificationStatus.EXECUTION_FAILED,
                                        resultCode = ResultCode.TIMEOUT,
                                        reason = reasonStr
                                    )
                                    val execResult = ActionExecutionResult(
                                        request = req,
                                        dispatchSuccess = false,
                                        preSnapshot = preSnap,
                                        postSnapshot = null,
                                        verificationResult = verResult,
                                        executionDurationMs = System.currentTimeMillis() - startTime
                                    )
                                    service?.recordActionExecution(execResult)
                                    "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} ($targetPkg) | Status: ${ResultCode.TIMEOUT} | Reason: $reasonStr"
                                }
                            }

                            is AppLaunchResult.LaunchFailed -> {
                                val req = ActionRequest(
                                    actionType = CoreActionType.APP_LAUNCH,
                                    targetNodeId = targetPkg,
                                    expectedPackageName = targetPkg,
                                    sourceChannel = command.source.name
                                )
                                val verResult = VerificationResult(
                                    status = VerificationStatus.EXECUTION_FAILED,
                                    resultCode = ResultCode.ACTION_FAILED,
                                    reason = launchRes.reason
                                )
                                val execResult = ActionExecutionResult(
                                    request = req,
                                    dispatchSuccess = false,
                                    preSnapshot = preSnap,
                                    postSnapshot = null,
                                    verificationResult = verResult,
                                    executionDurationMs = System.currentTimeMillis() - startTime
                                )
                                service?.recordActionExecution(execResult)
                                "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} ($targetPkg) | Status: ${ResultCode.ACTION_FAILED} | Reason: ${launchRes.reason}"
                            }
                        }
                    }

                    is AppResolutionResult.PackageNotInstalled -> {
                        "Command: APP_LAUNCH | Target: $appQuery | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: ${resolveRes.reason}"
                    }

                    is AppResolutionResult.NotLaunchable -> {
                        "Command: APP_LAUNCH | Target: $appQuery | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: ${resolveRes.reason}"
                    }
                }
            }

            else -> "Command: ${command.actionType} | Status: ${ResultCode.CAPABILITY_UNAVAILABLE}"
        }
    }
}
