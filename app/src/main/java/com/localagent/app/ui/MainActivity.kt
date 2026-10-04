package com.localagent.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.databinding.ActivityMainBinding
import com.localagent.core.command.*
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventFilter
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.policy.PolicyEvaluationResult
import com.localagent.core.result.ResultCode
import java.util.LinkedList
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val commandNormalizer = CommandNormalizer()
    private val maxEventLogSize = 10
    private val recentEventLogs = LinkedList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        logEvent("[SYSTEM] Foundation Test UI Ready. Target API: 27 Baseline.")
        updateStorageDiagnostics()
    }

    private fun setupListeners() {
        binding.btnExecute.setOnClickListener {
            val input = binding.etCommandInput.text.toString()
            executeCommandFromInput(input)
        }

        binding.btnObserve.setOnClickListener {
            triggerObserveAction()
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

        logEvent("[COMMAND_RECEIVED] Raw Input: '$input'")

        when (val parseResult = commandNormalizer.parseInput(input, CommandSource.CONSOLE)) {
            is CommandParseResult.InvalidInput -> {
                val resultText = "Result: ${ResultCode.INVALID_INPUT} | Reason: ${parseResult.reason}"
                binding.tvLatestResult.text = resultText
                logEvent("[COMMAND_REJECTED] $resultText")

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
                updateStorageDiagnostics()
                return
            }
            is CommandParseResult.UnknownCommand -> {
                val resultText = "Result: ${ResultCode.UNKNOWN_COMMAND} | Reason: Unrecognized command syntax '${parseResult.rawInput}'"
                binding.tvLatestResult.text = resultText
                logEvent("[COMMAND_REJECTED] $resultText")

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
                updateStorageDiagnostics()
                return
            }
            is CommandParseResult.Success -> {
                val normalizedCmd = parseResult.command
                logEvent("[COMMAND_PARSED] Action: ${normalizedCmd.actionType} | Target: ${normalizedCmd.targetSelector}")

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
                    logEvent("[POLICY_EVALUATED] $resultText")

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
                    updateStorageDiagnostics()
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
                    logEvent("[COMMAND_RESULT] $executionResult")

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
                    logEvent("[EXECUTION_LOCK] $busyText")

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
                updateStorageDiagnostics()
            }
        }
    }

    private fun triggerObserveAction() {
        logEvent("[COMMAND_RECEIVED] Raw Input: 'observe'")
        executeCommandFromInput("observe")
    }

    private fun executeNormalizedCommand(command: NormalizedCommand): String {
        return when (command.actionType) {
            ActionType.GLOBAL_BACK -> "Command: GLOBAL_BACK | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.GLOBAL_HOME -> "Command: GLOBAL_HOME | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.GLOBAL_RECENTS -> "Command: GLOBAL_RECENTS | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_CLICK -> "Command: UI_CLICK | Target: ${command.targetSelector} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_LONG_CLICK -> "Command: UI_LONG_CLICK | Target: ${command.targetSelector} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_SCROLL_FORWARD -> "Command: UI_SCROLL_FORWARD | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_SCROLL_BACKWARD -> "Command: UI_SCROLL_BACKWARD | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.OBSERVE -> "Command: OBSERVE | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound (Phase 5 Observation Engine Required)"
            ActionType.AGENT_STATUS -> "Command: AGENT_STATUS | Status: ${ResultCode.NO_EFFECT_EXPECTED} | Info: Agent ACTIVE, A11y DISCONNECTED, Core Ready"
            ActionType.APP_LAUNCH -> "Command: APP_LAUNCH | Target: ${command.parameters["appLabel"]} | Status: ${ResultCode.DISPATCHED_BUT_NOT_VERIFIED} | Reason: App launch dispatched without foreground verification"
            else -> "Command: ${command.actionType} | Status: ${ResultCode.CAPABILITY_UNAVAILABLE}"
        }
    }

    private fun logEvent(eventText: String) {
        if (recentEventLogs.size >= maxEventLogSize) {
            recentEventLogs.removeFirst()
        }
        recentEventLogs.addLast("${System.currentTimeMillis() % 1000000}: $eventText")

        binding.tvRecentEvents.text = recentEventLogs.joinToString("\n")
    }

    private fun updateStorageDiagnostics() {
        val app = application as? LocalAgentApplication ?: return
        val session = app.eventLogger.getActiveSession()
        val durableStatus = app.durableStorageManager.getAvailabilityStatus()

        // Asynchronous storage fetch off UI thread
        Thread {
            try {
                val count = app.eventRepository.getEventCount()
                val bytes = app.eventRepository.getStorageFootprintBytes()
                val recentEvents = app.eventRepository.queryEvents(EventFilter(limit = 1))
                val lastEvent = recentEvents.firstOrNull()?.eventType ?: "NONE"

                runOnUiThread {
                    binding.tvStorageDiagnostics.text = "Session: ${session.sessionId.take(8)}... | Events: $count | DB: ${bytes / 1024} KB | Durable Storage: $durableStatus | Latest: $lastEvent"
                }
            } catch (_: Exception) {}
        }.start()
    }
}
