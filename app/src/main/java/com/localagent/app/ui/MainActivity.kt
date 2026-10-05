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

        binding.btnOpenCurrentObservationScreen.setOnClickListener {
            startActivity(Intent(this, CurrentObservationActivity::class.java))
        }

        binding.btnOpenExternalObservationScreen.setOnClickListener {
            startActivity(Intent(this, ExternalObservationActivity::class.java))
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
                val normalizedCmd = parseResult.command

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
                    val resultText = "Result: ${ResultCode.POLICY_BLOCKED} | ${policyResult.explanation}"
                    binding.tvLatestResult.text = resultText

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
                    updateSystemStatusSummary()
                    return
                }

                // Dispatch through Universal Production Pipeline
                app.goalDispatcher.enqueueCommand(normalizedCmd, priority = 1)

                app.eventLogger.logEvent(
                    AgentEvent(
                        eventId = UUID.randomUUID().toString(),
                        sessionId = activeSessionId,
                        correlationId = correlationId,
                        subsystem = EventSubsystem.ACTION,
                        eventType = "GOAL_QUEUED",
                        actionType = normalizedCmd.actionType.name,
                        sourceChannel = normalizedCmd.source.name
                    )
                )

                val polled = app.goalDispatcher.pollNextCommandForExecution()

                if (polled != null) {
                    val executionResult = executeNormalizedCommand(polled.command)
                    binding.tvLatestResult.text = executionResult

                    app.eventLogger.logEvent(
                        AgentEvent(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = activeSessionId,
                            correlationId = correlationId,
                            subsystem = EventSubsystem.ACTION,
                            eventType = "ACTION_RESULT",
                            actionType = polled.command.actionType.name,
                            sourceChannel = polled.command.source.name,
                            metadataJson = "{\"resultText\":\"$executionResult\"}"
                        )
                    )

                    app.goalDispatcher.completeExecution()
                } else {
                    val busyText = "Result: ${ResultCode.TIMEOUT} | Execution channel locked by concurrent transaction"
                    binding.tvLatestResult.text = busyText

                    app.eventLogger.logEvent(
                        AgentEvent(
                            eventId = UUID.randomUUID().toString(),
                            sessionId = activeSessionId,
                            correlationId = correlationId,
                            subsystem = EventSubsystem.ACTION,
                            eventType = "EXECUTION_LOCKED",
                            actionType = normalizedCmd.actionType.name,
                            resultCode = ResultCode.TIMEOUT,
                            severity = EventSeverity.WARNING
                        )
                    )
                }
                updateSystemStatusSummary()
            }
        }
    }

    private fun executeNormalizedCommand(command: NormalizedCommand): String {
        return when (command.actionType) {
            ActionType.OBSERVE -> {
                if (AgentAccessibilityService.isBound) {
                    val snapshot = AgentAccessibilityService.INSTANCE?.captureLiveSnapshot()
                    if (snapshot != null) {
                        "Command: OBSERVE | Status: ${ResultCode.SUCCESS_VERIFIED} | Pkg: ${snapshot.packageName} | Nodes: ${snapshot.nodeCount} | Truncated: ${snapshot.truncationInfo.isTruncated}"
                    } else {
                        "Command: OBSERVE | Status: ${ResultCode.ACTION_FAILED} | Reason: Null snapshot"
                    }
                } else {
                    "Command: OBSERVE | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Open Settings to enable)"
                }
            }
            ActionType.GLOBAL_BACK -> "Command: GLOBAL_BACK | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.GLOBAL_HOME -> "Command: GLOBAL_HOME | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.GLOBAL_RECENTS -> "Command: GLOBAL_RECENTS | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_CLICK -> "Command: UI_CLICK | Target: ${command.targetSelector} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_LONG_CLICK -> "Command: UI_LONG_CLICK | Target: ${command.targetSelector} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_SCROLL_FORWARD -> "Command: UI_SCROLL_FORWARD | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_SCROLL_BACKWARD -> "Command: UI_SCROLL_BACKWARD | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.AGENT_STATUS -> "Command: AGENT_STATUS | Status: ${ResultCode.NO_EFFECT_EXPECTED} | Info: Agent ACTIVE, A11y ${if (AgentAccessibilityService.isBound) "BOUND" else "DISCONNECTED"}, Core Ready"
            ActionType.APP_LAUNCH -> "Command: APP_LAUNCH | Target: ${command.parameters["appLabel"]} | Status: ${ResultCode.DISPATCHED_BUT_NOT_VERIFIED} | Reason: App launch dispatched without foreground verification"
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
