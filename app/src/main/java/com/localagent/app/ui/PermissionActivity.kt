package com.localagent.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.databinding.ActivityPermissionBinding
import com.localagent.app.system.PermissionCategory
import com.localagent.app.system.PermissionDescriptor
import com.localagent.app.system.PermissionManager
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSubsystem
import java.util.UUID

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        refreshPermissionCenterUi()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionCenterUi()
    }

    private fun setupListeners() {
        val app = application as? LocalAgentApplication
        val pm = app?.permissionManager ?: PermissionManager(this)

        binding.btnOpenAccessibilitySettings.setOnClickListener {
            launchSettingsFlow(PermissionManager.PERABILITY_ACCESSIBILITY, pm)
        }

        binding.btnLaunchSafPicker.setOnClickListener {
            try {
                val safIntent = pm.getSafDocumentPickerIntent()
                startActivityForResult(safIntent, REQUEST_CODE_SAF_PICKER)

                app?.eventLogger?.logEvent(
                    AgentEvent(
                        eventId = UUID.randomUUID().toString(),
                        sessionId = app.eventLogger.getActiveSession().sessionId,
                        subsystem = EventSubsystem.PERMISSION,
                        eventType = "SAF_PICKER_LAUNCHED",
                        sourceChannel = "PERMISSION_CENTER"
                    )
                )
            } catch (e: Exception) {
                Toast.makeText(this, "SAF Picker launch failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchSettingsFlow(permissionId: String, pm: PermissionManager) {
        val app = application as? LocalAgentApplication
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""
        try {
            val intent = pm.getSettingsIntentForPermission(permissionId)
            startActivity(intent)

            app?.eventLogger?.logEvent(
                AgentEvent(
                    eventId = UUID.randomUUID().toString(),
                    sessionId = activeSessionId,
                    subsystem = EventSubsystem.PERMISSION,
                    eventType = "PERMISSION_INTENT_LAUNCHED",
                    sourceChannel = "PERMISSION_CENTER",
                    metadataJson = "{\"permissionId\":\"$permissionId\"}"
                )
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Error launching Settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshPermissionCenterUi() {
        val app = application as? LocalAgentApplication
        val pm = app?.permissionManager ?: PermissionManager(this)

        val a11y = pm.checkAccessibilityPermission()
        val storage = pm.checkStoragePermission()
        val overlay = pm.checkOverlayPermission()
        val writeSettings = pm.checkWriteSettingsPermission()
        val usageAccess = pm.checkUsageAccessPermission()
        val notifListener = pm.checkNotificationListenerPermission()
        val audio = pm.checkAudioRecordPermission()
        val postNotif = pm.checkNotificationPostPermission()
        val camera = pm.checkCameraPermission()

        val requiredCount = if (a11y.isGranted) "1/1 READY" else "0/1 ACTION REQUIRED"
        val futureCount = "7 ITEMS INVENTORY ONLY"
        binding.tvPermissionSummary.text = "REQUIRED NOW: $requiredCount | OPTIONAL NOW: SAF ACTIVE | FUTURE PHASE: $futureCount"

        binding.tvAccessibilityDetails.text = formatDetails(a11y)
        binding.tvStorageDetails.text = formatDetails(storage)
        binding.tvOverlayDetails.text = formatDetails(overlay)
        binding.tvWriteSettingsDetails.text = formatDetails(writeSettings)
        binding.tvUsageAccessDetails.text = formatDetails(usageAccess)
        binding.tvNotificationListenerDetails.text = formatDetails(notifListener)
        binding.tvAudioRecordDetails.text = formatDetails(audio)
        binding.tvPostNotificationsDetails.text = formatDetails(postNotif)
        binding.tvCameraDetails.text = formatDetails(camera)

        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.PERMISSION,
                eventType = "PERMISSION_CHECKED",
                sourceChannel = "PERMISSION_CENTER",
                metadataJson = "{\"a11yBound\":${a11y.isGranted},\"storageGranted\":${storage.isGranted},\"futurePermissionsCount\":7}"
            )
        )
    }

    private fun formatDetails(desc: PermissionDescriptor): String {
        val statusLabel = when (desc.category) {
            PermissionCategory.REQUIRED_NOW -> if (desc.isGranted) "AVAILABLE / GRANTED (${desc.status})" else "ACTION REQUIRED — SERVICE_UNBOUND (${desc.status})"
            PermissionCategory.AVAILABLE_OPTIONAL_NOW -> if (desc.isGranted) "GRANTED (${desc.status})" else "OPTIONAL — NOT GRANTED (${desc.status})"
            PermissionCategory.FUTURE_PHASE -> "NOT CURRENTLY REQUIRED — INVENTORY ONLY (${desc.status})"
        }

        val capsStr = "Target Phase: ${desc.targetPhase} | Capabilities: ${desc.dependentCapabilities.joinToString(", ")}"
        val passiveStr = "Operational Impact: ${desc.passiveDegradationSummary}"
        return "STATUS: $statusLabel\n$capsStr\n$passiveStr"
    }

    companion object {
        const val REQUEST_CODE_SAF_PICKER = 1002
    }
}
