package com.localagent.app.system

import android.content.Context
import android.content.Intent

class AppResolver(private val context: Context) {

    fun resolvePackageName(appLabel: String): String? {
        val trimmed = appLabel.trim()
        if (trimmed.isEmpty()) return null

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val lowerLabel = trimmed.lowercase()

        // 1. Exact label match (case-insensitive)
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().trim().lowercase()
            if (label == lowerLabel) {
                return info.activityInfo.packageName
            }
        }

        // 2. Contains match or package name match
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().trim().lowercase()
            val pkg = info.activityInfo.packageName.lowercase()
            if (label.contains(lowerLabel) || pkg.contains(lowerLabel)) {
                return info.activityInfo.packageName
            }
        }

        // 3. Fallback for common target aliases
        if (lowerLabel.contains("calculator")) {
            val calcMatch = resolveInfos.firstOrNull {
                it.activityInfo.packageName.lowercase().contains("calculator")
            }
            if (calcMatch != null) {
                return calcMatch.activityInfo.packageName
            }
        }

        // 4. Direct package string check if appLabel looks like a package (e.g. com.transsion.calculator)
        if (trimmed.contains(".")) {
            return trimmed
        }

        return null
    }
}
