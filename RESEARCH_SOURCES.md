# RESEARCH_SOURCES.md — Research Sources & Architectural Inferences

## 1. Executive Summary

This document details all external research sources, official Android platform documentation, third-party automation tools, security research papers, and technical lessons that inform the architecture of **LocalAgent**.

Every major decision is categorized by:
- **SOURCE FACT:** Verified platform fact or API behavior from documentation.
- **MODEL / ARCHITECTURAL INFERENCE:** Technical design choice derived from facts.
- **PROPOSED LOCALAGENT DESIGN:** Specific architectural pattern implemented in LocalAgent.

---

## 2. Research Sources & Architectural Breakdown

### 2.1 MacroDroid Architecture Research
- **URL / Source:** [MacroDroid Official Website](https://macrodroid.com/) & [MacroDroid Helper](https://macrodroid.com/helper/)
- **SOURCE FACT:** MacroDroid uses a modular **Trigger → Action → Constraint** automation architecture. Its UI Interaction action depends on Android AccessibilityService to detect screen elements and perform touch interactions.
- **ARCHITECTURAL INFERENCE:** Automation capabilities should be modular, composable plugins rather than monolithic procedural scripts.
- **LOCALAGENT DESIGN:** LocalAgent adopts composable `Capability` blocks and a unified `GoalDispatcher` pipeline.

### 2.2 Automate by LlamaLab Research
- **URL / Source:** [Automate Documentation — Interact Block](https://llamalab.com/automate/doc/block/interact.html)
- **SOURCE FACT:** Automate warns that retaining large UI node hierarchies causes excessive RAM overhead and performance degradation. It recommends filtering processing rather than holding entire UI trees.
- **ARCHITECTURAL INFERENCE:** Accessibility node trees must be converted into lightweight primitive snapshots and immediately recycled.
- **LOCALAGENT DESIGN:** Implemented in `OBSERVATION_MODEL.md` via `ObservationSnapshot` and explicit `.recycle()` calls.

### 2.3 AutoInput Research
- **URL / Source:** [AutoInput on Google Play](https://play.google.com/store/apps/details?id=com.joaomgcd.autoinput)
- **SOURCE FACT:** AutoInput uses Android AccessibilityService for no-root UI interaction and screen text extraction across arbitrary third-party applications.
- **ARCHITECTURAL INFERENCE:** AccessibilityService is the single most viable non-root API for cross-application UI observation and action execution on Android.
- **LOCALAGENT DESIGN:** `AgentAccessibilityService` forms the primary execution engine.

### 2.4 Android Official Accessibility Service Documentation
- **URL / Source:** [Android Developers — AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- **SOURCE FACT:** `AccessibilityService.performGlobalAction()` provides system-level global navigation (`GLOBAL_ACTION_BACK`, `GLOBAL_ACTION_HOME`, `GLOBAL_ACTION_RECENTS`). `getSystemActions()` allows runtime querying of available global actions. `AccessibilityNodeInfo` objects can become stale as windows update.
- **ARCHITECTURAL INFERENCE:** `performAction() == true` merely indicates dispatch to the framework, not verified UI mutation. Live nodes must be reacquired before execution.
- **LOCALAGENT DESIGN:** Implemented in `ACTION_CONTRACTS.md` with explicit pre/post state-diff verification.

### 2.5 Android Special Permissions & Low-RAM Documentation
- **URL / Source:** [Android Developers — Request Special Permissions](https://developer.android.com/training/permissions/requesting-special) & [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- **SOURCE FACT:** Special permissions (`SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS`, `PACKAGE_USAGE_STATS`) require launching dedicated system settings flows. On Android Q and below, devices where `ActivityManager.isLowRamDevice()` returns true can restrict NotificationListenerService.
- **ARCHITECTURAL INFERENCE:** Special access features cannot be hard dependencies; the core agent must degrade gracefully when special permissions are restricted or denied.
- **LOCALAGENT DESIGN:** Centralized `PermissionManager` and in-app Permission Center UI (`PERMISSION_MATRIX.md`).

### 2.6 Android Low-RAM & Low Memory Killer (LMK) Guidance
- **URL / Source:** [Android Developers — Performance & Low Memory Killer](https://developer.android.com/topic/performance/issues/lmk)
- **SOURCE FACT:** On entry-level Android devices (1 GB–2 GB RAM), LMK terminates background processes under RAM pressure.
- **ARCHITECTURAL INFERENCE:** The agent must be zero-state in RAM, committing execution logs and task states to SQLite synchronously so it can recover cleanly after process death.
- **LOCALAGENT DESIGN:** Persistent SQLite storage and process recovery protocol (`LOW_RAM_DESIGN.md`).

### 2.7 Android Agent Security & Indirect Prompt Injection Research
- **URL / Source:** [arXiv:2608.08939 — Indirect Prompt Injection against Android UI Agents](https://arxiv.org/abs/2608.08939)
- **SOURCE FACT:** AI UI agents relying on accessibility metadata and screenshots are highly vulnerable to indirect prompt injection embedded in webpage text or third-party app UIs.
- **ARCHITECTURAL INFERENCE:** Screen text and external content must be treated strictly as UNTRUSTED DATA, never executable instructions.
- **LOCALAGENT DESIGN:** Implemented in `EXTERNAL_KNOWLEDGE_ARCHITECTURE.md` via `KnowledgeSanitizer` and `ActionPolicyEngine`.
