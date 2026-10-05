# Phase 4 Test Cases — Accessibility Service Foundation

## 1. Executive Summary
This document specifies the executable test suite for **Phase 4 — Accessibility Service Foundation**.

All tests verify service lifecycle transitions (`BOUND`, `CONNECTING`, `UNBOUND`, `DEGRADED`), passive degradation logging, system accessibility state observation, and read-only observation preservation without performing automated action execution or violating phase boundaries.

---

## 2. Test Cases Specification

### Test ID: P4-ACC-001
- **Requirement:** AgentAccessibilityService binding lifecycle monitoring
- **Purpose:** Verify that service connection notifies `AccessibilityServiceConnectionMonitor` and updates state to `BOUND`.
- **Preconditions:** LocalAgent application initialized.
- **Input:** `AgentAccessibilityService.onServiceConnected()` lifecycle trigger.
- **Expected Result:** Connection monitor state transitions to `BOUND`. Structured `AGENT_SERVICE_CONNECTED` event logged to `agent.db`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AccessibilityServiceConnectionMonitorTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** `android.permission.BIND_ACCESSIBILITY_SERVICE`
- **Hardware:** Baseline API 27+
- **Evidence:** `agent.db` event log entry `AGENT_SERVICE_CONNECTED` with `status: BOUND`.
- **Status:** PASS

### Test ID: P4-ACC-002
- **Requirement:** AgentAccessibilityService unbinding and passive degradation
- **Purpose:** Verify that service unbinding updates state to `DEGRADED` and logs disconnection without crashing.
- **Preconditions:** Service previously bound or active.
- **Input:** `AgentAccessibilityService.onUnbind()` or `onDestroy()` trigger.
- **Expected Result:** Connection monitor state updates to `DEGRADED`. Structured `AGENT_SERVICE_DISCONNECTED` event logged to `agent.db`. Diagnostic UI displays passive degradation banner. Zero crashes.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AccessibilityServiceConnectionMonitorTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `agent.db` event log entry `AGENT_SERVICE_DISCONNECTED` with `reason: ON_UNBIND`.
- **Status:** PASS

### Test ID: P4-LIFE-001
- **Requirement:** System AccessibilityStateChangeListener integration
- **Purpose:** Verify that changes in system-wide accessibility setting notify listeners and adjust monitor state.
- **Preconditions:** `AccessibilityServiceConnectionMonitor` initialized with `AccessibilityManager`.
- **Input:** System accessibility state callback `onAccessibilityStateChanged(enabled)`.
- **Expected Result:** System state change event `SYSTEM_ACCESSIBILITY_STATE_CHANGED` logged. Monitor updates `isSystemAccessibilityEnabled`.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/accessibility/AccessibilityServiceConnectionMonitorTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Event log entry `SYSTEM_ACCESSIBILITY_STATE_CHANGED`.
- **Status:** PASS

### Test ID: P4-LIFE-002
- **Requirement:** PermissionManager integration with Accessibility Lifecycle State
- **Purpose:** Verify `PermissionManager.checkAccessibilityPermission()` accurately reflects `AccessibilityLifecycleState`.
- **Preconditions:** `PermissionManager` initialized.
- **Input:** Query `checkAccessibilityPermission()`.
- **Expected Result:** Status reports `SPECIAL_ACCESS_GRANTED` when `BOUND`, and `SPECIAL_ACCESS_DENIED` when `UNBOUND`/`DEGRADED`. Detail text displays active lifecycle state string.
- **Test Type:** Tier B Robolectric Test
- **Executable Location:** `app/src/test/java/com/localagent/app/system/PermissionManagerTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** `PermissionDescriptor` status string matching `AccessibilityLifecycleState`.
- **Status:** PASS

### Test ID: P4-DEG-001
- **Requirement:** Read-only observation preservation during service degradation
- **Purpose:** Confirm that requesting UI observation while service is unbound fails safely without crashing or performing action execution.
- **Preconditions:** Accessibility service unbound (`isBound == false`).
- **Input:** Trigger `captureLiveSnapshot()` or tap "Observe Current UI".
- **Expected Result:** `PASSIVE_DEGRADATION_DETECTED` event logged. Diagnostic screen displays guidance banner directing user to Permission Center. Zero action execution.
- **Test Type:** Tier B Robolectric Test / Instrumentation Test
- **Executable Location:** `app/src/test/java/com/localagent/app/ui/ObservationActivityTest.kt`
- **Execution Command:** `./gradlew test --offline`
- **Permissions:** None
- **Hardware:** Baseline API 27+
- **Evidence:** Event log entry `PASSIVE_DEGRADATION_DETECTED`.
- **Status:** PASS
