package com.localagent.app.system

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.localagent.app.accessibility.AgentAccessibilityService
import java.util.UUID

enum class PermissionTier {
    NORMAL_MANIFEST,
    RUNTIME_PERMISSION,
    SPECIAL_APP_ACCESS,
    USER_CONSENT_SAF,
    USER_CONSENT_SESSION
}

enum class PermissionStatus {
    GRANTED,
    DENIED,
    SPECIAL_ACCESS_GRANTED,
    SPECIAL_ACCESS_DENIED,
    REQUIRES_USER_INTENT,
    NOT_APPLICABLE
}

data class PermissionDescriptor(
    val id: String,
    val name: String,
    val description: String,
    val tier: PermissionTier,
    val manifestPermission: String? = null,
    val status: PermissionStatus,
    val isGranted: Boolean,
    val dependentCapabilities: List<String>,
    val passiveDegradationSummary: String
)

class PermissionManager(private val context: Context) {

    fun getAllPermissions(): List<PermissionDescriptor> {
        return listOf(
            checkAccessibilityPermission(),
            checkOverlayPermission(),
            checkWriteSettingsPermission(),
            checkUsageAccessPermission(),
            checkNotificationListenerPermission(),
            checkAudioRecordPermission(),
            checkNotificationPostPermission(),
            checkCameraPermission(),
            checkStoragePermission()
        )
    }

    fun checkAccessibilityPermission(): PermissionDescriptor {
        val isBound = AgentAccessibilityService.isBound
        val status = if (isBound) PermissionStatus.SPECIAL_ACCESS_GRANTED else PermissionStatus.SPECIAL_ACCESS_DENIED

        return PermissionDescriptor(
            id = PERABILITY_ACCESSIBILITY,
            name = "Accessibility Service",
            description = "Allows observing UI hierarchy and performing read-only observations",
            tier = PermissionTier.SPECIAL_APP_ACCESS,
            manifestPermission = Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
            status = status,
            isGranted = isBound,
            dependentCapabilities = listOf("UI_OBSERVE", "GLOBAL_BACK", "GLOBAL_HOME", "GLOBAL_RECENTS", "UI_CLICK", "UI_SCROLL"),
            passiveDegradationSummary = "Console UI, Voice STT, EventLogger, and Durable Memory remain 100% active. A11y commands return ACCESSIBILITY_UNAVAILABLE."
        )
    }

    fun checkOverlayPermission(): PermissionDescriptor {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        val status = if (isGranted) PermissionStatus.SPECIAL_ACCESS_GRANTED else PermissionStatus.SPECIAL_ACCESS_DENIED

        return PermissionDescriptor(
            id = PERMISSION_OVERLAY,
            name = "System Overlay Access",
            description = "Allows rendering floating movable overlay controls over other applications",
            tier = PermissionTier.SPECIAL_APP_ACCESS,
            manifestPermission = Manifest.permission.SYSTEM_ALERT_WINDOW,
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("OVERLAY_SHOW", "OVERLAY_MOVE"),
            passiveDegradationSummary = "Movable Overlay surface disabled. Agent controls operate solely through Console UI Activity."
        )
    }

    fun checkWriteSettingsPermission(): PermissionDescriptor {
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }
        val status = if (isGranted) PermissionStatus.SPECIAL_ACCESS_GRANTED else PermissionStatus.SPECIAL_ACCESS_DENIED

