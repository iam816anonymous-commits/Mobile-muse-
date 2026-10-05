package com.localagent.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.localagent.app.LocalAgentApplication
import com.localagent.app.databinding.ActivityPermissionBinding
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

        binding.btnOpenOverlaySettings.setOnClickListener {
            launchSettingsFlow(PermissionManager.PERMISSION_OVERLAY, pm)
        }

        binding.btnOpenWriteSettings.setOnClickListener {
            launchSettingsFlow(PermissionManager.PERMISSION_WRITE_SETTINGS, pm)
        }

        binding.btnOpenUsageAccessSettings.setOnClickListener {
            launchSettingsFlow(PermissionManager.PERMISSION_USAGE_ACCESS, pm)
        }

        binding.btnOpenNotificationListenerSettings.setOnClickListener {
            launchSettingsFlow(PermissionManager.PERMISSION_NOTIFICATION_LISTENER, pm)
        }

        binding.btnRequestAudioRecord.setOnClickListener {
            val audioDesc = pm.checkAudioRecordPermission()
            if (!audioDesc.isGranted && audioDesc.manifestPermission != null) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(audioDesc.manifestPermission),
                    REQUEST_CODE_AUDIO_PERM
                )
            } else {
                Toast.makeText(this, "Microphone permission already granted.", Toast.LENGTH_SHORT).show()
            }
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
            try {
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                startActivity(fallbackIntent)
            } catch (fallbackEx: Exception) {
                Toast.makeText(this, "Error launching Settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshPermissionCenterUi() {
        val app = application as? LocalAgentApplication
        val pm = app?.permissionManager ?: PermissionManager(this)

        val a11y = pm.checkAccessibilityPermission()
        val overlay = pm.checkOverlayPermission()
        val writeSettings = pm.checkWriteSettingsPermission()
        val usageAccess = pm.checkUsageAccessPermission()
        val notifListener = pm.checkNotificationListenerPermission()
        val audio = pm.checkAudioRecordPermission()
        val storage = pm.checkStoragePermission()

        val totalSpecial = listOf(a11y, overlay, writeSettings, usageAccess, notifListener).count { it.isGranted }
        binding.tvPermissionSummary.text = "Special Access: $totalSpecial/5 GRANTED | Mic: ${if (audio.isGranted) "GRANTED" else "DENIED"} | SAF: ACTIVE"

        binding.tvAccessibilityDetails.text = formatDetails(a11y)
        binding.tvOverlayDetails.text = formatDetails(overlay)
        binding.tvWriteSettingsDetails.text = formatDetails(writeSettings)
        binding.tvUsageAccessDetails.text = formatDetails(usageAccess)
        binding.tvNotificationListenerDetails.text = formatDetails(notifListener)
        binding.tvAudioRecordDetails.text = formatDetails(audio)
        binding.tvStorageDetails.text = formatDetails(storage)

        // Log permission status check
        val activeSessionId = app?.eventLogger?.getActiveSession()?.sessionId ?: ""
        app?.eventLogger?.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.PERMISSION,
                eventType = "PERMISSION_CHECKED",
                sourceChannel = "PERMISSION_CENTER",
                metadataJson = "{\"specialGranted\":$totalSpecial,\"a11yBound\":${a11y.isGranted},\"overlay\":${overlay.isGranted},\"mic\":${audio.isGranted}}"
            )
        )
    }

    private fun formatDetails(desc: PermissionDescriptor): String {
        val statusStr = if (desc.isGranted) "STATUS: GRANTED (${desc.status})" else "STATUS: DENIED (${desc.status})"
        val capsStr = "Dependent Capabilities: ${desc.dependentCapabilities.joinToString(", ")}"
        val passiveStr = "Passive Degradation: ${desc.passiveDegradationSummary}"
        return "$statusStr\n$capsStr\n$passiveStr"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_AUDIO_PERM) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Toast.makeText(this, "Microphone permission ${if (granted) "GRANTED" else "DENIED"}", Toast.LENGTH_SHORT).show()
            refreshPermissionCenterUi()
        }
    }

    companion object {
        const val REQUEST_CODE_AUDIO_PERM = 1001
        const val REQUEST_CODE_SAF_PICKER = 1002
    }
}
