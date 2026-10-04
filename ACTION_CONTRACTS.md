# ACTION_CONTRACTS.md — Target-Aware Action Contracts & Verification Specifications

## 1. Executive Summary

In **LocalAgent**, every action executed by the agent must fulfill a formal **Action Contract**. An action dispatch is never considered complete based solely on Android framework API returns (`performAction() == true`).

Every Action Contract defines:
1. **Preconditions & Capability Prerequisites**
2. **Target Resolution & Actionable Ancestor Traversal Rules**
3. **Execution Dispatch Mechanism**
4. **Post-Action Settle Delay & Fresh Observation Capture**
5. **Target-Aware `VerificationStrategy` Protocol**
6. **Standardized Result Classification Codes**
7. **Failure Modes & Graceful Degradation Policies**

---

## 2. Standardized Result Classification Taxonomy

The architecture defines explicit result categories. The system **never claims verified success** merely because `performAction()` returned true, timestamps updated, or unrelated background UI mutations occurred.

| Result Code | Meaning | Verification Status | Allowed Contexts / Trigger Criteria | Next Action |
|---|---|---|---|---|
| `SUCCESS_VERIFIED` | Action dispatched AND target-specific expected state diff confirmed. | Verified | Target-aware criteria strictly satisfied | Proceed to next step |
| `DISPATCHED_BUT_NOT_VERIFIED` | Action dispatched (`performAction() == true`), but target-specific state diff did not settle within timeout. | Unverified | Ambiguous UI change or non-deterministic target response | Log warning / Retry or prompt user |
| `NO_EFFECT_EXPECTED` | Action dispatched for an idempotent state (e.g., setting volume to current level). | Dispatched | Target already in desired state | Proceed without error |
| `NO_SCROLL_POSSIBLE` | Scroll action attempted, but container is demonstrably at boundary end/beginning. | Deterministic | End of scrollable container reached | Complete scroll sequence |
| `ACTION_FAILED` | Android API `performAction()` returned false or exception thrown. | Failed | Framework API dispatch rejected | Retry with gesture fallback |
| `TARGET_NOT_FOUND` | Target selector matched no node or matched only with `LOW`/`EPHEMERAL` confidence. | Failed | Target missing or ambiguous | Re-scan UI or re-plan |
| `TARGET_NOT_ACTIONABLE` | Target found, but no actionable ancestor matched capability requirement. | Failed | Non-actionable container | Abort action |
| `TARGET_STALE` | Target node state mutated or window closed before dispatch. | Failed | Window transitioned before dispatch | Reacquire live window |
| `ACCESSIBILITY_UNAVAILABLE` | Accessibility service unbound or disconnected. | Unavailable | Service disconnected | Prompt user / Passive fallback |
| `POLICY_BLOCKED` | Action blocked by `ActionPolicyEngine` (HIGH/CRITICAL risk without user confirmation). | Denied | Policy restriction | Request user confirmation |
| `PERMISSION_REQUIRED` | Capability missing mandatory system permission or special access. | Denied | Missing permission | Launch Permission Center |
| `CAPABILITY_UNAVAILABLE` | Action unsupported on device, API level, or current app state. | Unavailable | Device restriction | Abort action |
| `TIMEOUT` | Post-action verification window timed out before state settled. | Failed | Animation lag / ANR | Retry step |
| `CANCELLED` | Task or goal explicitly cancelled by user via Overlay/Console. | Cancelled | User cancellation | Stop goal execution |
| `INTERRUPTED` | Task interrupted by Low Memory Killer (LMK) process termination. | Interrupted | LMK process kill | Initiate process recovery |
| `RECOVERY_REQUIRED` | Inconsistent UI state detected requiring fresh observation re-acquisition. | Recovery | UI state desynchronized | Re-observe active window |

---

## 3. Target-Aware Verification Strategies (`VerificationStrategy`)

Generic "something changed" verification is strictly forbidden. Unrelated UI mutations (e.g., a clock ticking, a notification banner arriving, or a background ad cycling) MUST NOT trigger `SUCCESS_VERIFIED`.

