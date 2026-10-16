package com.localagent.core.policy

import com.localagent.core.capability.ActionRiskLevel
import com.localagent.core.capability.CapabilityRegistry
import com.localagent.core.command.NormalizedCommand

sealed class PolicyEvaluationResult {
    object Approved : PolicyEvaluationResult()
    data class UserConfirmationRequired(val riskLevel: ActionRiskLevel, val explanation: String) : PolicyEvaluationResult()
    data class Blocked(val reason: String) : PolicyEvaluationResult()
}

class ActionPolicyEngine(private val capabilityRegistry: CapabilityRegistry) {

    fun evaluateCommand(command: NormalizedCommand, isUserConfirmed: Boolean = false): PolicyEvaluationResult {
        val capability = capabilityRegistry.getCapability(command.actionType.name)
            ?: return PolicyEvaluationResult.Approved // Default to approved if capability not explicitly registered

        return when (capability.riskLevel) {
            ActionRiskLevel.LOW -> PolicyEvaluationResult.Approved
            ActionRiskLevel.MEDIUM -> PolicyEvaluationResult.Approved
            ActionRiskLevel.HIGH -> {
                if (isUserConfirmed) {
                    PolicyEvaluationResult.Approved
                } else {
                    PolicyEvaluationResult.UserConfirmationRequired(
                        riskLevel = ActionRiskLevel.HIGH,
                        explanation = "Action '${command.actionType}' carries HIGH risk and requires explicit user confirmation."
                    )
                }
            }
            ActionRiskLevel.CRITICAL -> {
                if (isUserConfirmed) {
                    PolicyEvaluationResult.Approved
                } else {
                    PolicyEvaluationResult.UserConfirmationRequired(
                        riskLevel = ActionRiskLevel.CRITICAL,
                        explanation = "Action '${command.actionType}' carries CRITICAL risk and requires strict explicit user authentication."
                    )
                }
            }
        }
    }
}
