package com.localagent.app.accessibility

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.localagent.app.LocalAgentApplication
import com.localagent.core.action.ActionEvidence
import com.localagent.core.action.ActionExecutionResult
import com.localagent.core.action.ActionRequest
import com.localagent.core.action.ActionType
import com.localagent.core.action.TargetAwareVerificationStrategy
import com.localagent.core.action.VerificationResult
import com.localagent.core.action.VerificationStatus
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.resolver.TargetActionType
import com.localagent.core.resolver.TargetResolutionResult
import com.localagent.core.resolver.TargetResolutionStatus
import com.localagent.core.resolver.TargetResolver
import com.localagent.core.result.ResultCode
import java.util.UUID

class UiActionExecutor(
    private val accessibilityService: AgentAccessibilityService? = AgentAccessibilityService.INSTANCE,
    private val targetResolver: TargetResolver = TargetResolver(),
    private val liveTargetResolver: LiveTargetResolver = LiveTargetResolver(),
    private val verificationStrategy: TargetAwareVerificationStrategy = TargetAwareVerificationStrategy(),
    private val resolutionLogger: TargetResolutionLogger? = null,
    private val liveRootNodeProvider: (targetPackageName: String?) -> AccessibilityNodeInfo? = { targetPkg ->
        val service = accessibilityService ?: AgentAccessibilityService.INSTANCE
        val externalRoot = service?.getLiveExternalRootNode(targetPkg)
        if (externalRoot != null) {
            externalRoot
        } else {
            val activeRoot = service?.rootInActiveWindow
            if (activeRoot != null) {
                val activePkg = activeRoot.packageName?.toString() ?: ""
                if (!targetPkg.isNullOrBlank() && activePkg == targetPkg) {
                    activeRoot
                } else if (targetPkg.isNullOrBlank() && service?.isValidExternalApplicationPackage(activePkg) == true) {
                    activeRoot
                } else {
                    activeRoot.recycle()
                    null
                }
            } else {
                null
            }
        }
    },
    private val snapshotProvider: () -> ObservationSnapshot? = { (accessibilityService ?: AgentAccessibilityService.INSTANCE)?.getSnapshotForContext(isExternal = true) ?: (accessibilityService ?: AgentAccessibilityService.INSTANCE)?.captureLiveSnapshot() },
    private val requireExternalContext: Boolean = true
) {
    fun execute(request: ActionRequest): ActionExecutionResult {
        val startTime = System.currentTimeMillis()
        val service = accessibilityService ?: AgentAccessibilityService.INSTANCE

        if (service == null || !AgentAccessibilityService.isBound) {
            val verResult = VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACCESSIBILITY_UNAVAILABLE,
                reason = "AccessibilityService is unbound or unavailable"
            )
            return buildResultAndLog(request, null, false, null, null, verResult, startTime)
        }

        // Validate action type FIRST
        val targetActionType = when (request.actionType) {
            ActionType.UI_CLICK -> TargetActionType.CLICK
            ActionType.UI_LONG_CLICK -> TargetActionType.LONG_CLICK
            ActionType.UI_TEXT_INPUT -> TargetActionType.EDITABLE
            ActionType.UI_SCROLL_FORWARD, ActionType.UI_SCROLL_BACKWARD -> TargetActionType.SCROLL
            else -> {
                val verResult = VerificationResult(
                    status = VerificationStatus.ACTION_NOT_SUPPORTED,
                    resultCode = ResultCode.CAPABILITY_UNAVAILABLE,
                    reason = "Action ${request.actionType} is not a UI action"
                )
                return buildResultAndLog(request, null, false, null, null, verResult, startTime)
            }
        }

        // Capture pre-action observation snapshot
        var preSnapshot = snapshotProvider() ?: ObservationSnapshot()

        // Package Safety Check: Reject LocalAgent-owned targets when external app action is required
        if (requireExternalContext && !service.isValidExternalApplicationPackage(preSnapshot.packageName)) {
            val verResult = VerificationResult(
                status = VerificationStatus.TARGET_NOT_FOUND,
                resultCode = ResultCode.TARGET_NOT_FOUND,
                reason = "Target package '${preSnapshot.packageName}' belongs to LocalAgent itself. UI actions require a valid external application context."
            )
            return buildResultAndLog(request, null, false, preSnapshot, null, verResult, startTime)
        }

        // Blocking Overlay / Modal Dialog Window Detection
        val activeWindowCandidate = service.latestDiagnostics.availableWindows.firstOrNull { it.isActive && it.isFocused }
        if (activeWindowCandidate != null &&
            activeWindowCandidate.windowType == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM &&
            activeWindowCandidate.packageName.isNotBlank() &&
            activeWindowCandidate.packageName != preSnapshot.packageName &&
            activeWindowCandidate.packageName != "com.localagent.app"
        ) {
            val verResult = VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACTION_FAILED,
                reason = "CLICK NOT EXECUTED: A blocking system dialog or overlay ('${activeWindowCandidate.packageName}') changed the actionable window. The target '${request.targetNodeId ?: request.targetNodeIdentity}' could not be safely resolved in the live UI."
            )
            return buildResultAndLog(request, null, false, preSnapshot, null, verResult, startTime)
        }

        // Source Package Window Usability Verification Step (Ensure source window where target resides is ready)
        val sourcePkg = preSnapshot.packageName
        if (requireExternalContext && service.isValidExternalApplicationPackage(sourcePkg)) {
            val verifier = com.localagent.app.system.LaunchVerifier(accessibilityServiceSupplier = { service })
            val minGen = request.preActionMinGeneration.coerceAtLeast(preSnapshot.generation - 1)
            val freshSnap = verifier.awaitUsableWindowAndSnapshot(
                targetPackageName = sourcePkg,
                minGeneration = minGen,
                timeoutMs = 1500L,
                settleDelayMs = 100L
            )

            if (freshSnap != null) {
                preSnapshot = freshSnap
            }
        }

        val targetNodeId = request.targetNodeId ?: if (request.actionType == ActionType.UI_SCROLL_FORWARD || request.actionType == ActionType.UI_SCROLL_BACKWARD) "scroll" else null
        val targetNodeIdentity = request.targetNodeIdentity

        if (targetNodeId == null && targetNodeIdentity == null) {
            val verResult = VerificationResult(
                status = VerificationStatus.TARGET_NOT_FOUND,
                resultCode = ResultCode.TARGET_NOT_FOUND,
                reason = "Action request lacks targetNodeId and targetNodeIdentity"
            )
            return buildResultAndLog(request, null, false, preSnapshot, null, verResult, startTime)
        }

        // 1. Resolve Target
        resolutionLogger?.beginRequest()
        val resolutionResult = if (targetNodeId != null) {
            targetResolver.resolveTargetById(preSnapshot, targetNodeId, targetActionType)
        } else {
            targetResolver.resolveTargetByIdentity(preSnapshot, targetNodeIdentity!!, targetActionType)
        }
        resolutionLogger?.logTargetResolution(resolutionResult)

        if (resolutionResult.status != TargetResolutionStatus.RESOLVED || resolutionResult.resolvedNode == null) {
            resolutionLogger?.endRequest()
            // Fallback for scroll actions: Gesture fallback if target container not found
            if (request.actionType == ActionType.UI_SCROLL_FORWARD || request.actionType == ActionType.UI_SCROLL_BACKWARD) {
                val gestureSuccess = dispatchGestureFallback(request.actionType)
                val verResult = if (gestureSuccess) {
                    verificationStrategy.verify(request.actionType, "gesture", preSnapshot, snapshotProvider() ?: preSnapshot)
                } else {
                    VerificationResult(
                        status = VerificationStatus.EXECUTION_FAILED,
                        resultCode = ResultCode.ACTION_FAILED,
                        reason = "Accessibility scroll target not found and gesture fallback failed or was rejected"
                    )
                }
                return buildResultAndLog(request, resolutionResult, gestureSuccess, preSnapshot, snapshotProvider(), verResult, startTime)
            }

            val verResult = VerificationResult(
                status = if (resolutionResult.status == TargetResolutionStatus.NO_ACTIONABLE_TARGET) VerificationStatus.TARGET_NOT_ACTIONABLE else VerificationStatus.TARGET_NOT_FOUND,
                resultCode = if (resolutionResult.status == TargetResolutionStatus.NO_ACTIONABLE_TARGET) ResultCode.TARGET_NOT_ACTIONABLE else ResultCode.TARGET_NOT_FOUND,
                reason = resolutionResult.failureReason ?: "Target resolution failed"
            )
            return buildResultAndLog(request, resolutionResult, false, preSnapshot, null, verResult, startTime)
        }

        val resolvedNode = resolutionResult.resolvedNode!!

        // 2. Live Re-acquisition
        val liveRoot = liveRootNodeProvider(preSnapshot.packageName)

        // Diagnostic Logging
        logDiagnosticContext(request, preSnapshot, service, liveRoot, resolvedNode)

        val reacquisitionResult = liveTargetResolver.reacquireLiveTarget(
            rootLiveNode = liveRoot,
            targetResolutionResult = resolutionResult,
            resolvedTarget = resolvedNode,
            actionType = targetActionType,
            requestedTargetQuery = request.targetNodeId ?: request.targetNodeIdentity
        )
        resolutionLogger?.logTargetResolution(reacquisitionResult.targetResolutionResult)
        resolutionLogger?.endRequest()

        var liveNode = reacquisitionResult.liveNode
        var finalTargetRes = reacquisitionResult.targetResolutionResult

        if (!reacquisitionResult.reacquired || liveNode == null) {
            // Safe bounded re-resolution against live root if initial re-acquisition failed due to layout shift
            if (liveRoot != null) {
                val liveSnap = (accessibilityService ?: AgentAccessibilityService.INSTANCE)?.captureLiveSnapshot(preSnapshot.packageName)
                if (liveSnap != null) {
                    val freshRes = if (targetNodeId != null) {
                        targetResolver.resolveTargetById(liveSnap, targetNodeId, targetActionType)
                    } else {
                        targetResolver.resolveTargetByIdentity(liveSnap, targetNodeIdentity!!, targetActionType)
                    }
                    if (freshRes.status == TargetResolutionStatus.RESOLVED && freshRes.resolvedNode != null) {
                        val retryReacq = liveTargetResolver.reacquireLiveTarget(
                            rootLiveNode = liveRoot,
                            targetResolutionResult = freshRes,
                            resolvedTarget = freshRes.resolvedNode!!,
                            actionType = targetActionType,
                            requestedTargetQuery = request.targetNodeId ?: request.targetNodeIdentity
                        )
                        if (retryReacq.reacquired && retryReacq.liveNode != null) {
                            liveNode = retryReacq.liveNode
                            finalTargetRes = retryReacq.targetResolutionResult
                        }
                    }
                }
            }
        }

        if (liveNode == null) {
            liveRoot?.recycle()
            val verResult = VerificationResult(
                status = VerificationStatus.TARGET_STALE,
                resultCode = ResultCode.TARGET_STALE,
                reason = reacquisitionResult.failureReason ?: "Live target re-acquisition and re-resolution failed"
            )
            return buildResultAndLog(request, reacquisitionResult.targetResolutionResult, false, preSnapshot, null, verResult, startTime)
        }
        var dispatchSuccess = false

        // Log resolved live target details
        logResolvedLiveTarget(liveNode, request)

        // 3. Dispatch Action against Live Node
        try {
            dispatchSuccess = when (request.actionType) {
                ActionType.UI_CLICK -> {
                    var success = liveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    if (!success) {
                        var currentParent = liveNode.parent
                        while (currentParent != null) {
                            val p = currentParent
                            if (p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                success = true
                                p.recycle()
                                break
                            }
                            currentParent = p.parent
                            p.recycle()
                        }
                    }
                    success
                }
                ActionType.UI_LONG_CLICK -> liveNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                ActionType.UI_TEXT_INPUT -> {
                    val payload = request.textInputPayload ?: ""
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, payload)
                    }
                    var success = liveNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                    if (!success) {
                        if (liveNode.isFocusable && liveNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                            success = liveNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        }
                    }
                    if (!success) {
                        if (liveNode.isClickable && liveNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            success = liveNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        }
                    }
                    success
                }
                ActionType.UI_SCROLL_FORWARD -> {
                    var success = liveNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    if (!success) {
                        success = dispatchGestureFallback(ActionType.UI_SCROLL_FORWARD)
                    }
                    success
                }
                ActionType.UI_SCROLL_BACKWARD -> {
                    var success = liveNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
                    if (!success) {
                        success = dispatchGestureFallback(ActionType.UI_SCROLL_BACKWARD)
                    }
                    success
                }
                else -> false
            }
            logDispatchResult(request, dispatchSuccess)
        } finally {
            liveNode.recycle()
            if (liveRoot != null && liveRoot != liveNode) {
                liveRoot.recycle()
            }
        }

        // Settle Delay before capturing post-action snapshot (600ms for CLICK, 800ms for SCROLL)
        val settleMillis = verificationStrategy.getSettleMillis(request.actionType)
        if (settleMillis > 0) {
            try {
                Thread.sleep(settleMillis)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }

        // 4. Capture Post-Action Snapshot strictly post-dispatch
        fun isValidPostSnapshot(snap: ObservationSnapshot?): Boolean {
            if (snap == null) return false
            if (snap.snapshotId == preSnapshot.snapshotId) return false
            if (snap.packageName.isBlank() || service?.isValidExternalApplicationPackage(snap.packageName) == false) return false
            // Package transition is valid even if destination hierarchy is sparse
            if (snap.packageName != preSnapshot.packageName) return true
            // Same-package capture requires nodeCount > 0 and (generation > preSnapshot.generation or timestamp >= preSnapshot.timestamp)
            return snap.nodeCount > 0 && (snap.generation > preSnapshot.generation || snap.timestamp >= preSnapshot.timestamp)
        }

        val postTargetPkg = request.expectedPackageName ?: preSnapshot.packageName
        val freshPostCap = service?.captureLiveSnapshot(targetPackageName = postTargetPkg)
        var postSnapshot: ObservationSnapshot? = freshPostCap.takeIf { isValidPostSnapshot(it) }
            ?: snapshotProvider()?.takeIf { isValidPostSnapshot(it) }

        // Bounded polling for fresh post-action snapshot if generation hasn't advanced or package transition occurred
        if (service != null && !isValidPostSnapshot(postSnapshot)) {
            val pollStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - pollStart < 1000L) {
                try {
                    Thread.sleep(150L)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
                val polledCap = service.captureLiveSnapshot(targetPackageName = postTargetPkg)
                val candidatePost = polledCap.takeIf { isValidPostSnapshot(it) }
                    ?: snapshotProvider()?.takeIf { isValidPostSnapshot(it) }

                if (isValidPostSnapshot(candidatePost)) {
                    postSnapshot = candidatePost
                    break
                }
            }
        }

        // Enforce strict freshness & usability
        if (!isValidPostSnapshot(postSnapshot)) {
            postSnapshot = null
        }

        // 5. Verify Target-Aware Result
        val verification = if (!dispatchSuccess) {
            VerificationResult(
                status = VerificationStatus.EXECUTION_FAILED,
                resultCode = ResultCode.ACTION_FAILED,
                reason = "Framework performAction returned false"
            )
        } else {
            verificationStrategy.verifyTargetAware(
                actionType = request.actionType,
                targetNodeId = resolvedNode.nodeId,
                expectedPackageName = request.expectedPackageName,
                preSnapshot = preSnapshot,
                postSnapshot = postSnapshot
            )
        }

        return buildResultAndLog(request, finalTargetRes, dispatchSuccess, preSnapshot, postSnapshot, verification, startTime)
    }

    private fun buildResultAndLog(
        request: ActionRequest,
        targetRes: TargetResolutionResult?,
        dispatchSuccess: Boolean,
        preSnap: ObservationSnapshot?,
        postSnap: ObservationSnapshot?,
        verification: VerificationResult,
        startTime: Long
    ): ActionExecutionResult {
        val duration = System.currentTimeMillis() - startTime
        val result = ActionExecutionResult(
            request = request,
            targetResolutionResult = targetRes,
            dispatchSuccess = dispatchSuccess,
            preSnapshot = preSnap,
            postSnapshot = postSnap,
            verificationResult = verification,
            executionDurationMs = duration
        )

        (accessibilityService ?: AgentAccessibilityService.INSTANCE)?.recordActionExecution(result)
        return result
    }

    private fun logDiagnosticContext(
        request: ActionRequest,
        preSnapshot: ObservationSnapshot,
        service: AgentAccessibilityService,
        liveRoot: AccessibilityNodeInfo?,
        resolvedNode: com.localagent.core.observation.ObservationNode
    ) {
        val app = (accessibilityService as? AgentAccessibilityService)?.application as? LocalAgentApplication ?: return
        val activeSessionId = app.eventLogger.getActiveSession().sessionId

        val windowsInfo = service.latestDiagnostics.availableWindows.joinToString(";") { win ->
            "winId:${win.windowId},pkg:${win.packageName},type:${win.windowTypeName},active:${win.isActive},focused:${win.isFocused}"
        }

        val json = """
            {
              "requestedAction": "${request.actionType}",
              "requestedTarget": "${request.targetNodeId ?: request.targetNodeIdentity}",
              "targetPackage": "${preSnapshot.packageName}",
              "currentForegroundPackage": "${service.activePackageName}",
              "activeRootPackage": "${liveRoot?.packageName}",
              "windowsCount": ${service.latestDiagnostics.availableWindows.size},
              "availableWindows": "$windowsInfo",
              "selectedTargetWindowId": ${service.latestDiagnostics.lastExternalWindowId},
              "selectedTargetWindowPackage": "${service.latestDiagnostics.lastExternalPackage}",
              "resolvedTargetText": "${resolvedNode.text ?: ""}",
              "resolvedTargetResId": "${resolvedNode.resourceId ?: ""}",
              "resolvedTargetIdentity": "${resolvedNode.nodeIdentity}"
            }
        """.trimIndent()

        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACTION,
                eventType = "ACTION_DIAGNOSTIC_BEFORE_DISPATCH",
                actionType = request.actionType.name,
                sourceChannel = request.sourceChannel,
                severity = EventSeverity.INFO,
                metadataJson = json
            )
        )
    }

    private fun logResolvedLiveTarget(liveNode: AccessibilityNodeInfo, request: ActionRequest) {
        val app = (accessibilityService as? AgentAccessibilityService)?.application as? LocalAgentApplication ?: return
        val activeSessionId = app.eventLogger.getActiveSession().sessionId

        val json = """
            {
              "liveNodePackage": "${liveNode.packageName}",
              "liveNodeClass": "${liveNode.className}",
              "liveNodeText": "${liveNode.text}",
              "liveNodeResId": "${liveNode.viewIdResourceName}",
              "isClickable": ${liveNode.isClickable},
              "isLongClickable": ${liveNode.isLongClickable},
              "isScrollable": ${liveNode.isScrollable},
              "isEditable": ${liveNode.isEditable}
            }
        """.trimIndent()

        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACTION,
                eventType = "ACTION_DIAGNOSTIC_LIVE_NODE_ACQUIRED",
                actionType = request.actionType.name,
                sourceChannel = request.sourceChannel,
                severity = EventSeverity.INFO,
                metadataJson = json
            )
        )
    }

    private fun logDispatchResult(request: ActionRequest, success: Boolean) {
        val app = (accessibilityService as? AgentAccessibilityService)?.application as? LocalAgentApplication ?: return
        val activeSessionId = app.eventLogger.getActiveSession().sessionId

        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACTION,
                eventType = "ACTION_DIAGNOSTIC_DISPATCH_RESULT",
                actionType = request.actionType.name,
                sourceChannel = request.sourceChannel,
                severity = if (success) EventSeverity.INFO else EventSeverity.WARNING,
                metadataJson = "{\"dispatchSuccess\":$success}"
            )
        )
    }

    private fun dispatchGestureFallback(actionType: ActionType): Boolean {
        val service = accessibilityService ?: AgentAccessibilityService.INSTANCE ?: return false
        val metrics = service.resources?.displayMetrics
        val width = metrics?.widthPixels?.toFloat()?.takeIf { it > 0 } ?: 1080f
        val height = metrics?.heightPixels?.toFloat()?.takeIf { it > 0 } ?: 2160f

        val (startX, startY, endX, endY) = when (actionType) {
            ActionType.UI_SCROLL_FORWARD -> listOf(width / 2f, height * 0.75f, width / 2f, height * 0.25f)
            ActionType.UI_SCROLL_BACKWARD -> listOf(width / 2f, height * 0.25f, width / 2f, height * 0.75f)
            else -> return false
        }

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            try {
                val path = android.graphics.Path().apply {
                    moveTo(startX, startY)
                    lineTo(endX, endY)
                }
                val stroke = android.accessibilityservice.GestureDescription.StrokeDescription(path, 0L, 300L)
                val gesture = android.accessibilityservice.GestureDescription.Builder().addStroke(stroke).build()
                var dispatched = false
                val latch = java.util.concurrent.CountDownLatch(1)
                service.dispatchGesture(gesture, object : android.accessibilityservice.AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: android.accessibilityservice.GestureDescription?) {
                        dispatched = true
                        latch.countDown()
                    }
                    override fun onCancelled(gestureDescription: android.accessibilityservice.GestureDescription?) {
                        dispatched = false
                        latch.countDown()
                    }
                }, null)
                latch.await(1000L, java.util.concurrent.TimeUnit.MILLISECONDS)
                dispatched
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    private fun logActionEvidence(result: ActionExecutionResult) {
        val app = (accessibilityService as? AgentAccessibilityService)?.application as? LocalAgentApplication ?: return
        val activeSessionId = app.eventLogger.getActiveSession().sessionId

        val evidence = ActionEvidence(
            executionId = result.executionId,
            actionType = result.request.actionType,
            targetIdentity = result.targetResolutionResult?.resolvedNodeIdentity ?: result.request.targetNodeIdentity ?: result.request.targetNodeId,
            targetConfidence = result.targetResolutionResult?.confidence,
            preSnapshotId = result.preSnapshot?.snapshotId,
            postSnapshotId = result.postSnapshot?.snapshotId,
            diffSummary = result.verificationResult.diffResult?.toJsonString() ?: "No diff",
            status = result.verificationResult.status,
            resultCode = result.verificationResult.resultCode
        )

        app.eventLogger.logEvent(
            AgentEvent(
                eventId = UUID.randomUUID().toString(),
                sessionId = activeSessionId,
                subsystem = EventSubsystem.ACTION,
                eventType = "ACTION_EVIDENCE_GENERATED",
                actionType = result.request.actionType.name,
                sourceChannel = result.request.sourceChannel,
                resultCode = result.verificationResult.resultCode,
                durationMs = result.executionDurationMs,
                severity = if (result.verificationResult.status == VerificationStatus.EXECUTED_AND_VERIFIED) EventSeverity.INFO else EventSeverity.WARNING,
                errorCode = result.verificationResult.reason,
                metadataJson = evidence.toJsonObject().toString()
            )
        )
    }
}
