# ACTION_CONTRACTS.md — Action Contracts & Execution Specifications

## 1. Executive Summary

In **LocalAgent**, every action executed by the agent must fulfill a formal **Action Contract**. An action dispatch is never considered complete based on Android API returns (`performAction() == true`).

Every Action Contract defines:
1. **Preconditions & Capability Prerequisites**
2. **Target Resolution & Actionable Ancestor Traversal Rules**
3. **Execution Dispatch Mechanism**
4. **Post-Action Settle Delay & Fresh Observation Capture**
5. **Observable UI State-Diff Verification Protocol**
6. **Standardized Result Classification Codes**
7. **Failure Modes & Graceful Degradation Policies**

---

## 2. Standardized Action Execution Lifecycle

```text
               ┌───────────────────────────────────────────────┐
               │           1. Action Intent Received           │
               │   NormalizedCommand(actionType, targetSelector)│
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │         2. Precondition & Capability Check    │
               │   Check AccessibilityService, Permissions     │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │         3. Pre-Action Snapshot Capture        │
               │   Capture ObservationSnapshot (PreState)      │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │         4. Live Target Resolution             │
               │   Reacquire live node via rootInActiveWindow  │
               │   Traverse up to actionable ancestor          │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │         5. Dispatch Android Action            │
               │   node.performAction() / performGlobalAction()│
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │         6. Settle Delay (200ms - 500ms)       │
               │   Allow UI animations & transitions to settle │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │        7. Post-Action Snapshot Capture        │
               │   Capture ObservationSnapshot (PostState)     │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │        8. State-Diff Verification             │
               │   Compare PreState vs PostState UI primitives │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │        9. Classification & Persistent Log     │
               │   SUCCESS / DISPATCHED_UNVERIFIED / FAILED    │
               └───────────────────────────────────────────────┘
```

---

## 3. Universal Result Classification Codes

| Result Code | Meaning | Verification Status | Next Action |
|---|---|---|---|
| `SUCCESS_VERIFIED` | Action dispatched and expected observable state diff confirmed. | Verified | Proceed to next step |
| `DISPATCHED_BUT_NOT_VERIFIED` | `performAction()` returned true, but no observable state diff occurred within timeout. | Unverified | Retry or log warning |
| `ACTION_FAILED` | Android API `performAction()` returned false. | Failed | Retry with gesture fallback |
| `TARGET_NOT_FOUND` | Target selector matched no node in active window tree. | Failed | Re-scan UI or re-plan |
| `TARGET_NOT_ACTIONABLE` | Target found, but no actionable ancestor matched capability requirement. | Failed | Abort action |
| `TARGET_STALE` | Node state mutated or window closed before dispatch. | Failed | Reacquire live window |
| `ACCESSIBILITY_UNAVAILABLE` | Accessibility service unbound or disconnected. | Unavailable | Prompt user / Re-bind |
| `PERMISSION_REQUIRED` | Capability missing mandatory system permission. | Denied | Launch Permission Center |
| `CAPABILITY_UNAVAILABLE` | Action unsupported on device or current API level. | Unavailable | Abort action |
| `TIMEOUT` | Post-action verification window timed out before state settled. | Failed | Retry step |

---

## 4. Core Action Contracts

### 4.1 `UI_CLICK` Contract
- **Target Selection:** Match node by View ID, exact text, content description, or semantic attributes.
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
  *(Crucial Calculator lesson: A child `TextView` displaying "7" is not clickable, but its parent `MaterialButton` container is clickable).*
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)`
- **Verification Rule:** `SUCCESS_VERIFIED` if:
  - Active foreground package or activity changed, OR
  - Screen window or layout hierarchy changed, OR
  - Checked state toggled (`isChecked` flipped), OR
  - Observable text in target area changed.

### 4.2 `UI_LONG_CLICK` Contract
- **Target Selection:** Match node by View ID, exact text, or content description.
- **Actionable Ancestor Rule:** Traverse parents until `isLongClickable == true`.
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)`
- **Verification Rule:** `SUCCESS_VERIFIED` if a new context menu, popup window, or selection handle appears in post-action snapshot.

### 4.3 `UI_TEXT_INPUT` Contract
- **Target Selection:** Match node where `isEditable == true` or class is `EditText`.
- **Pre-Dispatch Action:** Focus node via `ACTION_FOCUS`.
- **Dispatch:** `node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) })`
- **Verification Rule:** `SUCCESS_VERIFIED` if post-action snapshot node text matches input string exactly.

### 4.4 `UI_SCROLL_FORWARD` / `UI_SCROLL_BACKWARD` Contract
- **Target Selection:** Target container or active window root.
- **Actionable Ancestor Rule:** Traverse parents until `isScrollable == true`.
- **Dispatch:** `ancestorNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)`
- **Verification Rule:** `SUCCESS_VERIFIED` if:
  - Visible child node set changed (new nodes appeared or previous nodes disappeared), OR
  - Bounds or text coordinates of child items shifted.

### 4.5 `GLOBAL_BACK` Contract
- **Preconditions:** `AgentAccessibilityService.isConnected == true`.
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)`
- **Verification Rule:** `SUCCESS_VERIFIED` if active foreground activity or window ID changed compared to pre-action snapshot.

### 4.6 `GLOBAL_HOME` Contract
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)`
- **Verification Rule:** `SUCCESS_VERIFIED` if foreground package matches system launcher package name.

### 4.7 `GLOBAL_RECENTS` Contract
- **Dispatch:** `accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)`
- **Verification Rule:** `SUCCESS_VERIFIED` if recents provider package/window appears in foreground.