```kotlin
sealed class VerificationStrategy {
    abstract fun verify(
        targetIdentityKey: String?,
        preState: ObservationSnapshot,
        postState: ObservationSnapshot,
        diff: SnapshotDiffResult
    ): VerificationResult

    // Target-Specific Click Verification
    data class TargetClickStrategy(
        val expectedTargetViewId: String? = null,
        val expectedDestinationPackage: String? = null,
        val expectedDestinationWindow: String? = null
    ) : VerificationStrategy() {
        override fun verify(
            targetIdentityKey: String?,
            preState: ObservationSnapshot,
            postState: ObservationSnapshot,
            diff: SnapshotDiffResult
        ): VerificationResult {
            // Target-aware check 1: Verify target node text/checked state mutated specifically
            val targetMutated = diff.textChanges.any { it.nodeIdentityKey == targetIdentityKey || (expectedTargetViewId != null && it.nodeIdentityKey.contains(expectedTargetViewId)) } ||
                    diff.checkedStateChanges.any { it.nodeIdentityKey == targetIdentityKey }

            // Target-aware check 2: Explicitly expected navigation outcome reached
            val expectedNavigationOccurred = (expectedDestinationPackage != null && postState.packageName == expectedDestinationPackage) ||
                    (expectedDestinationWindow != null && postState.windowId.toString() == expectedDestinationWindow)

            return when {
                targetMutated || expectedNavigationOccurred -> VerificationResult.SuccessVerified
                diff.hasObservableChange -> VerificationResult.DispatchedButNotVerified("UI mutated elsewhere, but target node state did not mutate specifically")
                else -> VerificationResult.DispatchedButNotVerified("No observable change detected post-click")
            }
        }
    }

    // Toggle Verification (Settings Switch, Checkbox, Radio Button)
    data class ToggleVerificationStrategy(val expectedCheckedState: Boolean) : VerificationStrategy() {
        override fun verify(
            targetIdentityKey: String?,
            preState: ObservationSnapshot,
            postState: ObservationSnapshot,
            diff: SnapshotDiffResult
        ): VerificationResult {
            val toggleChange = diff.checkedStateChanges.firstOrNull { it.nodeIdentityKey == targetIdentityKey }
            return if (toggleChange?.isChecked == expectedCheckedState) {
                VerificationResult.SuccessVerified
            } else {
                val currentTarget = postState.nodes.firstOrNull { it.identity.identityKey == targetIdentityKey }
                if (currentTarget?.isChecked == expectedCheckedState) {
                    VerificationResult.SuccessVerified
                } else {
                    VerificationResult.DispatchedButNotVerified("Toggle state did not reach expected state: $expectedCheckedState")
                }
            }
        }
    }

    // Text Input Verification (Strictly tied to target identity or re-resolved target node)
    data class TextInputStrategy(val expectedText: String) : VerificationStrategy() {
        override fun verify(
            targetIdentityKey: String?,
            preState: ObservationSnapshot,
            postState: ObservationSnapshot,
            diff: SnapshotDiffResult
        ): VerificationResult {
            // Verify specific target field text matches expected string (never arbitrary field)
            val targetField = postState.nodes.firstOrNull { it.identity.identityKey == targetIdentityKey }
                ?: postState.nodes.firstOrNull { it.isFocused && it.isEditable }

            val isVerified = targetField?.text == expectedText ||
                    diff.textChanges.any { it.nodeIdentityKey == targetIdentityKey && it.newText == expectedText }

            return if (isVerified) {
                VerificationResult.SuccessVerified
            } else {
                VerificationResult.DispatchedButNotVerified("Text in target field specifically did not match expected string: '$expectedText'")
            }
        }
    }

    // Container-Specific Scroll Verification Strategy
    data class ScrollVerificationStrategy(val expectedDirection: ScrollDirection = ScrollDirection.FORWARD) : VerificationStrategy() {
        override fun verify(
            targetIdentityKey: String?,
            preState: ObservationSnapshot,
            postState: ObservationSnapshot,
            diff: SnapshotDiffResult
        ): VerificationResult {
            val containerDiff = diff.scrollContainerChanges.firstOrNull { it.containerIdentityKey == targetIdentityKey }
                ?: diff.scrollContainerChanges.firstOrNull()

            return when {
                containerDiff != null && (containerDiff.newlyVisibleChildIdentities.isNotEmpty() || containerDiff.shiftedChildIdentities.isNotEmpty()) -> {
                    VerificationResult.SuccessVerified
                }
                containerDiff?.isAtBoundary == true -> {
                    VerificationResult.NoScrollPossible("Scrollable container is demonstrably at boundary $expectedDirection")
                }
                else -> {
                    VerificationResult.DispatchedButNotVerified("Scroll action dispatched, but no descendant node of the target scroll container shifted position")
                }
            }
        }
    }

    // Navigation & Layer-Aware Back Strategy
    data class NavigationAwareBackStrategy(
        val expectedPreviousPackage: String? = null,
        val expectedPreviousWindow: Int? = null
    ) : VerificationStrategy() {
        override fun verify(
            targetIdentityKey: String?,
            preState: ObservationSnapshot,
            postState: ObservationSnapshot,
            diff: SnapshotDiffResult
        ): VerificationResult {
            // Layer-aware checks: Transition to previous activity/window, package, dismissed dialog/popup, keyboard dismissal, launcher return
            val activityWindowNavigated = diff.packageChanged || diff.windowChanged
            val expectedPackageReached = expectedPreviousPackage != null && postState.packageName == expectedPreviousPackage
            val expectedWindowReached = expectedPreviousWindow != null && postState.windowId == expectedPreviousWindow
            val popupDismissed = preState.windowId != postState.windowId

            return when {
                activityWindowNavigated || expectedPackageReached || expectedWindowReached || popupDismissed -> VerificationResult.SuccessVerified
                else -> VerificationResult.DispatchedButNotVerified("Back pressed, but active navigation/window layer remained identical")
            }
        }
    }

    // Flexible Long Click Strategy
    data class FlexibleLongClickStrategy(val expectedOutcome: LongClickOutcome) : VerificationStrategy() {
        override fun verify(
            targetIdentityKey: String?,
            preState: ObservationSnapshot,
            postState: ObservationSnapshot,
            diff: SnapshotDiffResult
        ): VerificationResult {
            val isVerified = when (expectedOutcome) {
                LongClickOutcome.CONTEXT_MENU -> diff.windowChanged || diff.addedNodeIdentities.any { it.contains("menu") || it.contains("popup") }
                LongClickOutcome.TEXT_SELECTION -> diff.focusChanges.any { it.nodeIdentityKey == targetIdentityKey }
                LongClickOutcome.EXPECTED_UI_RESULT -> diff.packageChanged || diff.windowChanged || diff.textChanges.any { it.nodeIdentityKey == targetIdentityKey }
                LongClickOutcome.NO_DETERMINISTIC_EXPECTATION -> diff.hasObservableChange
            }
            return if (isVerified) VerificationResult.SuccessVerified else VerificationResult.DispatchedButNotVerified("Long click executed, but expected outcome '$expectedOutcome' was not observed on target")
        }
    }
}

enum class ScrollDirection { FORWARD, BACKWARD }
enum class LongClickOutcome { CONTEXT_MENU, TEXT_SELECTION, EXPECTED_UI_RESULT, NO_DETERMINISTIC_EXPECTATION }

sealed class VerificationResult {
    object SuccessVerified : VerificationResult()
    data class DispatchedButNotVerified(val reason: String) : VerificationResult()
    data class NoScrollPossible(val reason: String) : VerificationResult()
    data class Failed(val reason: String) : VerificationResult()
}
```

