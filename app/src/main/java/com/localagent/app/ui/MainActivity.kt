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
            val input = binding.etCommandInput.text.toString().trim()
            if (input.isNotEmpty()) {
                executeCommandFromInput(input)
            } else {
                binding.tvLatestResult.text = "Error: Command input cannot be empty."
            }
        }

        binding.btnObserve.setOnClickListener {
            triggerObserveAction()
        }
    }

    fun executeCommandFromInput(input: String) {
        val app = application as? LocalAgentApplication
            ?: return

        logEvent("[COMMAND_RECEIVED] Raw Input: '$input'")

        val normalizedCmd = parseCommandSyntax(input)
        if (normalizedCmd == null) {
            val resultText = "Result: ${ResultCode.CAPABILITY_UNAVAILABLE} | Reason: UNSUPPORTED / NOT AVAILABLE IN CURRENT PHASE"
            binding.tvLatestResult.text = resultText
            logEvent("[COMMAND_RESULT] $resultText")
            return
        }

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
            // Simulated execution result for Phase 1 baseline
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

    private fun triggerObserveAction() {
        logEvent("[OBSERVATION_REQUESTED] Source: Test UI")
        val resultText = "Observation Summary: Active Window = com.localagent.app | Node Count = N/A (Phase 5 Observation Engine Required) | Result: ${ResultCode.ACCESSIBILITY_UNAVAILABLE}"
        binding.tvLatestResult.text = resultText
        logEvent("[OBSERVATION_COMPLETED] Result: ${ResultCode.ACCESSIBILITY_UNAVAILABLE}")
    }

    private fun parseCommandSyntax(input: String): NormalizedCommand? {
        val lower = input.lowercase()
        return when {
            lower == "back" -> NormalizedCommand(
                source = CommandSource.CONSOLE,
                actionType = ActionType.GLOBAL_BACK
            )
            lower.startsWith("click ") -> {
                val target = input.substring(6).trim()
                NormalizedCommand(
                    source = CommandSource.CONSOLE,
                    actionType = ActionType.UI_CLICK,
                    targetSelector = TargetSelector.ByText(target)
                )
            }
            lower.startsWith("launch ") -> {
                val appLabel = input.substring(7).trim()
                NormalizedCommand(
                    source = CommandSource.CONSOLE,
                    actionType = ActionType.APP_LAUNCH,
                    parameters = mapOf("appLabel" to appLabel)
                )
            }
            else -> null
        }
    }

    private fun executeNormalizedCommand(command: NormalizedCommand): String {
        return when (command.actionType) {
            ActionType.GLOBAL_BACK -> "Command: GLOBAL_BACK | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
            ActionType.UI_CLICK -> "Command: UI_CLICK | Target: ${command.targetSelector} | Status: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
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
