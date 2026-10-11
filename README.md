# README.md — LocalAgent Architecture, Phase Roadmap & Command Reference

## Overview

**LocalAgent** is a low-RAM, offline-first Android device control agent capable of observing, reasoning about, and safely executing actions on Android applications. It prioritizes **deterministic accessibility automation first**, with AI learning, research, and planning layers sitting above the core execution pipeline.

This repository implements **Phases 1 through 8** of the LocalAgent architecture baseline targeting compileSdk 34 and minSdk 27 (Android 8.1+).

---

## Phase Roadmap & Implementation Overview (Phases 1 – 8)

### Phase 1 — Core Domain Models & Command Core
- **Domain Primitives**: Defines `NormalizedCommand`, `TargetSelector` (`ByViewId`, `ByText`, `ByContentDescription`, `ByNodeIdentityKey`, `ByCoordinates`), `ActionRequest`, `ActionExecutionResult`, and `VerificationResult`.
- **Command Normalization**: `CommandNormalizer` parses raw command strings from all sources (Console, Overlay, Voice, Planners) into normalized command representations.
- **Single Execution Pipeline**: All commands route through `GoalDispatcher` -> `CapabilityResolver` -> Action Executor, preventing competing execution engines.

### Phase 2 — Storage Partitioning & Unified Event Logging
- **Operational Storage (`agent.db`)**: SQLite database using Room ORM persisting transient audit events, task lifecycles, and resolution logs with sensitive data redaction and automated row-pruning caps (30 MB / 50,000 rows).
- **Durable Memory Storage**: `DurableMemoryStorageManager` manages long-term agent memory via SAF/external storage (`/sdcard/LocalAgent/memory/`) with SHA-256 provenance checksums to survive app uninstalls.

### Phase 3 — Accessibility Service Foundation & Lifecycle Monitoring
- **`AgentAccessibilityService`**: Production accessibility service intercepting window state and UI content change events.
- **`AccessibilityServiceConnectionMonitor`**: Monitors service connection states (`UNBOUND`, `CONNECTING`, `BOUND`, `DEGRADED`). Unbinding logs `PASSIVE_DEGRADATION_DETECTED` to `agent.db` without crashing or launching unauthorized Settings intents.

### Phase 4 — Permission Manager & Centralized Permission Registry
- **`PermissionManager`**: Central authority categorizing permissions into `REQUIRED_NOW` (Accessibility Service), `AVAILABLE_OPTIONAL_NOW` (Storage/SAF), and `FUTURE_PHASE` (non-requestable inventory-only permissions).
- **`PermissionActivity`**: Dedicated permission management screen providing settings intents for requestable permissions while passively detailing future-phase degradation summaries.

