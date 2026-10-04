# MEMORY_AND_LEARNING.md — Memory & Learning Subsystem Architecture

## 1. Executive Summary

**LocalAgent** strictly separates **audit logs** ("what happened") from **memory and learning** ("what the agent knows and how it performs tasks").

### Architectural Boundaries
1. **Logs Are Not Memory:** Event logs capture structured execution traces for auditability and debugging. Memory extracts reusable knowledge, app structures, and task procedures.
2. **Revalidation Before Replay:** The agent must **never blindly replay** a learned sequence of target nodes. Every procedural workflow step re-validates the live UI tree to resolve current target nodes before executing an action.
3. **Local & Persistent:** All memory is stored locally in app-private storage (`/data/data/com.localagent.app/files/agent/memory/`).

---

## 2. Memory Subsystem Architecture

```text
                     ┌───────────────────────────────────────────────┐
                     │            Universal Event Stream             │
                     │  Actions, Observations, Verification Results  │
                     └───────────────────────┬───────────────────────┘
                                             │
                                             ▼
                     ┌───────────────────────────────────────────────┐
                     │            Memory Pipeline Engine             │
                     └───────┬───────────────┬───────────────┬───────┘
                             │               │               │
                             ▼               ▼               ▼
                   ┌─────────────────┐ ┌───────────┐ ┌───────────────┐
                   │  Episodic DB    │ │Procedural │ │ Semantic DB   │
                   │ Task Execution  │ │ Workflows │ │ App Structure │
                   │  & Diagnostics  │ │ Definitions│ │  & Vocabulary │
                   └─────────────────┘ └───────────┘ └───────────────┘
```

---

## 3. Four Tiers of Agent Memory

### 3.1 Working Memory (Short-Term / Ephemeral)
- **Scope:** In-memory state maintained during active task execution.
- **Contents:** Current goal, active step index, current UI snapshot diff, retry attempts counter, variables extracted during step execution.
- **Lifecycle:** Cleared immediately upon task completion, cancellation, or failure.

### 3.2 Episodic Memory (Task Experience History)
- **Scope:** Persistent execution history stored in SQLite (`memory/episodic.db`).
- **Contents:** Task goal summary, package/activity context, executed action sequence, success/failure result, total duration, verification diff summary.
- **Retention:** Max 500 recent episodes or 14 days, auto-pruned.

### 3.3 Semantic Memory (App Structure & Domain Knowledge)
- **Scope:** Persistent knowledge about applications, package labels, and custom semantic mappings stored in SQLite (`memory/semantic.db`).
- **Contents:**
  - Package to app label mappings (e.g., `"Settings"` → `"com.android.settings"`).
  - UI layout patterns (e.g., *"Calculator digit buttons contain non-clickable TextViews inside clickable MaterialButtons"*).
  - User-approved preferences (e.g., *"Preferred media player = Spotify"*).

### 3.4 Procedural Memory (Learned Workflows & Sequences)
- **Scope:** Reusable multi-step automation workflows stored in JSON/SQLite (`memory/workflows.db`).
- **Contents:** Trigger conditions, parameter definitions, sequential action templates, expected pre/post verification criteria.
- **Revalidation Protocol:** When replaying a procedural workflow, the agent executes target resolution against the live `ObservationSnapshot` for every step. If target resolution fails or UI structure has changed, the agent aborts procedural replay and falls back to deterministic planning or prompts the user.

---

## 4. Learning from Experience Engine

```kotlin
data class LearnedWorkflow(
    val workflowId: String = UUID.randomUUID().toString(),
    val name: String, // e.g., "Open Calculator and Add Numbers"
    val targetPackage: String,
    val triggerIntent: String,
    val steps: List<WorkflowStep>,
    val successCount: Int = 1,
    val failureCount: Int = 0,
    val confidenceScore: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis()
)

data class WorkflowStep(
    val stepNumber: Int,
    val actionType: ActionType,
    val targetSelector: TargetSelector,
    val parameterKey: String?,
    val expectedVerification: VerificationCriterion
)
```

### Automatic Workflow Discovery Flow
1. User executes a successful sequence of actions manually via Console, Overlay, or Voice (e.g., `launch Calculator` → `click 7` → `click +` → `click 5` → `click =`).
2. The `WorkflowExtractor` detects a completed goal state.
3. If the sequence succeeded with 100% verified UI diffs, the agent prompts: *"Save this 5-step sequence as a shortcut workflow?"*
4. Upon user approval, the sequence is saved to `Procedural Memory` with a user-assigned label.
5. On subsequent runs, issuing the label executes the procedural workflow with live node revalidation at each step.
