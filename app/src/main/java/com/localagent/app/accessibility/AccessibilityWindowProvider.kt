package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo

data class WindowNodeCandidate(
    val candidate: WindowCandidateInfo,
    val node: AccessibilityNodeInfo?
)

interface AccessibilityWindowProvider {
    fun getAvailableWindows(): List<WindowCandidateInfo>
    fun getRootForPackage(packageName: String): AccessibilityNodeInfo?
}

class DefaultAccessibilityWindowProvider(
    private val windowCandidatesSupplier: () -> List<WindowNodeCandidate>
) : AccessibilityWindowProvider {

    override fun getAvailableWindows(): List<WindowCandidateInfo> {
        return windowCandidatesSupplier().map { it.candidate }
    }

    override fun getRootForPackage(packageName: String): AccessibilityNodeInfo? {
        if (packageName.isBlank()) return null
        val candidates = windowCandidatesSupplier()
        val directMatch = candidates.firstOrNull { it.candidate.packageName == packageName && it.node != null }
        return if (directMatch?.node != null) {
            AccessibilityNodeInfo.obtain(directMatch.node)
        } else {
            null
        }
    }
}
