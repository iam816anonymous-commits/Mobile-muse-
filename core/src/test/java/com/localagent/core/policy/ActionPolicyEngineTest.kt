package com.localagent.core.policy

import com.localagent.core.capability.*
import com.localagent.core.command.ActionType
import com.localagent.core.command.CommandSource
import com.localagent.core.command.NormalizedCommand
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionPolicyEngineTest {

    private lateinit var registry: CapabilityRegistry
    private lateinit var policyEngine: ActionPolicyEngine

    @Before
    fun setUp() {
        registry = CapabilityRegistry()
        policyEngine = ActionPolicyEngine(registry)

        registry.registerCapability(
            CapabilityRule(
                capabilityId = "UI_CLICK",
                name = "Click",
                category = CapabilityCategory.UI_CONTROL,
                minApi = 27,
                verificationStrategy = "TargetClickStrategy",
                riskLevel = ActionRiskLevel.LOW
            )
        )

        registry.registerCapability(
            CapabilityRule(
                capabilityId = "APP_LAUNCH",
                name = "Launch App",
                category = CapabilityCategory.SYSTEM,
                minApi = 27,
                verificationStrategy = "PackageMatchStrategy",
                riskLevel = ActionRiskLevel.LOW
            )
        )

        registry.registerCapability(
            CapabilityRule(
                capabilityId = "HARDWARE_RESET",
                name = "Hardware Reset",
                category = CapabilityCategory.SYSTEM,
                minApi = 27,
                verificationStrategy = "SystemResetStrategy",
                riskLevel = ActionRiskLevel.HIGH
            )
        )
    }

    @Test
    fun testLowRiskActionApprovedAutomatically() {
        val command = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = ActionType.UI_CLICK
        )

        val result = policyEngine.evaluateCommand(command, isUserConfirmed = false)
        assertTrue(result is PolicyEvaluationResult.Approved)
    }

    @Test
    fun testAppLaunchLowRiskApprovedAutomatically() {
        val command = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = ActionType.APP_LAUNCH
        )

        val result = policyEngine.evaluateCommand(command, isUserConfirmed = false)
        assertTrue(result is PolicyEvaluationResult.Approved)
    }

    @Test
    fun testHighRiskActionRequiresUserConfirmation() {
        val highRiskCommand = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = ActionType.AGENT_STATUS
        )

        registry.registerCapability(
            CapabilityRule(
                capabilityId = "AGENT_STATUS",
                name = "Status",
                category = CapabilityCategory.SYSTEM,
                minApi = 27,
                verificationStrategy = "StatusStrategy",
                riskLevel = ActionRiskLevel.HIGH
            )
        )

        val unconfirmedResult = policyEngine.evaluateCommand(highRiskCommand, isUserConfirmed = false)
        assertTrue(unconfirmedResult is PolicyEvaluationResult.UserConfirmationRequired)
        assertEquals(ActionRiskLevel.HIGH, unconfirmedResult.riskLevel)

        val confirmedResult = policyEngine.evaluateCommand(highRiskCommand, isUserConfirmed = true)
        assertTrue(confirmedResult is PolicyEvaluationResult.Approved)
    }
}
