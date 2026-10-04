# TESTING_STRATEGY.md — Master Testing Strategy & Certification Framework

## 1. Executive Summary & Testing Philosophy

Testing in **LocalAgent** is a core product feature and architectural gatekeeper. Unit tests alone are insufficient to prove real-world Android device behavior.

### Testing Mandates
1. **Three Distinct Testing Levels:**
   - **LEVEL 1 — CONTRACT / UNIT:** Pure Kotlin domain tests (no physical UI/device interaction).
   - **LEVEL 2 — DEVICE SELF TEST:** In-app `TestCenter` running real framework services on device.
   - **LEVEL 3 — CROSS-APP E2E:** Real cross-application automation across Settings, Calculator, Files, Chrome.
2. **Contract Pass != Device Execution Pass:** A unit or contract test passing in memory is NEVER accepted as proof that Android executed the action on real hardware.
3. **Mandatory Physical Baseline:** Physical device or emulator running **Android 8.1 / API level 27**.
4. **Cross-Application Automation via UIAutomator & Accessibility:** Espresso is restricted to in-app testing. Cross-app testing uses `UIAutomator` and `AgentAccessibilityService`.
5. **Cross-Channel Parity Testing:** Every action MUST be tested across Console, Movable Overlay, and Voice to verify execution parity via `GoalDispatcher`.

---

## 2. Three-Level Test Architecture

```text
                                 ▲
                                / \
                               /   \   LEVEL 3: CROSS-APP E2E TESTS
                              /     \  (Real App Automation: Settings, Calculator, Files, Chrome)
                             /───────\
                            /         \   LEVEL 2: DEVICE SELF TESTS
                           /           \  (Real Framework Services, Room DB, A11y Binding, Overlay)
                          /─────────────\
                         /               \   LEVEL 1: CONTRACT / UNIT TESTS
                        /                 \  (Command Parsing, Policy Engine, Diff Math, Queue Priority)
                       /───────────────────\
```

---

## 3. Detailed Level Specifications

### LEVEL 1 — CONTRACT / UNIT TESTS
- **Framework:** JUnit 5 / MockK / Kotlin Test.
- **Execution:** Fast local execution (< 5 seconds) via `./gradlew test`.
- **Scope:** Pure Kotlin logic inside `core/`, `storage/`, `solver/`, `research/`, `voice/`, and `memory/` modules. No physical UI or Android framework binding required.
- **Test Coverage:**
  - `NormalizedCommand` parsing and syntax validation.
  - `CommandRegistry` syntax lookup.
  - `ActionPolicyEngine` risk tier classification (LOW, MEDIUM, HIGH, CRITICAL).
  - `TargetResolver` ancestor search algorithms (Clickable, Scrollable, Editable).
  - `NodeIdentityConfidence` classification math.
  - `SnapshotDiffEngine` state-diff logic.
  - `EventLogger` batching, SQLite query generation, and WAL checkpoint retention math.
  - Prompt injection tag wrapping and untrusted data isolation.

### LEVEL 2 — DEVICE SELF TESTS
- **Framework:** AndroidX Test Runner / Robolectric / Room Test DB / `TestCenter` UI.
- **Execution:** Run via `./gradlew connectedAndroidTest` or inside the app's `TestCenter` UI.
- **Scope:** Real Android framework-dependent components running on an Android OS instance.
- **Test Coverage:**
  - Contract execution suites: Verify that dispatching `NormalizedCommand` via Console, Overlay, and Voice hits the exact same `GoalDispatcher` and produces identical `AgentEvent` traces.
  - Unified SQLite `agent.db` migrations, query performance, and `PRAGMA wal_checkpoint(TRUNCATE)` on API 27.
  - `PermissionManager` settings intent generation and passive degradation semantics.
  - `ObservationSnapshotGenerator` single-root node recycling.
  - `AgentAccessibilityService` connection/disconnection broadcasts and passive degradation.

### LEVEL 3 — CROSS-APP E2E TESTS
- **Framework:** Android UIAutomator / `AgentAccessibilityService`.
- **Primary Baseline Target:** Physical Android 8.1 (API 27) reference device or emulator.
- **Scope:** End-to-end device interactions across third-party applications (Settings, Calculator, Files, Chrome).
- **Mandatory Action Test Matrix:**

| Test ID | Command | Target App | Expected Pre-State | Expected Post-State | Pass Criteria |
|---|---|---|---|---|---|
| `TEST-ACT-001` | `launch Settings` | Settings (`com.android.settings`) | Launcher Foreground | Settings Activity Foreground | Package == `com.android.settings` |
| `TEST-ACT-002` | `click "7"` | Calculator (`com.google.android.calculator`) | Blank Formula Field | "7" appended in Formula Field | Formula text == "7" & TargetClickStrategy Verified |
| `TEST-ACT-003` | `click "+"` | Calculator (`com.google.android.calculator`) | Formula "7" | Formula "7+" | Formula text == "7+" & TargetClickStrategy Verified |
| `TEST-ACT-004` | `scroll down` | Settings (`com.android.settings`) | Top of Settings list | Lower items visible | ScrollVerificationStrategy Verified |
| `TEST-ACT-005` | `back` | Any foreground app | App in foreground | Previous screen or Launcher | NavigationAwareBackStrategy Verified |
| `TEST-ACT-006` | `home` | Any foreground app | App in foreground | Home Launcher in foreground | Foreground package == Launcher |
| `TEST-ACT-007` | `recents` | Any foreground app | App in foreground | Recents UI visible | Recents window state diff |

---

## 4. Resource & Low-RAM Stress Tests (Level 3 Validation)

- **Target Platform:** Android 8.1 API 27 low-RAM device (1.5 GB RAM reference profile).
- **Execution Checks:**
  1. **Continuous Dispatch Leak Test:** Execute 1,000 sequential `UI_CLICK` actions while monitoring process heap via `Runtime.getRuntime().freeMemory()`. Heap footprint must remain within engineering targets (< 35 MB) with zero `OutOfMemoryError` or un-recycled native nodes.
  2. **LMK Death Recovery Test:** Trigger simulated process kill via `adb shell am kill com.localagent.app`. Confirm process restarts cleanly, restores persistent state from `agent.db`, and resumes without crashing.
  3. **Log Storage & WAL Checkpoint Cap Test:** Generate 100,000 continuous event logs. Verify `agent.db` + `agent.db-wal` file size remains under 30 MB cap and older logs are purged automatically.

---

## 5. Master Test Runner Component (`TestCenter`)

In addition to Gradle test tasks, the app itself contains a built-in **Diagnostic Test Center UI**:

```text
                     ┌───────────────────────────────────────────────┐
                     │            LOCALAGENT TEST CENTER             │
                     │  [ Run Level 1: Contract & Policy Suite ]     │
                     │  [ Run Level 2: Device Service Self Tests ]   │
                     │  [ Run Level 3: Cross-App E2E Test Suite ]    │
                     │  [ Run Master System Certification ]          │
                     └───────────────────────┬───────────────────────┘
                                             │
                                             ▼
                     ┌───────────────────────────────────────────────┐
                     │          In-App Master Test Runner            │
                     │   Executes test contracts sequentially        │
                     │   Generates diagnostic evidence log           │
                     └───────────────────────┬───────────────────────┘
                                             │
                                             ▼
                     ┌───────────────────────────────────────────────┐
                     │          Certification Report Output          │
                     │   PASS: 58 | FAIL: 0 | SKIPPED: 2             │
                     │   Saved to: evidence/certification-report.json│
                     └───────────────────────────────────────────────┘
```