---

## 4. Core Action Contracts

### 4.1 `UI_CLICK` Contract
- **Target Selection:** Match node by `nodeIdentity` key (requiring `EXACT`, `HIGH`, or `MEDIUM` confidence).
- **Actionable Ancestor Rule:**
  ```kotlin
  fun findClickableAncestor(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
      var current = node
      while (current != null) {
          if (current.isClickable) return current
          current = current.parent
      }
      return null
  }
  ```
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)`
- **Verification:** `VerificationStrategy.TargetClickStrategy`.

### 4.2 `UI_LONG_CLICK` Contract
- **Target Selection:** Match node by `nodeIdentity` key.
- **Actionable Ancestor Rule:** Traverse parents until `isLongClickable == true`.
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)`
- **Verification:** `VerificationStrategy.FlexibleLongClickStrategy`.

### 4.3 `UI_TEXT_INPUT` Contract
- **Target Selection:** Match node where `isEditable == true` or class is `EditText`.
- **Pre-Dispatch Action:** Focus node via `ACTION_FOCUS`.
- **Dispatch:** `node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) })`
- **Verification:** `VerificationStrategy.TextInputStrategy(text)`.

### 4.4 `UI_SCROLL_FORWARD` / `UI_SCROLL_BACKWARD` Contract
- **Target Selection:** Target container node or active window scrollable root.
- **Actionable Ancestor Rule:** Traverse parents until `isScrollable == true`.
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)`
- **Verification:** `VerificationStrategy.ScrollVerificationStrategy`.

### 4.5 `GLOBAL_BACK` Contract
- **Preconditions:** `AgentAccessibilityService.isConnected == true`.
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)`
- **Verification:** `VerificationStrategy.NavigationAwareBackStrategy`.

### 4.6 `GLOBAL_HOME` Contract
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)`
- **Verification:** `SUCCESS_VERIFIED` if postState foreground package matches system launcher package name.