        return PermissionDescriptor(
            id = PERMISSION_WRITE_SETTINGS,
            name = "Modify System Settings",
            description = "Allows adjusting system backlight brightness and timeout settings",
            tier = PermissionTier.SPECIAL_APP_ACCESS,
            manifestPermission = Manifest.permission.WRITE_SETTINGS,
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("HARDWARE_BRIGHTNESS"),
            passiveDegradationSummary = "Brightness adjustments fail safely returning PERMISSION_REQUIRED."
        )
    }

    fun checkUsageAccessPermission(): PermissionDescriptor {
        val isGranted = isAppOpsGranted(AppOpsManager.OPSTR_GET_USAGE_STATS)
        val status = if (isGranted) PermissionStatus.SPECIAL_ACCESS_GRANTED else PermissionStatus.SPECIAL_ACCESS_DENIED

        return PermissionDescriptor(
            id = PERMISSION_USAGE_ACCESS,
            name = "Usage Stats Access",
            description = "Allows inspecting foreground application statistics and recent app usage history",
            tier = PermissionTier.SPECIAL_APP_ACCESS,
            manifestPermission = "android.permission.PACKAGE_USAGE_STATS",
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("APP_USAGE_STATS", "FOREGROUND_APP_DETECTION"),
            passiveDegradationSummary = "Foreground package detection relies strictly on passive Accessibility events."
        )
    }

    fun checkNotificationListenerPermission(): PermissionDescriptor {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isGranted = flat != null && flat.contains(context.packageName)
        val status = if (isGranted) PermissionStatus.SPECIAL_ACCESS_GRANTED else PermissionStatus.SPECIAL_ACCESS_DENIED

        return PermissionDescriptor(
            id = PERMISSION_NOTIFICATION_LISTENER,
            name = "Notification Listener Access",
            description = "Allows reading incoming notification events for triggers and workflows",
            tier = PermissionTier.SPECIAL_APP_ACCESS,
            manifestPermission = Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("NOTIFICATION_READ"),
            passiveDegradationSummary = "Notification triggers disabled. Restricted on low-RAM API 27 devices."
        )
    }

    fun checkAudioRecordPermission(): PermissionDescriptor {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED

        return PermissionDescriptor(
            id = PERMISSION_RECORD_AUDIO,
            name = "Microphone Record Audio",
            description = "Allows spoken voice command input via SpeechRecognizer",
            tier = PermissionTier.RUNTIME_PERMISSION,
            manifestPermission = Manifest.permission.RECORD_AUDIO,
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("SPEECH_STT"),
            passiveDegradationSummary = "Speech input disabled. Console keyboard text input remains fully available."
        )
    }

    fun checkNotificationPostPermission(): PermissionDescriptor {
        val isGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.NOT_APPLICABLE

        return PermissionDescriptor(
            id = PERMISSION_POST_NOTIFICATIONS,
            name = "Post Notifications",
            description = "Allows posting system status notifications (API 33+)",
            tier = PermissionTier.RUNTIME_PERMISSION,
            manifestPermission = "android.permission.POST_NOTIFICATIONS",
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("STATUS_NOTIFICATIONS"),
            passiveDegradationSummary = "Status updates displayed inside Console UI log view only."
        )
    }

    fun checkCameraPermission(): PermissionDescriptor {
        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED

        return PermissionDescriptor(
            id = PERMISSION_CAMERA,
            name = "Camera / Torch Control",
            description = "Allows flashlight torch control on API 27 baseline",
            tier = PermissionTier.RUNTIME_PERMISSION,
            manifestPermission = Manifest.permission.CAMERA,
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("HARDWARE_FLASHLIGHT"),
            passiveDegradationSummary = "Flashlight control returns CAPABILITY_UNAVAILABLE."
        )
    }

    fun checkStoragePermission(): PermissionDescriptor {
        val isGranted = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val status = if (isGranted) PermissionStatus.GRANTED else PermissionStatus.DENIED

        return PermissionDescriptor(
            id = PERMISSION_STORAGE,
            name = "Storage / SAF Document Access",
            description = "Allows accessing durable memory and importing external document exports via SAF",
            tier = PermissionTier.RUNTIME_PERMISSION,
            manifestPermission = Manifest.permission.READ_EXTERNAL_STORAGE,
            status = status,
            isGranted = isGranted,
            dependentCapabilities = listOf("DURABLE_STORAGE", "EXTERNAL_KNOWLEDGE_IMPORT"),
            passiveDegradationSummary = "Document import disabled; copy/paste text input remains available."
        )
    }

    fun getSettingsIntentForPermission(permissionId: String): Intent {
        return when (permissionId) {
            PERABILITY_ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            PERMISSION_OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            PERMISSION_WRITE_SETTINGS -> Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            PERMISSION_USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            PERMISSION_NOTIFICATION_LISTENER -> Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        }
    }

    fun getSafDocumentPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
        }
    }

    private fun isAppOpsGranted(opStr: String): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.checkOpNoThrow(opStr, Process.myUid(), context.packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val PERABILITY_ACCESSIBILITY = "PERABILITY_ACCESSIBILITY"
        const val PERMISSION_OVERLAY = "PERMISSION_OVERLAY"
        const val PERMISSION_WRITE_SETTINGS = "PERMISSION_WRITE_SETTINGS"
        const val PERMISSION_USAGE_ACCESS = "PERMISSION_USAGE_ACCESS"
        const val PERMISSION_NOTIFICATION_LISTENER = "PERMISSION_NOTIFICATION_LISTENER"
        const val PERMISSION_RECORD_AUDIO = "PERMISSION_RECORD_AUDIO"
        const val PERMISSION_POST_NOTIFICATIONS = "PERMISSION_POST_NOTIFICATIONS"
        const val PERMISSION_CAMERA = "PERMISSION_CAMERA"
        const val PERMISSION_STORAGE = "PERMISSION_STORAGE"
    }
}
