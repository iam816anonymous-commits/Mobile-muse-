# TESTING_STRATEGY.md — Master Testing Strategy & Certification Framework

## 1. Executive Summary & Testing Philosophy

Testing in **LocalAgent** is a core product feature and architectural gatekeeper. Unit tests alone are insufficient to prove real-world Android device behavior.

### Testing Mandates
1. **Multi-Tiered Test Architecture:** Tests span pure domain unit tests, in-app contract & parity tests, Android framework instrumentation tests, and physical real-device cross-application verification tests.
2. **Mandatory Physical Device Baseline:** The primary physical test baseline is a **physical device or emulator running Android 8.1 / API level 27**.
3. **Cross-Application Testing via UIAutomator & Accessibility:** Espresso is restricted to in-app testing. Cross-app and device automation testing uses `UIAutomator` and `AgentAccessibilityService`.
4. **Cross-Channel Parity Testing:** Every action MUST be tested across Console, Movable Overlay, and Voice to verify execution parity via `GoalDispatcher`.
5. **No Phase Complete Without Tests:** Every phase defines its automated test suite in its Definition of Done.

---

## 2. Multi-Tiered Testing Pyramid

```text
                                 ▲
                                / \
                               /   \   Tier D: Resource & Low-RAM Tests
                              /     \  (Memory Leaks, LMK Death, WAL Checkpoints)
                             /───────\
                            /         \   Tier C: Real-Device & UIAutomator Tests
                           /           \  (Cross-App Verification, Calculator/Settings)
                          /─────────────\
                         /               \   Tier B: Contract, Parity & Instrumentation Tests
                        /                 \  (TestCenter UI, Cross-Channel Parity, A11y Binding)
                       /───────────────────\
                      /                     \   Tier A: Pure Kotlin Domain Unit Tests
                     /                       \  (Command Parsing, Target Resolvers, Policy Engine)
                    /─────────────────────────\
```

---

## 3. Detailed Test Tier Specifications

### Tier A: Pure Domain Unit Tests
- **Framework:** JUnit 5 / MockK / Kotlin Test.
- **Scope:** Pure Kotlin logic inside `core/`, `storage/`, `solver/`, `research/`, and `memory/` modules.
- **Test Components:**
  - `NormalizedCommand` parsing and syntax validation.
  - `CommandRegistry` syntax lookup.
  - `ActionPolicyEngine` risk tier checking (LOW, MEDIUM, HIGH, CRITICAL).
  - `TargetResolver` ancestor search logic (Clickable, Scrollable, Editable).
  - `SnapshotDiffEngine` state-diff logic.
  - `EventLogger` batching and log retention math.
  - Prompt injection tag wrapping and untrusted data isolation.
- **Execution:** Fast execution (< 5 seconds total) via `./gradlew test`.

### Tier B: Contract, Parity & Instrumentation Tests
- **Framework:** AndroidX Test Runner / Robolectric / Room Test DB / `TestCenter` UI.
- **Scope:** Android framework-dependent components, in-app action contracts, and cross-channel parity without requiring a physical device.
- **Test Components:**
  - Contract execution suites: Verify that dispatching `NormalizedCommand` via Console, Overlay, and Voice hits the exact same `GoalDispatcher` and produces identical `AgentEvent` traces.
  - Unified SQLite `agent.db` migrations and query execution on API 27.
  - `PermissionManager` settings intent generation and passive degradation semantics.
  - `ObservationSnapshotGenerator` single-root node recycling.
  - `AgentAccessibilityService` connection/disconnection broadcasts.
- **Execution:** Run via `./gradlew connectedAndroidTest` or inside the app's `TestCenter` UI.

### Tier C: Real-Device & UIAutomator Cross-App Tests
- **Framework:** Android UIAutomator / `AgentAccessibilityService`.
- **Primary Baseline Target:** Physical Android 8.1 (API 27) device.
- **Scope:** End-to-end device interactions across third-party applications (Settings, Calculator, Files, Chrome).
- **Mandatory Action Test Matrix:**

| Test ID | Command | Target App | Expected Pre-State | Expected Post-State | Pass Criteria |
|---|---|---|---|---|---|
| `TEST-ACT-001` | `launch Settings` | Settings (`com.android.settings`) | Launcher Foreground | Settings Activity Foreground | Package == `com.android.settings` |
| `TEST-ACT-002` | `click "7"` | Calculator (`com.google.android.calculator`) | Blank Formula Field | "7" appended in Formula Field | Formula text == "7" & UI Diff Verified |
| `TEST-ACT-003` | `click "+"` | Calculator (`com.google.android.calculator`) | Formula "7" | Formula "7+" | Formula text == "7+" & UI Diff Verified |
| `TEST-ACT-004` | `scroll down` | Settings (`com.android.settings`) | Top of Settings list | Lower items visible | Scroll container shift diff > 0 |
| `TEST-ACT-005` | `back` | Any foreground app | App in foreground | Previous screen or Launcher | Navigation-aware window/package diff |
| `TEST-ACT-006` | `home` | Any foreground app | App in foreground | Home Launcher in foreground | Foreground package == Launcher |
| `TEST-ACT-007` | `recents` | Any foreground app | App in foreground | Recents UI visible | Recents window state diff |

### Tier D: Resource & Low-RAM Stress Tests
- **Target Platform:** Android 8.1 API 27 low-RAM device (1 GB RAM target).
- **Execution Checks:**
  1. **Continuous Dispatch Leak Test:** Execute 1,000 sequential `UI_CLICK` actions while monitoring process heap via `Runtime.getRuntime().freeMemory()`. Heap footprint must remain under 35 MB with zero `OutOfMemoryError` or un-recycled native nodes.
  2. **LMK Death Recovery Test:** Trigger simulated process kill via `adb shell am kill com.localagent.app`. Confirm process restarts cleanly, restores persistent state from `agent.db`, and resumes without crashing.
  3. **Log Storage & WAL Checkpoint Cap Test:** Generate 100,000 continuous event logs. Verify `agent.db` + `agent.db-wal` file size remains under 30 MB cap and older logs are purged automatically.

---

## 4. Master Test Runner Component (`TestCenter`)

In addition to Gradle test tasks, the app itself contains a built-in **Diagnostic Test Center UI**:

```text
                     ┌───────────────────────────────────────────────┐
                     │            LOCALAGENT TEST CENTER             │
                     │  [ Run Foundation & Policy Tests ]            │
                     │  [ Run Action Contract & Parity Suite ]       │
                     │  [ Run Permission & Degradation Diagnostics ]  │
                     │  [ Run Full Master Certification ]            │
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
                     │   PASS: 52 | FAIL: 0 | SKIPPED: 2             │
                     │   Saved to: evidence/certification-report.json│
                     └───────────────────────────────────────────────┘
```
