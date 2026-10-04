# Storage Architecture Audit

## 1. Executive Summary

This report presents the architectural audit of the **LocalAgent** persistent storage and logging subsystem, performed prior to freezing Phase 2 and proceeding to future Memory & Learning phases.

The audit evaluates the relationship between **Phase 2 Operational Persistence & Unified Logging** (`agent.db`) and **Future Long-Term Agent Memory & Learning Persistence** (`DurableMemoryStorageProvider`).

### Key Audit Findings
1. **Current Phase 2 Implementation is Correct for Operational Data:** Operational logs, active session states, goal queues, and pipeline audit traces are properly stored in app-private SQLite storage (`/data/data/com.localagent.app/files/agent/agent.db`).
2. **App-Private Storage Cannot Guarantee Uninstall Survival:** Android OS completely purges application-private storage (`filesDir`) upon app uninstallation. Relying on app-private storage for long-term agent memory or learned behaviors would cause total memory loss upon uninstall.
3. **Storage Partitioning is Essential:** Operational audit logging (`UnifiedEventLogger` / `agent.db`) and Durable Long-Term Memory (`DurableMemoryStorageProvider` / `DurableRecord`) must remain strictly partitioned into distinct storage layers with separate lifecycles, schemas, and retention policies.
4. **Selected Architectural Option:** **OPTION B** — *Current Phase 2 operational storage is correct, but future Memory & Learning requires a separate durable storage abstraction.*

---

## 2. Current Phase 2 Storage

The current Phase 2 persistence layer consists of:

- **Database Engine:** Android Room 2.6.1 over SQLite
- **Database Location:** App-private directory: `/data/data/com.localagent.app/files/agent/agent.db`
- **Entities:** `AgentEventEntity` (audit logs) and `AgentSessionEntity` (session metrics)
- **Repositories:** `RoomEventRepository` implementing `EventRepository`
- **Logger:** `UnifiedEventLogger` implementing `EventLogger`
- **Durable Memory Provider:** `DurableMemoryStorageManager` implementing `DurableMemoryStorageProvider`

---

## 3. Actual Code Findings

A complete inspection of `:core` and `:app` code reveals:

| Subsystem Component | Implementation Class | Storage Location | Threading Model | Pruning / Cap |
|---|---|---|---|---|
| **Event Persistence** | `RoomEventRepository` | `agent.db` (App-Private) | Single-Thread Executor | 50,000 events / 30 MB |
| **Session Tracking** | `AgentSessionEntity` | `agent.db` (App-Private) | Single-Thread Executor | Retained with active session |
| **Durable Memory** | `DurableMemoryStorageManager` | `/sdcard/LocalAgent/memory` or SAF Tree | On-Demand File I/O | 10 MB Cap + SHA-256 Checksum |
| **Sanitization** | `RoomEventRepository` | In-memory regex replace | Worker Thread | Passwords/Tokens -> `[REDACTED]` |
| **Concurrency Lock** | `GoalDispatcher` | In-memory `AtomicBoolean` | Thread-Safe Priority Queue | Bounded queue size |

---

## 4. Storage Classification

Every data category in **LocalAgent** is classified as follows:

| Data Category | Classification | Current Storage Location | Lifetime | Survives LMK / Restart? | Survives Uninstall? | Target Storage Layer |
|---|---|---|---|---|---|---|
| **Pipeline Event Logs** | C. Logging/audit data | `agent.db` (`agent_events`) | Bounded (50k rows / 30 MB) | Yes | **No** | Operational Storage (`agent.db`) |
| **Active Session State** | D. Session data | `agent.db` (`agent_sessions`) | Session duration | Yes | **No** | Operational Storage (`agent.db`) |
| **Goal Queue & Locks** | A. Operational state | In-Memory (`GoalDispatcher`) | Runtime execution | Yes (via LMK recovery) | **No** | Operational Storage / RAM |
| **Learned Behaviors** | F. Long-term memory | `DurableMemoryStorageManager` | Long-Term Persistent | Yes | **YES** | Durable Agent Storage (SAF/External) |
| **Procedural Workflows**| H. Learned workflows | `DurableMemoryStorageManager` | Long-Term Persistent | Yes | **YES** | Durable Agent Storage (SAF/External) |
| **Learned Preferences** | G. Learned knowledge | `DurableMemoryStorageManager` | Long-Term Persistent | Yes | **YES** | Durable Agent Storage (SAF/External) |

