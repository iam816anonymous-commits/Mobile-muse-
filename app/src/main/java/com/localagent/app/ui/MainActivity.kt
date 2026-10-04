package com.localagent.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.databinding.ActivityMainBinding
import com.localagent.core.command.*
import com.localagent.core.policy.PolicyEvaluationResult
import com.localagent.core.result.ResultCode
import java.util.LinkedList

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

        logEvent("[COMMAND_RECEIVED] Raw Input: '$input'")

        when (val parseResult = commandNormalizer.parseInput(input, CommandSource.CONSOLE)) {
            is CommandParseResult.InvalidInput -> {
                val resultText = "Result: ${ResultCode.INVALID_INPUT} | Reason: ${parseResult.reason}"
                binding.tvLatestResult.text = resultText
                logEvent("[COMMAND_REJECTED] $resultText")
                return
            }
            is CommandParseResult.UnknownCommand -> {
                val resultText = "Result: ${ResultCode.UNKNOWN_COMMAND} | Reason: Unrecognized command syntax '${parseResult.rawInput}'"
                binding.tvLatestResult.text = resultText
                logEvent("[COMMAND_REJECTED] $resultText")
                return
            }
            is CommandParseResult.Success -> {
                val normalizedCmd = parseResult.command
                logEvent("[COMMAND_PARSED] Action: ${normalizedCmd.actionType} | Target: ${normalizedCmd.targetSelector}")

                // Policy Evaluation
                val policyResult = app.policyEngine.evaluateCommand(normalizedCmd)
                if (policyResult is PolicyEvaluationResult.UserConfirmationRequired) {
                    val resultText = "Result: ${ResultCode.POLICY_BLOCKED} | ${policyResult.explanation}"
                    binding.tvLatestResult.text = resultText
                    logEvent("[POLICY_EVALUATED] $resultText")
                    return
                }

                // Dispatch through Universal Production Pipeline
                app.goalDispatcher.enqueueCommand(normalizedCmd, priority = 1)
                val polled = app.goalDispatcher.pollNextCommandForExecution()

                if (polled != null) {
                    val executionResult = executeNormalizedCommand(polled.command)
                    binding.tvLatestResult.text = executionResult
                    logEvent("[COMMAND_RESULT] $executionResult")
                    app.goalDispatcher.completeExecution()
                } else {
                    val busyText = "Result: ${ResultCode.TIMEOUT} | Execution channel locked by concurrent transaction"
                    binding.tvLatestResult.text = busyText
                    logEvent("[EXECUTION_LOCK] $busyText")
                }
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
}
