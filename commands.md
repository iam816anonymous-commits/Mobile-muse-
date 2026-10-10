# LocalAgent Command Reference (`commands.md`)

This document details all supported command syntaxes in LocalAgent, explaining their purpose, execution flow, parameters, and failure handling across all command entry points (Console Input, Floating Console Overlay, Voice Input, and AI Planners).

---

## 1. Application Control Commands

### `launch <app>`
- **Purpose**: Opens or brings an installed Android application to the foreground before interacting with it.
- **Syntax**: `launch <app_query>`
- **Examples**:
  - `launch calculator`
  - `launch settings`
  - `launch com.android.calculator2`
- **Execution Pipeline**:
  1. `AppResolver` looks up the package name and launch intent via `PackageManager`.
  2. `AppLauncher` starts the target activity (or detects if it is already in the foreground).
  3. `LaunchVerifier` polls active accessibility events (`activePackageName`) for up to 3000ms to confirm that the app is in the foreground.
  4. Returns `SUCCESS_VERIFIED` once the foreground package transition is verified.

---

## 2. Targeted UI Action Commands

### `click <target>`
- **Purpose**: Performs a single tap/click on a visible, actionable UI element.
- **Syntax**: `click <target_descriptor>`
- **Examples**:
  - `click 7`
  - `click AC`
  - `click Submit`
  - `click com.app:id/btn_submit`
- **Execution Pipeline**:
  1. Verifies that the target application is in the foreground (calling Phase 8 `AppLauncher` / `LaunchVerifier` if needed).
  2. Captures a fresh accessibility observation snapshot.
  3. Resolves the target via `TargetResolver` using multi-strategy semantic lookup (ResID -> Identity -> Text -> ContentDesc).
  4. Re-acquires the live `AccessibilityNodeInfo` via `LiveTargetResolver`.
  5. Dispatches `ACTION_CLICK` via `UiActionExecutor` (with parent container fallback if the child view returns false).
  6. Enforces a 600ms settlement delay before capturing a post-action snapshot to verify the UI state change.

### `long click <target>`
- **Purpose**: Performs a long press on a target element.
- **Syntax**: `long click <target_descriptor>`
- **Examples**:
  - `long click card_item`
- **Execution Pipeline**: Identical to `click`, but dispatches `ACTION_LONG_CLICK`.

### `scroll down` / `scroll forward`
- **Purpose**: Scrolls forward on a scrollable container (e.g. `RecyclerView`, `ScrollView`).
- **Syntax**: `scroll down` or `scroll forward`
- **Execution Pipeline**: Dispatches `ACTION_SCROLL_FORWARD` on the primary scrollable target.

### `scroll up` / `scroll backward`
- **Purpose**: Scrolls backward on a scrollable container.
- **Syntax**: `scroll up` or `scroll backward`
- **Execution Pipeline**: Dispatches `ACTION_SCROLL_BACKWARD` on the primary scrollable target.

---

## 3. OS System Navigation Commands

### `back`
- **Purpose**: Triggers standard Android system BACK navigation.
- **Syntax**: `back`
- **Execution Pipeline**: Dispatches `GLOBAL_ACTION_BACK` via `GlobalActionExecutor` without requiring target resolution.

### `home`
- **Purpose**: Navigates to the Android home launcher screen.
- **Syntax**: `home`
- **Execution Pipeline**: Dispatches `GLOBAL_ACTION_HOME` via `GlobalActionExecutor`.

### `recents`
- **Purpose**: Opens the Android recent apps overview screen.
- **Syntax**: `recents`
- **Execution Pipeline**: Dispatches `GLOBAL_ACTION_RECENTS` via `GlobalActionExecutor`.

---

## 4. System Inspection & Diagnostic Commands

### `observe`
- **Purpose**: Captures and renders a fresh live accessibility observation tree.
- **Syntax**: `observe`
- **Execution Pipeline**: Invokes `AgentAccessibilityService.captureLiveSnapshot()` and updates the UI observation tree.

### `status`
- **Purpose**: Displays LocalAgent operational status, Accessibility Service connection state, and storage health.
- **Syntax**: `status`
