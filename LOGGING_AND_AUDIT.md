# LOGGING_AND_AUDIT.md — Universal Structured Event Logging & Audit Subsystem

## 1. Executive Summary & Core Architectural Principles

Logging in **LocalAgent** is a first-class, core subsystem rather than a transient UI display or localized debug tool. Every command, target resolution, accessibility dispatch, permission change, voice STT/TTS event, hardware control, memory update, workflow step, error, and system diagnostic generates a **structured, persistent event**.

### Universal Logging Directives
1. **Universal Execution Trail:** All input channels (Console, Movable Overlay, Voice Input, Workflow Engine, AI Planners) write structured logs to the single `EventLogger`.
2. **Persistent Storage:** Event logs are saved to an app-private SQLite/Room database (`/data/data/com.localagent.app/files/agent/logs/agent-events.db`).
3. **Structured Schema:** Events contain correlation IDs, precise timestamps, subsystem source tags, execution duration, result codes, and structured JSON metadata.
4. **Bounded Storage & Rotation:** Log database size is strictly capped (default 20 MB max). Automated log rotation purges or compresses expired records to ensure zero risk of filling low-storage devices.

---

## 2. Structured Event Schema (`AgentEvent`)

```kotlin
@Entity(tableName = "agent_events")
data class AgentEvent(
    @PrimaryKey
    val eventId: String = UUID.randomUUID().toString(),
    val correlationId: String, // Groups multi-step actions/workflows into one execution sequence
    val timestamp: Long = System.currentTimeMillis(),
    val subsystem: EventSubsystem, // COMMAND, ACCESSIBILITY, ACTION, PERMISSION, HARDWARE, VOICE, WORKFLOW, MEMORY, SYSTEM, TEST
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

enum class EventSeverity {
    DEBUG, INFO, WARNING, ERROR, CRITICAL
}

enum class EventSubsystem {
    COMMAND, ACCESSIBILITY, OBSERVATION, ACTION, PERMISSION, HARDWARE, VOICE, WORKFLOW, MEMORY, LEARNING, BROWSER, SYSTEM, TEST
}
```

---

## 3. Persistent Storage Directory Structure & Retention Policy

```text
/data/data/com.localagent.app/files/agent/
├── logs/
│   ├── agent-events.db          # Primary SQLite / Room Event Database
│   ├── agent-events.db-shm      # Shared memory WAL file
│   ├── agent-events.db-wal      # Write-Ahead Log
│   └── archives/
│       ├── agent-log-2026-10-03.gz  # Compressed historical log exports
│       └── agent-log-2026-10-04.gz
├── memory/
│   ├── workflows.db             # Procedural workflow definitions
│   └── knowledge.db             # Semantic app knowledge
├── evidence/                    # Diagnostic failure snapshots
│   ├── failure-snap-001.json    # Dumped UI tree primitive on failure
│   └── failure-snap-001.png     # Optional diagnostic screenshot
└── exports/                     # User-initiated log/audit exports
```

### Log Retention & Rotation Protocol
- **Max Database Size:** 20 MB (Configurable between 5 MB and 50 MB in Settings).
- **Max History Days:** 7 Days default retention.
- **Trigger Strategy:**
  1. Whenever a new log batch is written, `LogRetentionManager` checks `database.file.length()`.
  2. If DB size > 20 MB or row count > 100,000, execute purge query:
     `DELETE FROM agent_events WHERE timestamp < :cutoffTime OR rowid IN (SELECT rowid FROM agent_events ORDER BY timestamp ASC LIMIT 5000)`
  3. Execute SQLite `VACUUM` asynchronously during idle agent states to reclaim disk space.

---

## 4. Universal Event Trace Example

Example event sequence logged when user issues "click 7" in Calculator:

```json
[
  {
    "eventId": "evt-001",
    "correlationId": "corr-calc-7782",
    "timestamp": 1759546000100,
    "subsystem": "COMMAND",
    "eventType": "COMMAND_RECEIVED",
    "actionType": "UI_CLICK",
    "sourceChannel": "CONSOLE",
    "resultCode": "NORMALIZED",
    "durationMs": 2,
    "severity": "INFO",
    "metadataJson": "{\"rawInput\":\"click 7\",\"targetQuery\":\"7\"}"
  },
  {
    "eventId": "evt-002",
    "correlationId": "corr-calc-7782",
    "timestamp": 1759546000115,
    "subsystem": "OBSERVATION",
    "eventType": "TARGET_RESOLVING",
    "actionType": "UI_CLICK",
    "sourceChannel": "CONSOLE",
    "targetPackage": "com.google.android.calculator",
    "targetViewId": "com.google.android.calculator:id/digit_7",
    "resultCode": "ANCESTOR_TRAVERSED",
    "durationMs": 12,
    "severity": "INFO",
    "metadataJson": "{\"targetNode\":\"TextView('7')\",\"clickableAncestor\":\"MaterialButton('7')\"}"
  },
  {
    "eventId": "evt-003",
    "correlationId": "corr-calc-7782",
    "timestamp": 1759546000130,
    "subsystem": "ACTION",
    "eventType": "ACTION_DISPATCHED",
    "actionType": "UI_CLICK",
    "sourceChannel": "CONSOLE",
    "targetPackage": "com.google.android.calculator",
    "resultCode": "DISPATCHED",
    "durationMs": 5,
    "severity": "INFO",
    "metadataJson": "{\"actionId\":16,\"targetClass\":\"android.widget.Button\"}"
  },
  {
    "eventId": "evt-004",
    "correlationId": "corr-calc-7782",
    "timestamp": 1759546000435,
    "subsystem": "OBSERVATION",
    "eventType": "STATE_DIFF_VERIFIED",
    "actionType": "UI_CLICK",
    "sourceChannel": "CONSOLE",
    "targetPackage": "com.google.android.calculator",
    "resultCode": "SUCCESS_VERIFIED",
    "durationMs": 305,
    "severity": "INFO",
    "metadataJson": "{\"textDiff\":[{\"viewId\":\"com.google.android.calculator:id/formula\",\"oldText\":\"\",\"newText\":\"7\"}]}"
  }
]
```
