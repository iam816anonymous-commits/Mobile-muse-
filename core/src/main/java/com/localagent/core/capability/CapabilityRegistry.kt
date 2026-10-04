package com.localagent.core.capability

enum class CapabilityCategory {
    NAVIGATION,
    UI_CONTROL,
    SYSTEM,
    HARDWARE,
    VOICE,
    RESEARCH,
    SOLVER,
    AI
}

enum class ResourceCostTier {
    LOW,
    MEDIUM,
    HIGH
}

enum class ActionRiskLevel {
    LOW,       // Observe, scroll, click non-sensitive elements, open public app
    MEDIUM,    // Toggle Wi-Fi, change brightness, adjust volume, input text
    HIGH,      // Send messages, delete files, modify system settings, grant permissions
    CRITICAL   // Financial purchases, security setting changes, factory reset, credential input
}

enum class SpecialAccessType {
    ACCESSIBILITY,
    SYSTEM_OVERLAY,
    WRITE_SETTINGS,
    USAGE_ACCESS,
    NOTIFICATION_LISTENER
}

enum class PrivilegeLevel {
    NONE,
    USER_CONSENT,
    DEVICE_ADMIN,
    SYSTEM_DEVICE_OWNER,
    ROOT
}

data class CapabilityRule(
    val capabilityId: String,
    val name: String,
    val category: CapabilityCategory,
    val minApi: Int,
    val maxApi: Int = Int.MAX_VALUE,
    val requiredPermissions: List<String> = emptyList(),
    val requiredSpecialAccess: List<SpecialAccessType> = emptyList(),
    val requiredPrivilege: PrivilegeLevel = PrivilegeLevel.NONE,
    val directApiAvailable: Boolean = true,
    val fallbackStrategy: String? = null,
    val verificationStrategy: String,
    val resourceCost: ResourceCostTier = ResourceCostTier.LOW,
    val riskLevel: ActionRiskLevel = ActionRiskLevel.LOW
)

data class CapabilityDescriptor(
    val rule: CapabilityRule,
    val isAvailableOnCurrentDevice: Boolean = true
)

class CapabilityRegistry {
    private val capabilities = mutableMapOf<String, CapabilityRule>()

    fun registerCapability(rule: CapabilityRule) {
        capabilities[rule.capabilityId] = rule
    }

    fun getCapability(capabilityId: String): CapabilityRule? {
        return capabilities[capabilityId]
    }

    fun getAllCapabilities(): List<CapabilityRule> {
        return capabilities.values.toList()
    }

    fun isCapabilitySupported(capabilityId: String, deviceApi: Int): Boolean {
        val rule = capabilities[capabilityId] ?: return false
        return deviceApi in rule.minApi..rule.maxApi
    }
}
