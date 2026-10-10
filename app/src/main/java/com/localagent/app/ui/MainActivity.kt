package com.localagent.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.databinding.ActivityMainBinding
import com.localagent.app.system.AppLauncher
import com.localagent.app.system.AppResolver
import com.localagent.app.system.LaunchVerificationResult
import com.localagent.app.system.LaunchVerifier
import com.localagent.core.command.ActionType
import com.localagent.core.command.CommandNormalizer
import com.localagent.core.command.CommandParseResult
import com.localagent.core.command.CommandSource
import com.localagent.core.command.NormalizedCommand
import com.localagent.core.command.TargetSelector
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

        binding.btnOpenCurrentObservationScreen.setOnClickListener {
            startActivity(Intent(this, CurrentObservationActivity::class.java))
        }

        binding.btnOpenExternalObservationScreen.setOnClickListener {
            startActivity(Intent(this, ExternalObservationActivity::class.java))
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
                val resultsList = mutableListOf<String>()
                var sequenceHalted = false

                for ((index, normalizedCmd) in sequence.withIndex()) {
                    val stepNumber = index + 1
                    val totalSteps = sequence.size
                    val stepPrefix = if (totalSteps > 1) "Step $stepNumber/$totalSteps — " else ""

                    if (sequenceHalted) {
                        resultsList.add("${stepPrefix}${normalizedCmd.actionType}: SKIPPED (Prior step failed)")
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
                            metadataJson = "{\"target\":\"${normalizedCmd.targetSelector}\"}"
                        )
                    )

                    // Policy Evaluation
                    val policyResult = app.policyEngine.evaluateCommand(normalizedCmd)
                    if (policyResult is PolicyEvaluationResult.UserConfirmationRequired) {
                        val resultText = "${stepPrefix}Result: ${ResultCode.POLICY_BLOCKED} | ${policyResult.explanation}"
                        resultsList.add(resultText)
                        sequenceHalted = true

                        app.eventLogger.logEvent(
                            AgentEvent(
                                eventId = UUID.randomUUID().toString(),
                                sessionId = activeSessionId,
                                correlationId = correlationId,
                                subsystem = EventSubsystem.POLICY,
                                eventType = "POLICY_BLOCKED",
                                actionType = normalizedCmd.actionType.name,
                                sourceChannel = normalizedCmd.source.name,
                                resultCode = ResultCode.POLICY_BLOCKED,
                                severity = EventSeverity.WARNING,
                                metadataJson = "{\"explanation\":\"${policyResult.explanation}\"}"
                            )
                        )
                        continue
                    }

                    val executionResultText = kotlinx.coroutines.runBlocking { executeNormalizedCommand(normalizedCmd) }
                    val fullStepResult = "${stepPrefix}$executionResultText"
                    resultsList.add(fullStepResult)

                    app.eventLogger.logEvent(
                        AgentEvent(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = activeSessionId,
                            correlationId = correlationId,
                            subsystem = EventSubsystem.ACTION,
                            eventType = "ACTION_RESULT",
                            actionType = normalizedCmd.actionType.name,
                            sourceChannel = normalizedCmd.source.name,
                            metadataJson = "{\"resultText\":\"$executionResultText\"}"
                        )
                    )

                    if (executionResultText.contains("TIMEOUT") ||
                        executionResultText.contains("ACTION_FAILED") ||
                        executionResultText.contains("ACCESSIBILITY_UNAVAILABLE") ||
                        executionResultText.contains("TARGET_NOT_FOUND") ||
                        executionResultText.contains("CAPABILITY_UNAVAILABLE")) {
                        sequenceHalted = true
                    }
                }

                binding.tvLatestResult.text = resultsList.joinToString("\n")
                updateSystemStatusSummary()
            }
        }
    }

    private suspend fun executeNormalizedCommand(command: NormalizedCommand): String {
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
                    val request = com.localagent.core.action.ActionRequest(
                        actionType = coreActionType,
                        targetNodeId = targetIdStr,
                        targetNodeIdentity = command.parameters["targetIdentity"],
                        textInputPayload = command.parameters["text"],
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
                val appLabel = command.parameters["appLabel"] ?: ""
                val resolver = com.localagent.app.system.AppResolver(this)
                val targetPkg = resolver.resolvePackageName(appLabel)

                if (targetPkg == null) {
                    "Command: APP_LAUNCH | Target: $appLabel | Status: ${ResultCode.TARGET_NOT_FOUND} | Reason: App '$appLabel' not found on device"
                } else {
                    val launcher = com.localagent.app.system.AppLauncher(this)
                    val preGen = service?.currentGeneration ?: 0L
                    val launchSuccess = launcher.launchApp(targetPkg)

                    if (!launchSuccess) {
                        "Command: APP_LAUNCH | Target: $appLabel ($targetPkg) | Status: ${ResultCode.ACTION_FAILED} | Reason: Failed to dispatch launch intent"
                    } else {
                        val verifier = com.localagent.app.system.LaunchVerifier()
                        when (val verification = verifier.awaitUsableWindowAndSnapshot(targetPkg, preGen)) {
                            is com.localagent.app.system.LaunchVerificationResult.Success -> {
                                verification.rootNode.recycle()
                                "Command: APP_LAUNCH | Target: $appLabel ($targetPkg) | Status: ${ResultCode.SUCCESS_VERIFIED} | Reason: Usable interactive window and root node acquired"
                            }
                            is com.localagent.app.system.LaunchVerificationResult.Timeout -> {
                                "Command: APP_LAUNCH | Target: $appLabel ($targetPkg) | Status: ${ResultCode.TIMEOUT} | Reason: ${verification.reason}"
                            }
                        }
                    }
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
