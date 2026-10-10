package com.localagent.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.databinding.ActivityMainBinding
import com.localagent.core.command.*
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.policy.PolicyEvaluationResult
import com.localagent.core.result.ResultCode
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val commandNormalizer = CommandNormalizer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateSystemStatusSummary()
    }

    override fun onResume() {
        super.onResume()
        updateSystemStatusSummary()
    }

    private fun setupListeners() {
        binding.btnExecute.setOnClickListener {
            val input = binding.etCommandInput.text.toString()
            executeCommandFromInput(input)
        }

        binding.btnToggleFloatingConsole.setOnClickListener {
            val intent = Intent(this, FloatingConsoleService::class.java)
            startService(intent)
        }

        binding.btnOpenPermissionCenterScreen.setOnClickListener {
            startActivity(Intent(this, PermissionActivity::class.java))
        }

        binding.btnOpenObservationScreen.setOnClickListener {
            startActivity(Intent(this, ObservationActivity::class.java))
        }

        binding.btnOpenEvidenceScreen.setOnClickListener {
            startActivity(Intent(this, EvidenceActivity::class.java))
        }

        binding.btnOpenEventLogScreen.setOnClickListener {
            startActivity(Intent(this, EventLogActivity::class.java))
        }

        binding.btnOpenStorageDiagnosticsScreen.setOnClickListener {
            startActivity(Intent(this, StorageDiagnosticsActivity::class.java))
        }
    }

    fun executeCommandFromInput(input: String) {
        val app = application as? LocalAgentApplication
            ?: return

        val activeSessionId = app.eventLogger.getActiveSession().sessionId
        val correlationId = UUID.randomUUID().toString()

        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                correlationId = correlationId,
                subsystem = EventSubsystem.COMMAND,
                eventType = "COMMAND_INPUT_RECEIVED",
                sourceChannel = CommandSource.CONSOLE.name,
                metadataJson = "{\"rawInput\":\"$input\"}"
            )
        )

        when (val parseResult = commandNormalizer.parseInput(input, CommandSource.CONSOLE)) {
            is CommandParseResult.InvalidInput -> {
                val resultText = "Result: ${ResultCode.INVALID_INPUT} | Reason: ${parseResult.reason}"
                binding.tvLatestResult.text = resultText

                app.eventLogger.logEvent(
                    AgentEvent(
                        eventId = UUID.randomUUID().toString(),
                        sessionId = activeSessionId,
                        correlationId = correlationId,
                        subsystem = EventSubsystem.COMMAND,
                        eventType = "COMMAND_REJECTED",
                        sourceChannel = CommandSource.CONSOLE.name,
                        resultCode = ResultCode.INVALID_INPUT,
                        severity = EventSeverity.WARNING,
                        errorCode = "INVALID_INPUT",
                        metadataJson = "{\"reason\":\"${parseResult.reason}\"}"
                    )
                )
                updateSystemStatusSummary()
                return
            }
            is CommandParseResult.UnknownCommand -> {
                val resultText = "Result: ${ResultCode.UNKNOWN_COMMAND} | Reason: Unrecognized command syntax '${parseResult.rawInput}'"
                binding.tvLatestResult.text = resultText

                app.eventLogger.logEvent(
                    AgentEvent(
                        eventId = UUID.randomUUID().toString(),
                        sessionId = activeSessionId,
                        correlationId = correlationId,
                        subsystem = EventSubsystem.COMMAND,
                        eventType = "COMMAND_REJECTED",
                        sourceChannel = CommandSource.CONSOLE.name,
                        resultCode = ResultCode.UNKNOWN_COMMAND,
                        severity = EventSeverity.WARNING,
                        errorCode = "UNKNOWN_COMMAND",
                        metadataJson = "{\"rawInput\":\"${parseResult.rawInput}\"}"
                    )
                )
                updateSystemStatusSummary()
                return
            }
            is CommandParseResult.Success -> {
                val sequence = parseResult.sequence
                val summaryResults = mutableListOf<String>()
                var sequenceHalted = false

                for ((idx, normalizedCmd) in sequence.withIndex()) {
                    if (sequenceHalted) {
                        summaryResults.add("[Step ${idx + 1}/${sequence.size}] ${normalizedCmd.actionType}: SKIPPED (Prior step failed)")
                        continue
                    }

                    app.eventLogger.logEvent(
                        AgentEvent(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = activeSessionId,
                            correlationId = correlationId,
                            subsystem = EventSubsystem.COMMAND,
                            eventType = "COMMAND_PARSED",
                            actionType = normalizedCmd.actionType.name,
                            sourceChannel = normalizedCmd.source.name,
                            metadataJson = "{\"step\":${idx + 1},\"total\":${sequence.size},\"target\":\"${normalizedCmd.targetSelector}\"}"
                        )
                    )

                    // Policy Evaluation
                    val policyResult = app.policyEngine.evaluateCommand(normalizedCmd, isUserConfirmed = true)
                    if (policyResult is PolicyEvaluationResult.Blocked) {
                        summaryResults.add("[Step ${idx + 1}/${sequence.size}] ${normalizedCmd.actionType}: POLICY_BLOCKED (${policyResult.reason})")
                        sequenceHalted = true
                        continue
                    } else if (policyResult is PolicyEvaluationResult.UserConfirmationRequired) {
                        summaryResults.add("[Step ${idx + 1}/${sequence.size}] ${normalizedCmd.actionType}: POLICY_BLOCKED (${policyResult.explanation})")
                        sequenceHalted = true
                        continue
                    }

                    // Dispatch through Universal Production Pipeline
                    app.goalDispatcher.enqueueCommand(normalizedCmd, priority = 1)
                    val polled = app.goalDispatcher.pollNextCommandForExecution()

                    if (polled != null) {
                        val executionResult = executeNormalizedCommand(polled.command)
                        summaryResults.add("[Step ${idx + 1}/${sequence.size}] $executionResult")

                        if (executionResult.contains(ResultCode.TARGET_NOT_FOUND.name) ||
                            executionResult.contains(ResultCode.ACTION_FAILED.name) ||
                            executionResult.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name)) {
                            sequenceHalted = true
                        }

                        app.goalDispatcher.completeExecution()
                    } else {
                        summaryResults.add("[Step ${idx + 1}/${sequence.size}] ${normalizedCmd.actionType}: EXECUTION_LOCKED")
                        sequenceHalted = true
                    }
                }

                binding.tvLatestResult.text = summaryResults.joinToString("\n")
                updateSystemStatusSummary()
            }
        }
    }

    private fun executeNormalizedCommand(command: NormalizedCommand): String {
        val service = AgentAccessibilityService.INSTANCE
        val isA11yBound = AgentAccessibilityService.isBound && service != null

        return when (command.actionType) {
            ActionType.OBSERVE -> {
                if (isA11yBound) {
                    val snapshot = service?.captureLiveSnapshot()
                    if (snapshot != null) {
                        "Command: OBSERVE | Status: ${ResultCode.SUCCESS_VERIFIED} | Pkg: ${snapshot.packageName} | Nodes: ${snapshot.nodeCount} | Truncated: ${snapshot.truncationInfo.isTruncated}"
                    } else {
                        "Command: OBSERVE | Status: ${ResultCode.ACTION_FAILED} | Reason: Null snapshot"
                    }
                } else {
                    "Command: OBSERVE | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }
            ActionType.GLOBAL_BACK, ActionType.GLOBAL_HOME, ActionType.GLOBAL_RECENTS -> {
                if (isA11yBound) {
                    val globalExecutor = com.localagent.app.accessibility.GlobalActionExecutor(accessibilityService = service)
                    val coreActionType = when (command.actionType) {
                        ActionType.GLOBAL_BACK -> com.localagent.core.action.ActionType.GLOBAL_BACK
                        ActionType.GLOBAL_HOME -> com.localagent.core.action.ActionType.GLOBAL_HOME
                        ActionType.GLOBAL_RECENTS -> com.localagent.core.action.ActionType.GLOBAL_RECENTS
                        else -> com.localagent.core.action.ActionType.GLOBAL_BACK
                    }
                    val request = com.localagent.core.action.ActionRequest(
                        actionType = coreActionType,
                        sourceChannel = command.source.name
                    )
                    val result = globalExecutor.execute(request)
                    "Command: ${command.actionType} | Status: ${result.verificationResult.resultCode} | Reason: ${result.verificationResult.reason ?: "Executed"}"
                } else {
                    "Command: ${command.actionType} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }
            ActionType.UI_CLICK, ActionType.UI_LONG_CLICK, ActionType.UI_TEXT_INPUT, ActionType.UI_SCROLL_FORWARD, ActionType.UI_SCROLL_BACKWARD -> {
                if (isA11yBound && service != null) {
                    val activeService = service
                    val uiExecutor = com.localagent.app.accessibility.UiActionExecutor(
                        accessibilityService = activeService,
                        snapshotProvider = { activeService.getSnapshotForContext(isExternal = true) ?: activeService.captureLiveSnapshot() },
                        requireExternalContext = true
                    )
                    val coreActionType = when (command.actionType) {
                        ActionType.UI_CLICK -> com.localagent.core.action.ActionType.UI_CLICK
                        ActionType.UI_LONG_CLICK -> com.localagent.core.action.ActionType.UI_LONG_CLICK
                        ActionType.UI_TEXT_INPUT -> com.localagent.core.action.ActionType.UI_TEXT_INPUT
                        ActionType.UI_SCROLL_FORWARD -> com.localagent.core.action.ActionType.UI_SCROLL_FORWARD
                        ActionType.UI_SCROLL_BACKWARD -> com.localagent.core.action.ActionType.UI_SCROLL_BACKWARD
                        else -> com.localagent.core.action.ActionType.UI_CLICK
                    }
                    val targetIdStr = when (val selector = command.targetSelector) {
                        is TargetSelector.ByViewId -> selector.viewIdResourceName
                        is TargetSelector.ByText -> selector.text
                        is TargetSelector.ByContentDescription -> selector.contentDescription
                        is TargetSelector.ByNodeIdentityKey -> selector.nodeIdentityKey
                        is TargetSelector.ByCoordinates -> "coords:${selector.x},${selector.y}"
                        TargetSelector.None -> null
                    }
                    val expectedPkg = activeService.lastExternalPackageName.takeIf { activeService.isValidExternalApplicationPackage(it) }
                    val request = com.localagent.core.action.ActionRequest(
                        actionType = coreActionType,
                        targetNodeId = targetIdStr,
                        targetNodeIdentity = command.parameters["targetIdentity"],
                        textInputPayload = command.parameters["text"],
                        expectedPackageName = expectedPkg,
                        preActionMinGeneration = activeService.snapshotGenerationCounter,
                        sourceChannel = command.source.name
                    )
                    val result = uiExecutor.execute(request)
                    "Command: ${command.actionType} | Target: ${targetIdStr ?: "None"} | Status: ${result.verificationResult.resultCode} | Reason: ${result.verificationResult.reason ?: "Executed"}"
                } else {
                    "Command: ${command.actionType} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }
            ActionType.AGENT_STATUS -> "Command: AGENT_STATUS | Status: ${ResultCode.NO_EFFECT_EXPECTED} | Info: Agent ACTIVE, A11y ${if (isA11yBound) "BOUND" else "DISCONNECTED"}, Core Ready"
            ActionType.APP_LAUNCH -> {
                val appQuery = command.parameters["appLabel"] ?: ""
                val resolver = com.localagent.app.system.AppResolver(this)
                when (val resolveRes = resolver.resolvePackage(appQuery)) {
                    is com.localagent.app.system.AppResolutionResult.Success -> {
                        val launcher = com.localagent.app.system.AppLauncher(this)
                        val verifier = com.localagent.app.system.LaunchVerifier()
                        when (val launchRes = launcher.launchApp(resolveRes.packageName)) {
                            is com.localagent.app.system.AppLaunchResult.Dispatched,
                            is com.localagent.app.system.AppLaunchResult.AlreadyForeground -> {
                                when (val verifyRes = verifier.verifyForeground(resolveRes.packageName, timeoutMs = 3000L)) {
                                    is com.localagent.app.system.LaunchVerificationResult.Success ->
                                        "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} (${resolveRes.packageName}) | Status: ${ResultCode.SUCCESS_VERIFIED} | Duration: ${verifyRes.durationMs}ms"
                                    is com.localagent.app.system.LaunchVerificationResult.Timeout ->
                                        "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: Foreground verification timed out"
                                    is com.localagent.app.system.LaunchVerificationResult.PackageMismatch ->
                                        "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: Package mismatch '${verifyRes.actualPackageName}'"
                                    is com.localagent.app.system.LaunchVerificationResult.AccessibilityUnavailable ->
                                        "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility unbound"
                                }
                            }
                            is com.localagent.app.system.AppLaunchResult.LaunchFailed ->
                                "Command: APP_LAUNCH | Target: ${resolveRes.appLabel} | Status: ${ResultCode.ACTION_FAILED} | Reason: ${launchRes.reason}"
                        }
                    }
                    is com.localagent.app.system.AppResolutionResult.PackageNotInstalled ->
                        "Command: APP_LAUNCH | Target: $appQuery | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: App not installed"
                    is com.localagent.app.system.AppResolutionResult.NotLaunchable ->
                        "Command: APP_LAUNCH | Target: $appQuery | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: ${resolveRes.reason}"
                }
            }
            else -> "Command: ${command.actionType} | Status: ${ResultCode.CAPABILITY_UNAVAILABLE}"
        }
    }

    private fun updateSystemStatusSummary() {
        val app = application as? LocalAgentApplication ?: return
        val a11yBound = AgentAccessibilityService.isBound
        val durableStatus = app.durableStorageManager.getAvailabilityStatus()

        binding.tvSystemStatusSummary.text = "Agent Core: ACTIVE (API 27 Baseline)\nAccessibility Service: ${if (a11yBound) "READY (BOUND)" else "SERVICE_UNBOUND"}\nOperational Storage: agent.db (APP_PRIVATE)\nDurable Storage: $durableStatus"
    }
}
