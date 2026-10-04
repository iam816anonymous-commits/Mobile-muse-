# ACTION_CONTRACTS.md — Action Contracts & Verification Specifications

## 1. Executive Summary

In **LocalAgent**, every action executed by the agent must fulfill a formal **Action Contract**. An action dispatch is never considered complete based on Android API returns (`performAction() == true`).

Every Action Contract defines:
1. **Preconditions & Capability Prerequisites**
2. **Target Resolution & Actionable Ancestor Traversal Rules**
3. **Execution Dispatch Mechanism**
4. **Post-Action Settle Delay & Fresh Observation Capture**
5. **Action-Specific `VerificationStrategy` Protocol**
6. **Standardized Result Classification Codes**
7. **Failure Modes & Graceful Degradation Policies**

---

## 2. Universal Result Classification Codes

| Result Code | Meaning | Verification Status | Next Action |
|---|---|---|---|
| `SUCCESS_VERIFIED` | Action dispatched and action-specific verification criteria confirmed. | Verified | Proceed to next step |
| `DISPATCHED_BUT_NOT_VERIFIED` | `performAction()` returned true, but expected state diff did not settle within timeout. | Unverified | Log warning / Retry step |
| `ACTION_FAILED` | Android API `performAction()` returned false. | Failed | Retry with gesture fallback |
| `TARGET_NOT_FOUND` | Target selector matched no node in active window tree. | Failed | Re-scan UI or re-plan |
| `TARGET_NOT_ACTIONABLE` | Target found, but no actionable ancestor matched capability requirement. | Failed | Abort action |
| `TARGET_STALE` | Node state mutated or window closed before dispatch. | Failed | Reacquire live window |
| `ACCESSIBILITY_UNAVAILABLE` | Accessibility service unbound or disconnected. | Unavailable | Prompt user / Re-bind |
| `PERMISSION_REQUIRED` | Capability missing mandatory system permission. | Denied | Launch Permission Center |
| `POLICY_BLOCKED` | Action blocked by `ActionPolicyEngine` (high risk without user consent). | Denied | Request user confirmation |
| `CAPABILITY_UNAVAILABLE` | Action unsupported on device or current API level. | Unavailable | Abort action |
| `TIMEOUT` | Post-action verification window timed out before state settled. | Failed | Retry step |

---

## 3. Action-Specific Verification Strategies (`VerificationStrategy`)

Generic "something changed" verification is insufficient. Each action type enforces a specialized `VerificationStrategy`:

```kotlin
sealed class VerificationStrategy {
    abstract fun verify(preState: ObservationSnapshot, postState: ObservationSnapshot, diff: SnapshotDiffResult): VerificationResult

    object ClickStrategy : VerificationStrategy() {
        override fun verify(preState: ObservationSnapshot, postState: ObservationSnapshot, diff: SnapshotDiffResult): VerificationResult {
            val isVerified = diff.packageChanged ||
                    diff.windowChanged ||
                    diff.textChanges.isNotEmpty() ||
                    diff.checkedStateChanges.isNotEmpty() ||
                    diff.addedNodeIdentities.isNotEmpty()
            return if (isVerified) VerificationResult.SuccessVerified else VerificationResult.DispatchedUnverified
        }
    }

    data class TextInputStrategy(val expectedText: String) : VerificationStrategy() {
        override fun verify(preState: ObservationSnapshot, postState: ObservationSnapshot, diff: SnapshotDiffResult): VerificationResult {
            val targetField = postState.nodes.firstOrNull { it.isEditable || it.isFocused }
            val isVerified = targetField?.text == expectedText || diff.textChanges.any { it.newText == expectedText }
            return if (isVerified) VerificationResult.SuccessVerified else VerificationResult.DispatchedUnverified
        }
    }

    object ScrollStrategy : VerificationStrategy() {
        override fun verify(preState: ObservationSnapshot, postState: ObservationSnapshot, diff: SnapshotDiffResult): VerificationResult {
            val isVerified = diff.scrollContainerChanges.isNotEmpty() ||
                    diff.boundsChanges.isNotEmpty() ||
                    diff.addedNodeIdentities.isNotEmpty() ||
                    diff.removedNodeIdentities.isNotEmpty()
            return if (isVerified) VerificationResult.SuccessVerified else VerificationResult.DispatchedUnverified
        }
    }

    object NavigationAwareBackStrategy : VerificationStrategy() {
        override fun verify(preState: ObservationSnapshot, postState: ObservationSnapshot, diff: SnapshotDiffResult): VerificationResult {
            // Back navigation may return to previous activity, close a popup/dialog, or return to home screen
            val isVerified = diff.packageChanged ||
                    diff.windowChanged ||
                    diff.removedNodeIdentities.size > 2 ||
                    preState.totalNodeCount != postState.totalNodeCount
            return if (isVerified) VerificationResult.SuccessVerified else VerificationResult.DispatchedUnverified
        }
    }

    data class FlexibleLongClickStrategy(val expectedOutcome: LongClickOutcome) : VerificationStrategy() {
        override fun verify(preState: ObservationSnapshot, postState: ObservationSnapshot, diff: SnapshotDiffResult): VerificationResult {
            val isVerified = when (expectedOutcome) {
                LongClickOutcome.CONTEXT_MENU -> diff.addedNodeIdentities.isNotEmpty() || diff.windowChanged
                LongClickOutcome.TEXT_SELECTION -> diff.focusChanges.isNotEmpty() || diff.boundsChanges.isNotEmpty()
                LongClickOutcome.ANY_UI_CHANGE -> diff.hasObservableChange
            }
            return if (isVerified) VerificationResult.SuccessVerified else VerificationResult.DispatchedUnverified
        }
    }
}

enum class LongClickOutcome { CONTEXT_MENU, TEXT_SELECTION, ANY_UI_CHANGE }
```

---

## 4. Core Action Contracts

### 4.1 `UI_CLICK` Contract
- **Target Selection:** Match node by `nodeIdentity`, View ID, exact text, or content description.
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
- **Verification:** `VerificationStrategy.ClickStrategy`.

### 4.2 `UI_LONG_CLICK` Contract
- **Target Selection:** Match node by View ID, exact text, or content description.
- **Actionable Ancestor Rule:** Traverse parents until `isLongClickable == true`.
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)`
- **Verification:** `VerificationStrategy.FlexibleLongClickStrategy`.

### 4.3 `UI_TEXT_INPUT` Contract
- **Target Selection:** Match node where `isEditable == true` or class is `EditText`.
- **Pre-Dispatch Action:** Focus node via `ACTION_FOCUS`.
- **Dispatch:** `node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) })`
- **Verification:** `VerificationStrategy.TextInputStrategy(text)`.

### 4.4 `UI_SCROLL_FORWARD` / `UI_SCROLL_BACKWARD` Contract
- **Target Selection:** Target container or active window root.
- **Actionable Ancestor Rule:** Traverse parents until `isScrollable == true`.
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)`
- **Verification:** `VerificationStrategy.ScrollStrategy`.

### 4.5 `GLOBAL_BACK` Contract
- **Preconditions:** `AgentAccessibilityService.isConnected == true`.
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)`
- **Verification:** `VerificationStrategy.NavigationAwareBackStrategy`.

### 4.6 `GLOBAL_HOME` Contract
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)`
- **Verification:** `SUCCESS_VERIFIED` if postState foreground package matches system launcher package name.
