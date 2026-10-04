package com.localagent.app

import android.app.Application
import com.localagent.core.capability.*
import com.localagent.core.execution.GoalDispatcher
import com.localagent.core.policy.ActionPolicyEngine

class LocalAgentApplication : Application() {

    lateinit var capabilityRegistry: CapabilityRegistry
        private set

    lateinit var policyEngine: ActionPolicyEngine
        private set

    lateinit var goalDispatcher: GoalDispatcher
        private set

    override fun onCreate() {
        super.onCreate()
        initializeCoreDomain()
    }

    // Public method for testing and Activity initialization
    fun initializeCoreDomain() {
        capabilityRegistry = CapabilityRegistry().apply {
            registerCapability(
                CapabilityRule(
                    capabilityId = "UI_CLICK",
                    name = "UI Click",
                    category = CapabilityCategory.UI_CONTROL,
                    minApi = 27,
                    verificationStrategy = "TargetClickStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "GLOBAL_BACK",
                    name = "Global Back",
                    category = CapabilityCategory.NAVIGATION,
                    minApi = 27,
                    verificationStrategy = "NavigationAwareBackStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "APP_LAUNCH",
                    name = "App Launch",
                    category = CapabilityCategory.SYSTEM,
                    minApi = 27,
                    verificationStrategy = "PackageMatchStrategy",
                    riskLevel = ActionRiskLevel.HIGH
                )
            )
        }

        policyEngine = ActionPolicyEngine(capabilityRegistry)
        goalDispatcher = GoalDispatcher()
    }
}