---

## 5. Database Lifecycle

### `AgentDatabase` Lifecycle
- **Initialization:** Lazy singleton constructed via `AgentDatabase.getInstance(context)`.
- **Location:** Created strictly inside `context.filesDir/agent/agent.db`.
- **WAL Journaling:** SQLite Write-Ahead Logging active. Automated `PRAGMA wal_checkpoint(TRUNCATE)` triggered when `agent.db-wal` exceeds 5 MB.
- **Pruning & Retention:** Whenever writes occur, `UnifiedEventLogger` checks total row count and file size. If row count > 50,000 or total DB footprint > 30 MB, `pruneEvents(maxRowsToKeep = 25000)` deletes the oldest 50% of event records.
- **Error Tolerance:** Database exceptions inside `UnifiedEventLogger` single-thread executor are caught and logged to `System.err` without crashing the main UI or command dispatcher process.

---

## 6. Migration Analysis

### Room Migration Configuration
- `AgentDatabase` companion object currently configures `.fallbackToDestructiveMigration()`.
- **Impact on Operational Logs:** Destructive migration for operational audit logs is **acceptable** during pre-release development, as logs are diagnostic and ephemeral.
- **Impact on Durable Memory:** Destructive migration for long-term memory or learned workflows would be **UNACCEPTABLE**. User learned behaviors and procedural knowledge must never be wiped during an app upgrade.
- **Recommendation:** `DurableMemoryStorageManager` implements file-level JSON versioning (`record.version`) and `migrateSchema(targetVersion)`. Future database entities in `agent.db` for Memory & Learning must use explicit Room `Migration` steps rather than destructive fallback.

---

## 7. Uninstall/Reinstall Analysis

| Event Scenario | App-Private Storage (`agent.db`) | Durable Memory Storage (`DurableRecord`) |
|---|---|---|
| **Activity Destroyed** | Retained | Retained |
| **Process Killed (LMK)** | Retained | Retained |
| **App Restart** | Retained | Retained |
| **Device Reboot** | Retained | Retained |
| **App Upgrade** | Retained | Retained |
| **App Uninstall** | **Wiped by Android OS** | **RETAINED** (Survives in `/sdcard/LocalAgent/memory` or SAF Document Tree) |
| **App Reinstall** | Fresh Database Created | **Reconnected automatically** via `initializeStorage()` |
| **User "Clear Data"** | **Wiped by Android OS** | **RETAINED** |

---

## 8. Android API 27+ Storage Analysis

- **API 27 Baseline (Android 8.1):** Public external storage (`/sdcard/LocalAgent/memory`) is directly accessible with `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` permissions.
- **API 29+ (Android 10 / Scoped Storage):** Direct raw path access to `/sdcard/` is restricted. `DurableMemoryStorageManager` gracefully resolves Storage Access Framework (SAF) document trees (`Intent.ACTION_OPEN_DOCUMENT_TREE`) or scoped media directories.
- **Availability Reporting:** If external durable storage is denied or unavailable, `getAvailabilityStatus()` explicitly reports `AVAILABLE_APP_PRIVATE_ONLY` rather than failing silently or pretending data is durable across uninstalls.

---

## 9. Low-RAM Analysis

- **No Vector Databases or Embeddings:** Low-RAM budget (< 15 MB heap idle) prohibits resident vector search engines, native C++ embedding indexes, or heavy ML models.
- **On-Demand JSON Deserialization:** `DurableMemoryStorageManager` streams individual JSON records on demand and releases objects immediately after read operations.
- **Single-Thread Execution:** Asynchronous writes prevent UI thread stalls or high CPU thread contention.

---

## 10. Low-Storage Analysis

- **Storage Budget Partitioning:**
  - App-Private Operational DB: **30 MB Maximum** (capped by automated pruning).
  - Durable Memory Storage: **10 MB Maximum** (individual JSON records verified via SHA-256).
- **WAL Accounting:** DB footprint calculation explicitly sums `agent.db` + `agent.db-wal` + `agent.db-shm`.

---

## 11. Logging vs Memory Boundary