### Phase 5 — Read-Only UI Observation Engine
- **`ObservationSnapshotExtractor`**: Extracts single-root observation hierarchies enforcing strict boundaries (`MAX_NODES = 500`, `MAX_DEPTH = 30`) with explicit truncation flags.
- **Composite Identity Calculation**: `ObservationNode.computeIdentity()` generates deterministic identity strings (`id:<resId>_text:<text>`, `id:<resId>_idx:<childIndex>`, `cls:<class>_lbl:<label>_parent:<parent>`) with confidence ratings (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`).
- **Resource Cleanup**: Guarantees zero un-recycled `AccessibilityNodeInfo` leaks by recycling nodes in `finally` blocks.

### Phase 6 — Multi-Strategy Target Resolution
- **`TargetResolver`**: Resolves targets from `ObservationSnapshot` using a 5-priority strategy (Priority 1: exact internal `nodeId`, Priority 2: explicit `nodeIdentity` key, Priority 3: exact visible `text`, Priority 4: exact `contentDescription`, Priority 5: view ID resource match).
- **`LiveTargetResolver`**: Re-acquires live `AccessibilityNodeInfo` references from interactive window root nodes, matching semantic properties and ascending to actionable parent containers when necessary.

### Phase 7 — UI Action Execution Engine & Verification
- **`UiActionExecutor`**: Dispatches framework accessibility actions (`ACTION_CLICK`, `ACTION_LONG_CLICK`, `ACTION_SET_TEXT`, `ACTION_SCROLL_FORWARD`, `ACTION_SCROLL_BACKWARD`) against Phase 6-resolved targets.
- **Action Execution Fallbacks**: Implements parent container fallback for `UI_CLICK` (ascending to clickable parent nodes if child node `performAction` returns `false`) and input activation retries for `UI_TEXT_INPUT` (`ACTION_FOCUS`/`ACTION_CLICK` before retrying `ACTION_SET_TEXT`).
- **Post-Action Verification**: `TargetAwareVerificationStrategy` enforces action settlement delays (`600ms` for clicks, `400ms` for text inputs, `800ms` for scrolls) before capturing post-action observation snapshots to verify UI state changes.

### Phase 8 — Application Control Engine & Foreground-First Pipeline
- **`AppResolver`**: Resolves application packages and labels via `PackageManager` (mapping queries like `"calculator"` -> `"com.transsion.calculator"` or `"com.android.calculator2"`).
- **`AppLauncher`**: Launches target applications or brings existing instances to the foreground without unnecessary relaunching.
- **`LaunchVerifier`**: Polls active accessibility foreground events (`activePackageName`) up to a 3000ms bounded timeout to confirm that the target application has become active.
- **Foreground-First Pipeline Integration**: `UiActionExecutor` verifies and brings background target applications to the foreground before capturing fresh observations and resolving live targets.

---

## Command Reference & Syntax

All command entry channels (Console, Floating Overlay, Automated Scripts) support the following command syntax:

| Command Syntax | Example Usage | Description & Pipeline Path |
| :--- | :--- | :--- |
| `launch <app>` | `launch calculator`, `launch settings` | Resolves app query via `AppResolver`, launches activity via `AppLauncher`, and verifies active package transition via `LaunchVerifier` within 3s timeout. |
| `click <target>` | `click 7`, `click AC`, `click submit` | Verifies foreground app status, captures fresh observation, resolves semantic target node, re-acquires live target, dispatches click (with parent escalation fallback), and verifies post-action UI change. |
| `long click <target>` | `long click card_item` | Executes a long click action on the resolved target node. |
| `scroll down` / `scroll forward` | `scroll down` | Performs `ACTION_SCROLL_FORWARD` on scrollable container targets. |
| `scroll up` / `scroll backward` | `scroll up` | Performs `ACTION_SCROLL_BACKWARD` on scrollable container targets. |
| `back` | `back` | Dispatches system global BACK navigation action via `GlobalActionExecutor`. |
| `home` | `home` | Dispatches system global HOME navigation action via `GlobalActionExecutor`. |
| `recents` | `recents` | Dispatches system global RECENTS navigation action via `GlobalActionExecutor`. |
| `observe` | `observe` | Captures and displays a fresh live accessibility snapshot tree. |
| `status` | `status` | Displays Agent status, Accessibility Service connection state, and storage availability. |

---

## User Interface & Console Inspection Screens

LocalAgent provides clean, dedicated screens for subsystem inspection and execution:

1. **Unified UI Observation Screen (`ObservationActivity`)**:
   - Single authoritative screen presenting the selected target application, foreground match status, window selection diagnostics, snapshot metadata, and structured accessible element hierarchy (`NodeTreeRenderer`).
   - Includes Refresh (`btnObserveUi`), Clear Display (`btnClearObservation`), and A11y Settings controls.
2. **Floating Command Console (`FloatingConsoleService`)**:
   - Movable system overlay view (`TYPE_APPLICATION_OVERLAY`) rendering above external applications (such as Calculator).
   - Features a draggable title bar, target package status label, single-line command input field, Execute button (with keyboard action support), Close button, and real-time execution status output.
3. **Permission Center (`PermissionActivity`)**:
   - Central authority for runtime permissions and Accessibility Settings launches.
4. **Evidence Diagnostics (`EvidenceActivity`)**:
   - Displays SHA-256 provenance hashes and structural node evidence for UI actions.
5. **Pipeline Event Log (`EventLogActivity`)**:
   - Displays historical pipeline audit events hydrated directly from `agent.db`.

---

## Build & Testing Verification

The project includes a 3-level testing suite (JVM Unit Tests, Robolectric Component Tests, and Integration Tests) executed via Gradle:

```bash
# Execute unit test suites across all modules
./gradlew test

# Execute app module Debug unit tests
./gradlew :app:testDebugUnitTest

# Execute app module Release unit tests
./gradlew :app:testReleaseUnitTest

# Execute core module unit tests
./gradlew :core:test

# Run Android Lint analysis
./gradlew lint

# Assemble Debug and Release APKs
./gradlew assembleDebug
./gradlew assembleRelease
```

---

## Physical Device Validation Gate (`P7-PHY-*`)

Physical device validation specifications (`P7-PHY-001` through `P7-PHY-008`) evaluate hardware interaction against physical touchscreen devices. In automated sandbox and Robolectric test environments, all eight specifications are strictly recorded as:

`PENDING PHYSICAL VALIDATION / NOT RUN`

Phase 8 meets all software acceptance criteria and unit test requirements. Physical hardware validation remains pending until hardware test execution evidence is reviewed.
