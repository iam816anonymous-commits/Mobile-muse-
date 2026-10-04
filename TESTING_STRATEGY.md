# TESTING_STRATEGY.md — Master Testing Strategy & CI/CD Framework

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

## 2. GitHub CI/CD Foundation & Workflow Architecture

The CI/CD pipeline (`.github/workflows/ci.yml`) executes automated validation on every push and pull request targeting development and main branches.

```text
PUSH / PULL REQUEST
       │
       ▼
1. Checkout Repository (actions/checkout@v4)
       │
       ▼
2. Toolchain Setup (JDK 17 Temurin, Gradle Build Action)
       │
       ▼
3. Run Level 1 Unit Tests (`./gradlew test`)
       │
       ▼
4. Assemble Debug APK (`./gradlew assembleDebug`)
       │
       ▼
5. Artifact Collection & Upload (actions/upload-artifact@v4)
   ├── `unit-test-reports` (Retention: 7 days)
   └── `localagent-debug-apk` (Retention: 14 days)
```

### Local Equivalent Commands
CI/CD uses identical local Gradle tasks to ensure 100% validation parity:
- **Run Unit Tests:** `./gradlew test` (or `gradle test`)
- **Assemble Debug APK:** `./gradlew assembleDebug` (or `gradle assembleDebug`)
- **Clean Build:** `./gradlew clean` (or `gradle clean`)

---

## 3. Detailed Level Specifications

### LEVEL 1 — CONTRACT / UNIT TESTS
- **Framework:** JUnit 4 / Kotlin Test.
- **Execution:** Fast local/CI execution (< 5 seconds) via `./gradlew test`.
- **Scope:** Pure Kotlin logic inside `core/` and `app/` modules. No physical UI or Android framework binding required.
- **Test Coverage:**
  - `NormalizedCommand` parsing and syntax validation.
  - `CommandRegistry` syntax lookup.
  - `ActionPolicyEngine` risk tier classification (LOW, MEDIUM, HIGH, CRITICAL).
  - `GoalDispatcher` priority queue scheduling & `ExecutionLock` thread safety.
  - `TaskLifecycle` state machine transitions (including LMK recovery transitions).

### LEVEL 2 — DEVICE SELF TESTS
- **Framework:** AndroidX Test Runner / Robolectric / Room Test DB / `TestCenter` UI.
- **Execution:** Run via `./gradlew connectedAndroidTest` or inside the app's Foundation Test UI / `TestCenter`.

### LEVEL 3 — CROSS-APP E2E TESTS
- **Framework:** Android UIAutomator / `AgentAccessibilityService`.
- **Primary Baseline Target:** Physical Android 8.1 (API 27) reference device or emulator.
