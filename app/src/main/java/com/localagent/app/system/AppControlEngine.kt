package com.localagent.app.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.localagent.app.accessibility.AgentAccessibilityService

sealed class AppResolutionResult {
    data class Success(val packageName: String, val appLabel: String) : AppResolutionResult()
    data class PackageNotInstalled(val query: String, val reason: String) : AppResolutionResult()
    data class NotLaunchable(val packageName: String, val reason: String) : AppResolutionResult()
}

sealed class AppLaunchResult {
    data class Dispatched(val packageName: String) : AppLaunchResult()
    data class AlreadyForeground(val packageName: String) : AppLaunchResult()
    data class LaunchFailed(val packageName: String, val reason: String) : AppLaunchResult()
}

sealed class LaunchVerificationResult {
    data class Success(val packageName: String, val durationMs: Long) : LaunchVerificationResult()
    data class Timeout(val targetPackageName: String, val currentForegroundPackage: String) : LaunchVerificationResult()
    data class AccessibilityUnavailable(val reason: String) : LaunchVerificationResult()
    data class PackageMismatch(val expectedPackageName: String, val actualPackageName: String) : LaunchVerificationResult()
}

class AppResolver(private val context: Context) {

    fun resolvePackage(query: String): AppResolutionResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return AppResolutionResult.PackageNotInstalled(query, "Query cannot be empty")
        }

        val pm = context.packageManager

        // 1. Direct package match attempt
        val directIntent = pm.getLaunchIntentForPackage(trimmed)
        if (directIntent != null) {
            val appLabel = try {
                val appInfo = pm.getApplicationInfo(trimmed, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                trimmed
            }
            return AppResolutionResult.Success(trimmed, appLabel)
        }

        // Common alias mappings
        val knownAlias = when (trimmed.lowercase()) {
            "calculator", "calc" -> listOf("com.transsion.calculator", "com.android.calculator2", "com.google.android.calculator")
            "settings" -> listOf("com.android.settings")
            else -> emptyList()
        }

        for (pkg in knownAlias) {
            val aliasIntent = pm.getLaunchIntentForPackage(pkg)
            if (aliasIntent != null) {
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    pkg
                }
                return AppResolutionResult.Success(pkg, appLabel)
            }
        }

        // 2. Search installed applications by label
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)

        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString()
            val pkgName = info.activityInfo.packageName
            if (label.equals(trimmed, ignoreCase = true) || pkgName.equals(trimmed, ignoreCase = true)) {
                return AppResolutionResult.Success(pkgName, label)
            }
        }

        return AppResolutionResult.PackageNotInstalled(trimmed, "No launchable application found for query '$trimmed'")
    }
}

class AppLauncher(private val context: Context) {

    fun launchApp(packageName: String): AppLaunchResult {
        val service = AgentAccessibilityService.INSTANCE
        if (service != null && service.activePackageName == packageName) {
            return AppLaunchResult.AlreadyForeground(packageName)
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)
            ?: return AppLaunchResult.LaunchFailed(packageName, "No launch intent found for package '$packageName'")

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        return try {
            context.startActivity(launchIntent)
            AppLaunchResult.Dispatched(packageName)
        } catch (e: Exception) {
            AppLaunchResult.LaunchFailed(packageName, e.message ?: "Failed to start activity")
        }
    }
}

class LaunchVerifier(
    private val accessibilityServiceSupplier: () -> AgentAccessibilityService? = { AgentAccessibilityService.INSTANCE }
) {

    fun verifyForeground(targetPackageName: String, timeoutMs: Long = 3000L): LaunchVerificationResult {
        val service = accessibilityServiceSupplier()
        if (service == null || !AgentAccessibilityService.isBound) {
            return LaunchVerificationResult.AccessibilityUnavailable("AccessibilityService is unbound or unavailable")
        }

        val startTime = System.currentTimeMillis()
        val checkIntervalMs = 100L

        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            val currentFg = service.activePackageName
            val lastExt = service.lastExternalPackageName

            // 1. Direct package match on active or last external package
            if (currentFg == targetPackageName || lastExt == targetPackageName) {
                val elapsed = System.currentTimeMillis() - startTime
                return LaunchVerificationResult.Success(targetPackageName, elapsed)
            }

            // 2. Query live interactive windows for direct target package root node
            val targetRoot = service.getLiveExternalRootNode(targetPackageName)
            if (targetRoot != null) {
                val elapsed = System.currentTimeMillis() - startTime
                targetRoot.recycle()
                return LaunchVerificationResult.Success(targetPackageName, elapsed)
            }

            // 3. Inspect available window candidates for matching active application window
            val windows = service.latestDiagnostics.availableWindows
            if (windows.any { it.packageName == targetPackageName && (it.isActive || it.score >= 50) }) {
                val elapsed = System.currentTimeMillis() - startTime
                return LaunchVerificationResult.Success(targetPackageName, elapsed)
            }

            try {
                Thread.sleep(checkIntervalMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }

        // Final check before returning failure
        val finalRoot = service.getLiveExternalRootNode(targetPackageName)
        if (finalRoot != null) {
            finalRoot.recycle()
            val elapsed = System.currentTimeMillis() - startTime
            return LaunchVerificationResult.Success(targetPackageName, elapsed)
        }

        val finalFg = service.activePackageName
        return if (finalFg.isNotBlank() && finalFg != targetPackageName && finalFg != "com.localagent.app" && finalFg != "com.android.systemui") {
            LaunchVerificationResult.PackageMismatch(expectedPackageName = targetPackageName, actualPackageName = finalFg)
        } else {
            LaunchVerificationResult.Timeout(targetPackageName = targetPackageName, currentForegroundPackage = finalFg)
        }
    }
}
