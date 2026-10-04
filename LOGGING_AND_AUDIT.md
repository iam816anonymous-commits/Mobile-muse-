# LOGGING_AND_AUDIT.md — Universal Structured Event Logging & Audit Subsystem

## 1. Executive Summary & Core Architectural Principles

Logging in **LocalAgent** is a first-class, core subsystem rather than a transient UI display or localized debug tool. Every command, target resolution, accessibility dispatch, permission change, voice STT/TTS event, hardware control, memory update, workflow step, error, and system diagnostic generates a **structured, persistent event**.

### Universal Logging Directives
1. **Universal Execution Trail:** All input channels (Console, Movable Overlay, Voice Input, Workflow Engine, Solvers, AI Planners) write structured logs to the single `EventLogger`.
2. **Unified Persistence (`agent.db`):** All persistent data (events, episodes, workflows, knowledge, semantic mappings) is consolidated into a single SQLite database (`/data/data/com.localagent.app/files/agent/agent.db`).
3. **Structured Schema:** Events contain correlation IDs, precise timestamps, subsystem source tags, execution duration, result codes, and structured JSON metadata.
4. **Bounded Storage & Comprehensive Accounting:** Storage calculations explicitly budget for SQLite database files (`agent.db`), Write-Ahead Logging files (`agent.db-wal`), and Shared Memory files (`agent.db-shm`). Total storage is capped at 30 MB maximum across all log and WAL artifacts.

---

## 2. Structured Event Schema (`AgentEvent`)

```kotlin
@Entity(tableName = "agent_events")
data class AgentEvent(
    @PrimaryKey
    val eventId: String = UUID.randomUUID().toString(),
    val correlationId: String, // Groups multi-step actions/workflows into one execution sequence
    val timestamp: Long = System.currentTimeMillis(),
    val subsystem: EventSubsystem, // COMMAND, ACCESSIBILITY, OBSERVATION, ACTION, PERMISSION, HARDWARE, VOICE, WORKFLOW, MEMORY, LEARNING, BROWSER, SOLVER, SYSTEM, TEST
    val eventType: String, // e.g., "COMMAND_RECEIVED", "TARGET_RESOLVED", "ACTION_DISPATCHED", "STATE_DIFF_VERIFIED"
    val actionType: String?, // e.g., "UI_CLICK", "GLOBAL_BACK", "APP_LAUNCH"
    val sourceChannel: String, // e.g., "CONSOLE", "OVERLAY", "VOICE", "AUTOMATION", "AI"
    val targetPackage: String?,
    val targetViewId: String?,
    val resultCode: String?, // e.g., "SUCCESS_VERIFIED", "DISPATCHED_UNVERIFIED", "TARGET_NOT_FOUND"
    val durationMs: Long = 0L,
    val severity: EventSeverity = EventSeverity.INFO, // DEBUG, INFO, WARNING, ERROR, CRITICAL
    val metadataJson: String = "{}" // Structured JSON map for contextual detail
)

enum class EventSeverity { DEBUG, INFO, WARNING, ERROR, CRITICAL }

enum class EventSubsystem {
    COMMAND, ACCESSIBILITY, OBSERVATION, ACTION, PERMISSION, HARDWARE, VOICE, WORKFLOW, MEMORY, LEARNING, BROWSER, SOLVER, SYSTEM, TEST
}
```

---

## 3. Persistent Storage Directory Structure & Unified Storage Budget

```text
/data/data/com.localagent.app/files/agent/
├── agent.db                  # Consolidated Primary SQLite / Room Database
│   ├── Table: agent_events   # Event and audit log stream
│   ├── Table: episodes       # Episodic task experience
│   ├── Table: workflows      # Procedural workflow definitions
│   ├── Table: semantic_data  # App structure & package mappings
│   └── Table: knowledge      # Untrusted external research & chat knowledge
├── agent.db-wal              # SQLite Write-Ahead Log file
├── agent.db-shm              # SQLite Shared Memory index file
├── evidence/                 # Failure diagnostic dumps (Max 5 MB)
│   ├── failure-snap-001.json
│   └── failure-snap-001.png
└── exports/                  # User-initiated log exports
```

### Comprehensive Storage Budget Accounting
- **Total Storage Cap:** **30 MB Maximum** for entire `/data/data/com.localagent.app/files/agent/` folder.
- **Unified DB Budget Calculation:**
  `Total DB Footprint = FileSize(agent.db) + FileSize(agent.db-wal) + FileSize(agent.db-shm)`
- **Automated Retention Trigger:**
  1. Whenever a log batch is committed, `LogRetentionManager` computes `Total DB Footprint`.
  2. If `Total DB Footprint` > 20 MB or row count > 50,000, execute purge:
     `DELETE FROM agent_events WHERE timestamp < :cutoffTime OR rowid IN (SELECT rowid FROM agent_events ORDER BY timestamp ASC LIMIT 5000)`
  3. Execute `PRAGMA wal_checkpoint(TRUNCATE)` to reset and shrink WAL file size.
  4. Perform asynchronous `VACUUM` during idle state to reclaim unused disk pages.
