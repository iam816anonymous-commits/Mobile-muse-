package com.localagent.app.system

import android.content.Context
import android.content.Intent

class AppLauncher(private val context: Context) {

    fun launchApp(packageName: String): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return false

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        return try {
            context.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            System.err.println("AppLauncher: Failed to launch $packageName: ${e.message}")
            false
        }
    }
}