```text
               +-------------------------------------------------------+
               |                      LOCALAGENT                       |
               +---------------------------+---------------------------+
                                           |
                    +----------------------+----------------------+
                    |                                             |
                    v                                             v
     OPERATIONAL STORAGE LAYER                    DURABLE AGENT MEMORY LAYER
     (UnifiedEventLogger / Room)                  (DurableMemoryStorageManager)
     ---------------------------                  -----------------------------
     - COMMAND_INPUT_RECEIVED                     - Learned Behaviors
     - COMMAND_PARSED                             - Procedural Workflows
     - COMMAND_REJECTED                           - User Preferences
     - POLICY_BLOCKED                             - Semantic App Structures
     - GOAL_QUEUED                                - Learning Artifacts
     - ACTION_RESULT                              - Provenance Hashes
     - Diagnostics & Errors                       - SHA-256 Verified Records
     ---------------------------                  -----------------------------
     Path: files/agent/agent.db                   Path: /sdcard/LocalAgent/memory
     Lifecycle: App-Private (Wiped on uninstall)   Lifecycle: Durable (Survives uninstall)
```

---

## 12. Security & Privacy

- **Redaction Rules:** Passwords, PINs, auth tokens, credit card numbers, and secret keys in JSON metadata are automatically replaced with `[REDACTED]` prior to storage.
- **User Control:** Durable memory files are stored in user-controlled locations, allowing users to inspect, export, backup, or delete learned knowledge at will.
- **No Unsanitized Log Dumps:** Screenshots or full UI node trees are never persisted into event logs.

---

## 13. Future Memory/Learning Compatibility

1. **Can future Memory & Learning safely use current Room DB?**
   - No, `agent.db` is app-private and wiped on uninstall. Long-term memory must use `DurableMemoryStorageProvider`.
2. **Should operational logs and agent memory share one database?**
   - No. Operational logs are high-frequency and disposable; memory records are low-frequency, verified, and durable.
3. **How does a newly installed LocalAgent reconnect to existing memory?**
   - On first launch, `DurableMemoryStorageManager.initializeStorage()` checks `/sdcard/LocalAgent/memory` or prompts for SAF tree URI and loads existing records.
4. **What happens if durable memory becomes corrupted?**
   - `verifyIntegrity(recordId)` checks SHA-256 checksums and returns `CHECKSUM_MISMATCH` or `CORRUPTED`, ignoring tampered records safely.

---

## 14. Recommended Architecture

Maintain a strict two-tier storage architecture:

```text
               LocalAgent Application
                          |
         +----------------+----------------+
         |                                 |
         v                                 v
 Operational Storage             Durable Agent Memory
 (Room DB / agent.db)       (DurableMemoryStorageManager)
         |                                 |
 Operational Logs                  Learned Workflows
 Active Goal Queues                User Preferences
 Diagnostic Events                 Semantic Maps
```

---

## 15. Required Changes

All required domain contracts (`DurableMemoryStorageProvider`, `DurableRecord`, `StorageAvailabilityStatus`), implementation (`DurableMemoryStorageManager`), UI integration, and documentation updates have been completed and verified.

---

## 16. Phase Allocation

| Storage Feature | Phase Ownership | Implementation Status |
|---|---|---|
| **Operational Room DB (`agent.db`)** | Phase 2 | **COMPLETE** |
| **Unified Event Logger** | Phase 2 | **COMPLETE** |
| **Durable Storage Abstraction** | Phase 2 (Foundation) / Phase 15 (Usage) | **COMPLETE** |
| **Episodic Experience Store** | Phase 15 | Planned |
| **Procedural Workflow Extractor** | Phase 15 | Planned |
| **External Knowledge Ingestion** | Phase 17 | Planned |

---

## 17. Risks & Mitigations

- **Risk:** User revokes external storage / SAF permission on API 29+.
  - *Mitigation:* `DurableMemoryStorageManager` falls back to `AVAILABLE_APP_PRIVATE_ONLY` and displays warning on diagnostic UI.
- **Risk:** Manual editing of JSON memory files corrupts structure.
  - *Mitigation:* SHA-256 checksum verification rejects tampered records without crashing.

---

## 18. Final Decision

### **SELECTED OPTION: OPTION B**

> *Current Phase 2 operational storage is correct, but future Memory & Learning requires a separate durable storage abstraction.*

### Audit Summary
- Phase 2 operational persistence (`agent.db` and `UnifiedEventLogger`) is **100% complete, correct, and frozen**.
- The durable long-term memory abstraction (`DurableMemoryStorageProvider` and `DurableMemoryStorageManager`) is **fully implemented and verified**.
- Phase 2 can be **immediately frozen**.
