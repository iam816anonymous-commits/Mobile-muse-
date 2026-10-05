# TESTING.md — Developer Guide for Running & Inspecting LocalAgent Tests

## 1. Overview

This document explains how developers, architects, and automated CI pipelines inspect, execute, and verify tests for **LocalAgent**.

---

## 2. Test Classification & Phase Scope Governance

All test creation and execution is governed by the **Phase Scope Governance Rule** defined in [`docs/PHASE_SCOPE_GOVERNANCE_RULE.md`](docs/PHASE_SCOPE_GOVERNANCE_RULE.md).

Tests MUST follow the same phase boundaries as production code. A test does NOT belong to the current phase merely because it is easy to execute. Never create future-phase tests merely to artificially increase the current phase's test count.

LocalAgent strictly distinguishes automated unit tests from physical device verification:
1. **Pure JVM Unit Tests:** Execute in pure Java/Kotlin JVM environment (`core/src/test/`). 19 test methods active.
2. **Robolectric Integration Tests:** Execute Android framework, Accessibility window candidates, and Room database logic in Robolectric runner (`app/src/test/`). 19 test methods active.
3. **Automated Simulation Tests:** Robolectric tests simulating app-private directory deletion (`testDurableMemorySurvivesAppPrivateDirectoryDeletion`). These verify storage fallback logic, but do NOT count as proof of physical Android OS uninstall/reinstall survival.
4. **Physical Device Test Procedures:** Step-by-step manual test procedures for physical phone hardware (`PHYSICAL_DEVICE_TEST_PLAN.md`). Marked `NOT_RUN` until executed on a physical Android phone.

---

## 3. Frequently Asked Questions

### 1. Where are the executable tests?
Executable tests reside in standard Gradle source sets:
- Pure Kotlin JVM unit tests: `core/src/test/java/com/localagent/core/`
- Android Robolectric & Storage integration tests: `app/src/test/java/com/localagent/app/`
- On-device instrumentation tests: `app/src/androidTest/java/com/localagent/app/`

### 2. Where are the test specifications and traceability matrices?
Test specifications and requirement matrices are organized by phase:
- Matrix: `tests/TEST_TRACEABILITY_MATRIX.md`
- Phase Scope Governance: `docs/PHASE_SCOPE_GOVERNANCE_RULE.md`
- Phase 0 Spec: `tests/phase-00/PHASE_00_TEST_CASES.md`
- Phase 1 Spec: `tests/phase-01/PHASE_01_TEST_CASES.md`
- Phase 2 Spec: `tests/phase-02/PHASE_02_TEST_CASES.md`
- Phase 3.1 Spec: `tests/phase-03/PHASE_03_TEST_CASES.md`

### 3. Where are the test reports?
- Phase Test Reports: `docs/PHASE_0_TEST_REPORT.md`, `docs/PHASE_1_TEST_REPORT.md`, `docs/PHASE_2_TEST_REPORT.md`, `docs/PHASE_3_1_TEST_REPORT.md`
- System Audit Reports: `docs/TEST_SYSTEM_AUDIT_REPORT.md`, `docs/TEST_SYSTEM_FINAL_AUDIT.md`
- JUnit HTML Reports: `core/build/reports/tests/test/index.html` and `app/build/reports/tests/testDebugUnitTest/index.html`
- Android Lint Report: `app/build/reports/lint-results-debug.html`

### 4. How do I run all automated JVM unit and integration tests?
Execute:
```bash
./gradlew test
```

### 5. How do I run only Phase 1 tests?
Execute:
```bash
./gradlew :core:test
```

### 6. How do I run only Phase 2/3 storage, UI, and observation tests?
Execute:
```bash
./gradlew :app:testDebugUnitTest
```

### 7. How do I run Android Lint static analysis?
Execute:
```bash
./gradlew lint
```

### 8. How do I build the debug APK?
Execute:
```bash
./gradlew assembleDebug
```

### 9. How do I run physical-device manual test procedures?
Follow the step-by-step physical phone instructions in `PHYSICAL_DEVICE_TEST_PLAN.md`.

### 10. How do I map a test failure back to a phase requirement?
Lookup the failing test class or method in `tests/TEST_TRACEABILITY_MATRIX.md` to find its stable Test ID (e.g. `P1-CMD-002` or `P2-MEM-001`), requirement text, and specification document.

---

## 4. GitHub Actions CI Test Artifacts

GitHub Actions runs `./gradlew test lint assembleDebug` on every push and pull request. CI test artifacts preserved under Actions include:
- `unit-test-reports`: HTML and XML results from `:core` and `:app`
- `lint-reports`: Android Lint static analysis HTML report
- `localagent-debug-apk`: Compiled debug APK binary
