package com.localagent.app.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
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
    data class Timeout(
        val targetPackageName: String,
        val currentForegroundPackage: String,
        val detailedReason: String = "Target window timeout waiting for package '$targetPackageName'"
    ) : LaunchVerificationResult()
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

        // 0. If query is a home / launcher alias, prioritize resolving the actual default Home activity
        val isLauncherQuery = when (trimmed.lowercase()) {
            "hilauncher", "launcher", "home launcher", "home" -> true
            else -> false
        }
        if (isLauncherQuery) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val defaultHome = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val resolvedPkg = defaultHome?.activityInfo?.packageName
            if (!resolvedPkg.isNullOrBlank()) {
                val appLabel = try {
                    val appInfo = pm.getApplicationInfo(resolvedPkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) {
                    resolvedPkg
                }
                return AppResolutionResult.Success(resolvedPkg, appLabel)
            }
        }

        // Common alias mappings
        val knownAlias = when (trimmed.lowercase()) {
            "calculator", "calc" -> listOf("com.transsion.calculator", "com.android.calculator2", "com.google.android.calculator")
            "settings" -> listOf("com.android.settings")
            "hilauncher", "launcher", "home launcher", "home" -> listOf("com.transsion.hilauncher", "com.transsion.hilauncher.v2", "com.transsion.hilauncher3", "com.google.android.apps.nexuslauncher", "com.android.launcher3")
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
            // Check if package exists as a Home / Launcher Activity
            val homeCheckIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                setPackage(pkg)
            }
            if (pm.queryIntentActivities(homeCheckIntent, 0).isNotEmpty()) {
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
        var launchIntent = pm.getLaunchIntentForPackage(packageName)

        if (launchIntent == null) {
            // Check if this package is a Launcher / Home package
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (pm.queryIntentActivities(homeIntent, 0).isNotEmpty()) {
                launchIntent = homeIntent
            }
        }

        if (launchIntent == null) {
            return AppLaunchResult.LaunchFailed(packageName, "No launch intent found for package '$packageName'")
        }

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
    private val context: Context? = null,
    private val accessibilityServiceSupplier: () -> AgentAccessibilityService? = { AgentAccessibilityService.INSTANCE },
    private val rootNodeProvider: (targetPackageName: String) -> AccessibilityNodeInfo? = { pkg ->
        (accessibilityServiceSupplier() ?: AgentAccessibilityService.INSTANCE)?.getLiveExternalRootNode(pkg)
    }
) {

    private fun isPackageMatch(actualPkg: String, expectedPkg: String): Boolean {
        if (actualPkg.isBlank() || expectedPkg.isBlank()) return false
        if (actualPkg == expectedPkg) return true
        if (actualPkg == "com.android.systemui" || actualPkg == "com.localagent.app") return false

        val ctx = context ?: (accessibilityServiceSupplier() ?: AgentAccessibilityService.INSTANCE)?.applicationContext
        if (ctx != null) {
            try {
                val pm = ctx.packageManager
                val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val homeActivities = pm.queryIntentActivities(homeIntent, 0)
                val homePackages = homeActivities.mapNotNull { it.activityInfo?.packageName }.toSet()
                if (homePackages.contains(expectedPkg) && homePackages.contains(actualPkg)) {
                    return true
                }
            } catch (_: Exception) {
            }
        }

        return false
    }

    fun awaitUsableWindowAndSnapshot(
        targetPackageName: String,
        minGeneration: Long = 0L,
        timeoutMs: Long = 5000L,
        settleDelayMs: Long = 150L
    ): com.localagent.core.observation.ObservationSnapshot? {
        val service = accessibilityServiceSupplier() ?: return null
        if (!AgentAccessibilityService.isBound) return null

        val startTime = System.currentTimeMillis()
        val pollIntervalMs = 100L

        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            val root = rootNodeProvider(targetPackageName)
            if (root != null) {
                val rootPkg = root.packageName?.toString() ?: ""
                root.recycle()

                if (isPackageMatch(rootPkg, targetPackageName)) {
                    // Small controlled settling interval to allow hierarchy stabilization
                    if (settleDelayMs > 0) {
                        try {
                            Thread.sleep(settleDelayMs)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                        }
                    }

                    // Re-verify root availability after settling
                    val settledRoot = rootNodeProvider(targetPackageName)
                    if (settledRoot != null) {
                        settledRoot.recycle()
                        service.captureLiveSnapshot(targetPackageName)
                        val candidateSnapshot = service.getSnapshotForContext(isExternal = true)
                        if (candidateSnapshot != null &&
                            isPackageMatch(candidateSnapshot.packageName, targetPackageName) &&
                            candidateSnapshot.generation > minGeneration &&
                            candidateSnapshot.nodeCount > 0
                        ) {
                            return candidateSnapshot
                        }
                    }
                }
            }

            try {
                Thread.sleep(pollIntervalMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }

        // Final attempt
        val finalRoot = rootNodeProvider(targetPackageName)
        if (finalRoot != null) {
            finalRoot.recycle()
            service.captureLiveSnapshot(targetPackageName)
            val candidateSnapshot = service.getSnapshotForContext(isExternal = true)
            if (candidateSnapshot != null &&
                isPackageMatch(candidateSnapshot.packageName, targetPackageName) &&
                candidateSnapshot.generation > minGeneration &&
                candidateSnapshot.nodeCount > 0
            ) {
                return candidateSnapshot
            }
        }

        return null
    }

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

            // 1. Package match on active or last external package
            if (isPackageMatch(currentFg, targetPackageName) || isPackageMatch(lastExt, targetPackageName)) {
                val elapsed = System.currentTimeMillis() - startTime
                return LaunchVerificationResult.Success(targetPackageName, elapsed)
            }

            // 2. Query live interactive windows for target package root node
            val targetRoot = service.getLiveExternalRootNode(targetPackageName)
            if (targetRoot != null) {
                val elapsed = System.currentTimeMillis() - startTime
                targetRoot.recycle()
                return LaunchVerificationResult.Success(targetPackageName, elapsed)
            }

            // 3. Inspect available window candidates for matching active application window
            val windows = service.latestDiagnostics.availableWindows
            if (windows.any { isPackageMatch(it.packageName, targetPackageName) && (it.isActive || it.score >= 50) }) {
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
        return if (finalFg.isNotBlank() && !isPackageMatch(finalFg, targetPackageName) && finalFg != "com.localagent.app" && finalFg != "com.android.systemui") {
            LaunchVerificationResult.PackageMismatch(expectedPackageName = targetPackageName, actualPackageName = finalFg)
        } else {
            LaunchVerificationResult.Timeout(targetPackageName = targetPackageName, currentForegroundPackage = finalFg)
        }
    }
}
