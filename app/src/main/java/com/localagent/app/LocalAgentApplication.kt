package com.localagent.app

import android.app.Application
import com.localagent.app.accessibility.AccessibilityServiceConnectionMonitor
import com.localagent.app.logging.UnifiedEventLogger
import com.localagent.app.storage.AgentDatabase
import com.localagent.app.storage.DurableMemoryStorageManager
import com.localagent.app.storage.RoomEventRepository
import com.localagent.app.system.PermissionManager
import com.localagent.core.capability.*
import com.localagent.core.execution.GoalDispatcher
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.policy.ActionPolicyEngine
import java.io.File

class LocalAgentApplication : Application() {

    lateinit var capabilityRegistry: CapabilityRegistry
        private set

    lateinit var policyEngine: ActionPolicyEngine
        private set

    lateinit var goalDispatcher: GoalDispatcher
        private set

    lateinit var eventRepository: RoomEventRepository
        private set

    lateinit var eventLogger: UnifiedEventLogger
        private set

    lateinit var durableStorageManager: DurableMemoryStorageManager
        private set

    lateinit var permissionManager: PermissionManager
        private set

    lateinit var accessibilityConnectionMonitor: AccessibilityServiceConnectionMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        initializeCoreDomain()
    }

    // Public method for testing and Activity initialization
    fun initializeCoreDomain() {
        val db = AgentDatabase.getInstance(this)
        val dbFile = File(filesDir, "agent/${AgentDatabase.DATABASE_NAME}")
        eventRepository = RoomEventRepository(db.eventDao(), db.sessionDao(), dbFile)
        eventLogger = UnifiedEventLogger(eventRepository)
        durableStorageManager = DurableMemoryStorageManager(this)
        durableStorageManager.initializeStorage()
        permissionManager = PermissionManager(this)
        accessibilityConnectionMonitor = AccessibilityServiceConnectionMonitor(this)

        capabilityRegistry = CapabilityRegistry().apply {
            registerCapability(
                CapabilityRule(
                    capabilityId = "DURABLE_STORAGE",
                    name = "Durable Long-Term Memory Storage",
                    category = CapabilityCategory.STORAGE,
                    minApi = 27,
                    requiredPermissions = listOf("android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"),
                    requiredSpecialAccess = listOf(SpecialAccessType.STORAGE_ACCESS_FRAMEWORK),
                    verificationStrategy = "ChecksumAndFileExistsStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
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
                    capabilityId = "UI_LONG_CLICK",
                    name = "UI Long Click",
                    category = CapabilityCategory.UI_CONTROL,
                    minApi = 27,
                    verificationStrategy = "FlexibleLongClickStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "UI_SCROLL_FORWARD",
                    name = "UI Scroll Forward",
                    category = CapabilityCategory.UI_CONTROL,
                    minApi = 27,
                    verificationStrategy = "ScrollVerificationStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "UI_SCROLL_BACKWARD",
                    name = "UI Scroll Backward",
                    category = CapabilityCategory.UI_CONTROL,
                    minApi = 27,
                    verificationStrategy = "ScrollVerificationStrategy",
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
                    capabilityId = "GLOBAL_HOME",
                    name = "Global Home",
                    category = CapabilityCategory.NAVIGATION,
                    minApi = 27,
                    verificationStrategy = "LauncherPackageMatchStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "GLOBAL_RECENTS",
                    name = "Global Recents",
                    category = CapabilityCategory.NAVIGATION,
                    minApi = 27,
                    verificationStrategy = "RecentsWindowDiffStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "OBSERVE",
                    name = "UI Observation",
                    category = CapabilityCategory.UI_CONTROL,
                    minApi = 27,
                    verificationStrategy = "SnapshotDiffStrategy",
                    riskLevel = ActionRiskLevel.LOW
                )
            )
            registerCapability(
                CapabilityRule(
                    capabilityId = "AGENT_STATUS",
                    name = "Agent System Status",
                    category = CapabilityCategory.SYSTEM,
                    minApi = 27,
                    verificationStrategy = "StatusReportStrategy",
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
                    riskLevel = ActionRiskLevel.LOW
                )
            )
        }

        policyEngine = ActionPolicyEngine(capabilityRegistry)
        goalDispatcher = GoalDispatcher()

        eventLogger.logEvent(
            AgentEvent(
                eventId = java.util.UUID.randomUUID().toString(),
                sessionId = eventLogger.getActiveSession().sessionId,
                subsystem = EventSubsystem.SYSTEM,
                eventType = "APPLICATION_INITIALIZED",
                severity = EventSeverity.INFO,
                metadataJson = "{\"minSdk\":27,\"targetSdk\":34,\"durableStorage\":\"${durableStorageManager.getAvailabilityStatus()}\"}"
            )
        )
    }
}
