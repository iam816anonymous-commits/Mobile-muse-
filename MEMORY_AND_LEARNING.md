# MEMORY_AND_LEARNING.md — Memory, Learning & Task Lifecycle Architecture

## 1. Executive Summary

**LocalAgent** strictly separates **audit logs** ("what happened") from **memory and learning** ("what the agent knows and how it performs tasks"). Furthermore, it establishes a foundational distinction between **App-Private Storage** and **Durable Agent Memory Persistence**.

### Architectural Boundaries & Long-Term Memory Rules
1. **Logs Are Not Memory:** Event logs capture structured execution traces for auditability and debugging. Memory extracts reusable knowledge, app structures, and task procedures.
2. **Revalidation Before Replay:** The agent must **never blindly replay** a learned sequence of target nodes. Every procedural workflow step re-validates the live UI tree to resolve current target nodes before executing an action.
3. **App-Private Storage vs. Durable Agent Memory:**
   - **App-Private Storage (`/data/data/com.localagent.app/files/agent/agent.db`):** Holds operational state, working memory, active task queues, temporary caches, and short-term audit event logs. Subject to total deletion upon application uninstall or "Clear Data".
   - **Durable Agent Memory Storage (`DurableMemoryStorageProvider`):** Holds long-term learned behaviors, user preferences, procedural workflows, and semantic knowledge. Uses durable external locations (`/sdcard/LocalAgent/memory` or user-granted Storage Access Framework document trees) to **survive application uninstall/reinstall, app updates, and process termination**.
4. **Availability Transparency:** If durable external storage is unavailable or permission is denied, the agent explicitly reports `StorageAvailabilityStatus.AVAILABLE_APP_PRIVATE_ONLY` rather than pretending that memory is durable across uninstalls.

---

## 2. Goal State & Task Lifecycle Architecture

LocalAgent manages task execution using explicit state machines to guarantee thread safety, concurrency control, and crash recovery.

### 2.1 Goal State Model (`GoalState`)
Higher-level user goals (e.g., *"Open Calculator and calculate 125 x 37"*) progress through explicit goal states:

```kotlin
enum class GoalState {
    CREATED,      // Goal received and parsed
    PLANNING,     // Goal decomposed into sequential NormalizedCommands
    READY,        // Sub-commands queued in GoalDispatcher
    EXECUTING,    // Active command dispatched to device
    WAITING,      // Settle delay or post-action observation capture
    VERIFYING,    // Action-specific VerificationStrategy evaluating UI state diff
    COMPLETED,    // Goal criteria satisfied and verified
    FAILED,       // Sub-step failed or state diff unverified after retries
    CANCELLED,    // Explicitly cancelled by user via Overlay/Console
    INTERRUPTED,  // Interrupted by Low Memory Killer (LMK) process termination
    RECOVERING    // Process restarted; re-evaluating active window to resume
}

data class GoalVerification(
    val goalId: String,
    val targetCriterion: VerificationCriterion,
    val isGoalSatisfied: Boolean,
    val verificationTimestamp: Long = System.currentTimeMillis()
)
```

### 2.2 Task Lifecycle Contract (`TaskLifecycle`)
Individual tasks and sub-commands obey a strict state machine with defined legal transitions:

```text
  [CREATED] ──► [QUEUED] ──► [RUNNING] ──► [WAITING] ──► [VERIFYING] ──► [COMPLETED]
                   │             │             │             │
                   ▼             ▼             ▼             ▼
              [CANCELLED]   [CANCELLING]   [FAILED]    [INTERRUPTED]
                                                              │
                                                              ▼
                                                        [RECOVERING]
```

#### Task Recovery Protocol
- **Process Death (LMK):** Synchronous SQLite commits ensure state is saved before transitions. Upon restart, `TaskLifecycle` queries `agent.db` for tasks in `INTERRUPTED` state, captures a fresh `ObservationSnapshot`, and prompts user or resumes.
- **Accessibility Service Disconnect:** Tasks transition to `WAITING` with error `ACCESSIBILITY_UNAVAILABLE`. Passive subsystems remain active.
- **Overlay Service Restart:** Overlay re-binds to `GoalDispatcher` state without interrupting running background tasks.
- **Device Rotation / Screen Off:** Active step pauses in `WAITING` state until screen power state turns on or window settles.

---

## 3. Concurrency & Execution Lock Policy

Device UI automation requires strict single-threaded access to the Android Accessibility Service.

### Execution Lock Policy Rules
1. **Single Foreground Device Transaction:** Only **ONE** active device-control transaction may manipulate the Accessibility execution channel at a time.
2. **Channel Convergence Queue:** Simultaneous requests from Console, Overlay, Voice, or AI Planners pass through `GoalDispatcher`'s thread-safe Priority Queue:
   ```text
   CONSOLE (Priority 1 - User Manual Override)
   OVERLAY (Priority 1 - User Manual Override)
   VOICE   (Priority 2 - Spoken Command)
   AI      (Priority 3 - Autonomous Step)
   ```
3. **Execution Lock:** Before executing an action, `GoalDispatcher` acquires `AccessibilityExecutionLock`. Secondary incoming actions wait in queue or return `RESULT_BUSY`.
4. **Asynchronous Non-Device Operations:** Logging, memory reading, web research, and solver math run asynchronously without locking the Accessibility channel.

---

## 4. Four Tiers of Agent Memory

### 4.1 Working Memory (Short-Term / Ephemeral)
- **Scope:** In-memory state maintained during active task execution.
- **Contents:** Current goal, active step index, current UI snapshot diff, retry attempts counter, variables extracted during step execution.
- **Lifecycle:** Cleared immediately upon task completion, cancellation, or failure.

### 4.2 Episodic Memory (Task Experience History)
- **Scope:** Persistent execution history stored in `episodes` table inside `agent.db`.
- **Contents:** Task goal summary, package/activity context, executed action sequence, success/failure result, total duration, verification diff summary.
- **Retention:** Max 500 recent episodes or 14 days, auto-pruned.

### 4.3 Semantic Memory (App Structure & Domain Knowledge)
- **Scope:** Persistent knowledge about applications, package labels, and custom semantic mappings stored in `semantic_data` table inside `agent.db` and synchronized with `DurableMemoryStorageProvider`.

### 4.4 Procedural Memory (Learned Workflows & Sequences)
- **Scope:** Reusable multi-step automation workflows stored via `DurableMemoryStorageProvider` in SHA-256 verified JSON records.
- **Revalidation Protocol:** Replaying a procedural workflow re-evaluates the live `ObservationSnapshot` for every step. Stale node references are NEVER replayed blindly.

---

## 5. Durable Memory Survival Matrix

| Event Type | App-Private DB (`agent.db`) | Durable Agent Storage (`DurableRecord`) |
|---|---|---|
| **App Restart** | Retained | Retained |
| **Process Death (LMK)** | Retained | Retained |
| **App Upgrade** | Retained | Retained |
| **Uninstall / Reinstall** | **Cleared** | **Retained** (Survives via `/sdcard/LocalAgent/memory` or SAF URI) |
| **Device Restart** | Retained | Retained |
