package com.localagent.app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.databinding.ActivityMainBinding
import com.localagent.core.command.*
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventFilter
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.policy.PolicyEvaluationResult
import com.localagent.core.result.ResultCode
import com.localagent.core.storage.DurableMemoryCategory
import com.localagent.core.storage.DurableRecord
import java.util.LinkedList
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val commandNormalizer = CommandNormalizer()
    private val maxEventLogSize = 25
    private val recentEventLogs = LinkedList<String>()
    private var lastWriteStatus: String = "NONE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        hydratePersistedEventHistory()
        logEvent("[SYSTEM] Foundation Test UI Ready. Target API: 27 Baseline.")
        updateStorageDiagnostics()
    }

    override fun onResume() {
        super.onResume()
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

        binding.btnEnableStorage.setOnClickListener {
            requestDurableStorageAccess()
        }

        binding.btnTestWriteMemory.setOnClickListener {
            testWriteDurableMemoryRecord()
        }

        binding.btnOpenAccessibilitySettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                logEvent("[ACCESSIBILITY] Error opening Settings: ${e.message}")
            }
        }
    }

    private fun testWriteDurableMemoryRecord() {
        val app = application as? LocalAgentApplication ?: return
        val record = DurableRecord(
            recordId = "MEMORY_TEST_001",
            category = DurableMemoryCategory.LEARNING_ARTIFACT,
            version = 1,
            payloadJson = "{\"test\":true,\"createdMs\":${System.currentTimeMillis()},\"author\":\"diagnostic_ui\"}",
            metadataJson = "{\"origin\":\"testWriteDurableMemoryRecord\"}"
        )

        val success = app.durableStorageManager.writeRecord(record)
        if (success) {
            val readBack = app.durableStorageManager.readRecord("MEMORY_TEST_001")
            if (readBack != null) {
                lastWriteStatus = "SUCCESS (MEMORY_TEST_001 written & SHA-256 verified)"
                logEvent("[DURABLE_STORAGE] Successfully wrote & verified MEMORY_TEST_001.json")
            } else {
                lastWriteStatus = "FAILED (Integrity readback failed)"
                logEvent("[DURABLE_STORAGE] Integrity readback failed for MEMORY_TEST_001")
            }
        } else {
            lastWriteStatus = "FAILED (Write error to backend)"
            logEvent("[DURABLE_STORAGE] Write error writing MEMORY_TEST_001")
        }
        updateStorageDiagnostics()
    }

    private fun requestDurableStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            try {
                startActivityForResult(intent, 1001)
            } catch (e: Exception) {
                logEvent("[STORAGE_PERMISSION] SAF document tree intent launch error: ${e.message}")
            }
        } else {
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, 1002)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: Exception) {}
                val prefs = getSharedPreferences("agent_storage_prefs", MODE_PRIVATE)
                prefs.edit().putString("saf_memory_uri", uri.toString()).apply()
                logEvent("[STORAGE_GRANTED] SAF memory URI persisted: $uri")
                updateStorageDiagnostics()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002) {
            val granted = grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }
            if (granted) {
                logEvent("[STORAGE_GRANTED] Runtime storage permissions granted")
            } else {
                logEvent("[STORAGE_DENIED] Runtime storage permissions denied")
            }
            updateStorageDiagnostics()
        }
    }

    private fun hydratePersistedEventHistory() {
        val app = application as? LocalAgentApplication ?: return
        try {
            val pastEvents = app.eventRepository.queryEvents(EventFilter(limit = maxEventLogSize - 1))
            recentEventLogs.clear()
            pastEvents.reversed().forEach { event ->
                val text = "[${event.subsystem}] ${event.eventType}${if (event.actionType != null) " " + event.actionType else ""}"
                recentEventLogs.addLast("${event.timestamp % 1000000}: $text")
            }
        } catch (e: Exception) {
            System.err.println("MainActivity: Error hydrating event history: ${e.message}")
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

    private fun logEvent(eventText: String) {
        if (recentEventLogs.size >= maxEventLogSize) {
            recentEventLogs.removeFirst()
        }
        recentEventLogs.addLast("${System.currentTimeMillis() % 1000000}: $eventText")

        binding.tvRecentEvents.text = recentEventLogs.joinToString("\n")
        binding.scrollEventLogContainer.post {
            binding.scrollEventLogContainer.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun updateStorageDiagnostics() {
        val app = application as? LocalAgentApplication ?: return
        val session = app.eventLogger.getActiveSession()
        val durableStatus = app.durableStorageManager.getAvailabilityStatus()
        val a11yBound = AgentAccessibilityService.isBound
        val activePkg = AgentAccessibilityService.INSTANCE?.activePackageName ?: "None"

        val currentSnap = AgentAccessibilityService.INSTANCE?.currentObservationSnapshot
        val externalSnap = AgentAccessibilityService.INSTANCE?.lastExternalObservationSnapshot

        val currentSnapText = if (currentSnap != null) {
            "Pkg: ${currentSnap.packageName} | Nodes: ${currentSnap.nodeCount} | Depth: ${currentSnap.truncationInfo.maxDepthReached} | Truncated: ${currentSnap.truncationInfo.isTruncated}"
        } else {
            "None"
        }

        val externalSnapText = if (externalSnap != null) {
            "Pkg: ${externalSnap.packageName} | Nodes: ${externalSnap.nodeCount} | Depth: ${externalSnap.truncationInfo.maxDepthReached} | Truncated: ${externalSnap.truncationInfo.isTruncated}"
        } else {
            "None"
        }

        // Asynchronous storage fetch off UI thread
        Thread {
            try {
                val count = app.eventRepository.getEventCount()
                val bytes = app.eventRepository.getStorageFootprintBytes()
                val recentEvents = app.eventRepository.queryEvents(EventFilter(limit = 1))
                val lastEvent = recentEvents.firstOrNull()?.eventType ?: "NONE"
                val durableRecordsCount = app.durableStorageManager.listRecords().size

                runOnUiThread {
                    binding.tvAccessibilityStatus.text = "Status: ${if (a11yBound) "READY (BOUND)" else "SERVICE_UNBOUND"} | Active Pkg: $activePkg"
                    binding.tvCurrentObservationDiagnostics.text = "Current Snapshot: $currentSnapText"
                    binding.tvLastExternalObservationDiagnostics.text = "Last External Snapshot: $externalSnapText"
                    binding.tvEventStorageDiagnostics.text = "DB: agent.db | Location: APP_PRIVATE | Session: ${session.sessionId.take(8)}... | Events: $count | Size: ${bytes / 1024} KB | Latest: $lastEvent"
                    binding.tvDurableMemoryDiagnostics.text = "Location: /sdcard/LocalAgent/memory/ | Status: $durableStatus | Records: $durableRecordsCount | Last Write: $lastWriteStatus"
                }
            } catch (_: Exception) {}
        }.start()
    }
}
