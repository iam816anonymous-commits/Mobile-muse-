package com.localagent.app.system

import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.accessibility.ObservationWindowDiagnostics
import com.localagent.core.observation.ObservationSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

sealed class LaunchVerificationResult {
    data class Success(
        val snapshot: ObservationSnapshot,
        val attempts: Int,
        val elapsedTimeMs: Long,
        val rootNode: AccessibilityNodeInfo
    ) : LaunchVerificationResult()

    data class Timeout(
        val reason: String,
        val targetPackageName: String,
        val attempts: Int,
        val elapsedTimeMs: Long,
        val diagnostics: ObservationWindowDiagnostics
    ) : LaunchVerificationResult()
}

class LaunchVerifier(
    private val accessibilityServiceSupplier: () -> AgentAccessibilityService? = { AgentAccessibilityService.INSTANCE },
    private val rootNodeProvider: (targetPackageName: String) -> AccessibilityNodeInfo? = { pkg ->
        (accessibilityServiceSupplier() ?: AgentAccessibilityService.INSTANCE)?.getLiveExternalRootNode(pkg)
    }
) {

    suspend fun awaitUsableWindowAndSnapshot(
        targetPackageName: String,
        preLaunchGeneration: Long,
        timeoutMs: Long = 5000L,
        pollIntervalMs: Long = 50L,
        settlingDelayMs: Long = 150L
    ): LaunchVerificationResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        var attempts = 0

        while (System.currentTimeMillis() - startTime <= timeoutMs) {
            attempts++
            val service = accessibilityServiceSupplier()
            if (service == null || !AgentAccessibilityService.isBound) {
                val elapsedTime = System.currentTimeMillis() - startTime
                return@withContext LaunchVerificationResult.Timeout(
                    reason = "Accessibility service is unbound or null",
                    targetPackageName = targetPackageName,
                    attempts = attempts,
                    elapsedTimeMs = elapsedTime,
                    diagnostics = ObservationWindowDiagnostics(expectedPackage = targetPackageName)
                )
            }

            // Inspect live root for target package
            val candidateRoot = rootNodeProvider(targetPackageName)
            if (candidateRoot != null) {
                val candidatePkg = candidateRoot.packageName?.toString() ?: ""
                if (candidatePkg == targetPackageName) {
                    // Candidate window and root found! Apply settling delay (~150ms) non-blockingly
                    if (settlingDelayMs > 0) {
                        delay(settlingDelayMs)
                    }

                    // Re-verify live root after settling delay
                    val recheckRoot = rootNodeProvider(targetPackageName)
                    if (recheckRoot != null && (recheckRoot.packageName?.toString() ?: "") == targetPackageName) {
                        candidateRoot.recycle()

                        // Capture fresh snapshot
                        val freshSnapshot = service.getSnapshotForContext(isExternal = true) ?: service.captureLiveSnapshot()
                        val elapsedTime = System.currentTimeMillis() - startTime

                        val isFresh = if (preLaunchGeneration > 0L) {
                            freshSnapshot.generation > preLaunchGeneration && freshSnapshot.packageName == targetPackageName
                        } else {
                            freshSnapshot.packageName == targetPackageName
                        }

                        if (isFresh) {
                            return@withContext LaunchVerificationResult.Success(
                                snapshot = freshSnapshot,
                                attempts = attempts,
                                elapsedTimeMs = elapsedTime,
                                rootNode = recheckRoot
                            )
                        } else {
                            recheckRoot.recycle()
                        }
                    } else {
                        recheckRoot?.recycle()
                        candidateRoot.recycle()
                    }
                } else {
                    candidateRoot.recycle()
                }
            }

            // Non-blocking poll interval delay
            delay(pollIntervalMs)
        }

        val service = accessibilityServiceSupplier()
        val elapsedTime = System.currentTimeMillis() - startTime
        val latestDiag = service?.latestDiagnostics?.copy(
            expectedPackage = targetPackageName,
            refreshAttempts = attempts,
            elapsedTimeMs = elapsedTime,
            rejectionReason = "Target window timeout waiting for usable interactive window and root node for package '$targetPackageName'."
        ) ?: ObservationWindowDiagnostics(
            expectedPackage = targetPackageName,
            refreshAttempts = attempts,
            elapsedTimeMs = elapsedTime,
            rejectionReason = "Target window timeout waiting for usable interactive window and root node for package '$targetPackageName'."
        )

        LaunchVerificationResult.Timeout(
            reason = "Target window timeout waiting for usable interactive window and root node for package '$targetPackageName'.",
            targetPackageName = targetPackageName,
            attempts = attempts,
            elapsedTimeMs = elapsedTime,
            diagnostics = latestDiag
        )
    }
}
