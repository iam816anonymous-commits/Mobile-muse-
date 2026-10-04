# STORAGE_PERSISTENCE_AUDIT_REPORT.md — Phase 2 Storage Audit & Verification Report

## 1. Executive Summary

This report delivers the comprehensive audit and verification of the **LocalAgent Phase 2 Persistence Architecture**, covering:
- **Part A:** Phase 2 Operational Persistence (`agent.db`) and UI Event Stream Hydration.
- **Part B:** Durable Long-Term Memory Storage (`DurableMemoryStorageManager`) in Internal Shared Storage (`/sdcard/LocalAgent/memory/`) for uninstall/reinstall survival.

---

## 2. Answers to Audit Questions

### 1. Current Storage Architecture
The architecture strictly partitions storage into two distinct layers:
1. **App-Private Operational Storage (`agent.db`):** Room database at `/data/data/com.localagent.app/files/agent/agent.db`. Capped at 30 MB / 50,000 events. Used for event logging, session tracking, and active goal queues.
2. **Durable Long-Term Memory Storage (`DurableRecord`):** File-based store at `/sdcard/LocalAgent/memory/` (Internal Shared Storage). Uses SHA-256 integrity checksums and JSON serialization.

### 2. Exact Location/Type of Phase 2 Event DB
- **Type:** SQLite via Android Room 2.6.1 with WAL (Write-Ahead Logging).
- **Physical Path:** `/data/data/com.localagent.app/files/agent/agent.db` (App-Private).

### 3. Why UI Event History Disappeared After Reopening
Prior to the fix, `MainActivity` initialized an empty in-memory `LinkedList<String>` upon `onCreate()` and appended only new live events without querying past events from `agent.db`. While `agent.db` persisted the events across process restarts, the UI stream was not hydrating past logs from storage.

### 4. Fix Applied for Event-History Restoration
`MainActivity.onCreate()` now invokes `hydratePersistedEventHistory()`, which queries `EventRepository.queryEvents(EventFilter(limit = maxEventLogSize - 1))` from `agent.db` and populates the visible `tvRecentEvents` stream before new live events are logged.

### 5. Exact Long-Term Memory Storage Mechanism
`DurableMemoryStorageManager` writes JSON files with SHA-256 payload checksums to user-owned Internal Shared Storage (`/sdcard/LocalAgent/memory/` or user-granted Storage Access Framework URIs).

### 6. Why Long-Term Memory Survives Uninstall/Reinstall
App-private storage (`/data/data/com.localagent.app/...`) and app-specific external storage (`/Android/data/com.localagent.app/...`) are deleted by Android OS during app uninstallation. Internal Shared Storage (`/sdcard/LocalAgent/memory/`) is user-owned and NOT deleted during app uninstallation. Upon reinstall, `DurableMemoryStorageManager.initializeStorage()` reconnects to the directory and discovers all existing records.

### 7. Is a Removable SD Card Required?
**NO.** Long-term memory targets the phone's **internal shared/emulated storage** (`/sdcard/` or `/storage/emulated/0/LocalAgent/memory/`), which is available on physical Android devices without a removable SD card.

### 8. Android API 27 Baseline Compatibility Assessment
- On API 27 (Android 8.1), `/sdcard/LocalAgent/memory/` is directly accessible with standard storage permissions.
- On API 29+ (Scoped Storage), `DurableMemoryStorageManager` supports Storage Access Framework (SAF) document tree URIs (`Intent.ACTION_OPEN_DOCUMENT_TREE`).
- If external durable storage is denied, the system gracefully falls back to `AVAILABLE_APP_PRIVATE_ONLY` and displays the status on the diagnostic UI.

### 9. New Tests Added
1. `MainActivityEventHydrationTest`: Verifies that historical events in `agent.db` are hydrated into `tvRecentEvents` on activity launch.
2. `DurableMemoryStorageManagerTest`: Verifies read/write, SHA-256 integrity verification, tampering detection, category filtering, schema migration, and survival across app-private directory deletion.

### 10. Manual Uninstall / Reinstall Verification Procedure
See Section 3 below.

### 11. Security / Privacy Implications
All metadata saved to `agent.db` or durable records is passed through regex sanitization in `RoomEventRepository` to redact passwords, authentication tokens, PINs, and secret keys.

### 12. Limitations
Scoped storage on API 30+ requires user consent via SAF picker if public external storage permissions are restricted by OEM policies.

### 13. Phase 2 Freeze Status
**Phase 2 Operational Event Persistence = PASS**
`agent.db` persists events across restarts and `MainActivity` hydrates past event history.

### 14. Long-Term Memory Status
**Durable Agent Memory = PASS**
Implemented via `DurableMemoryStorageManager` in user-owned internal shared storage and verified to survive app-private directory deletion.

---

## 3. Manual Uninstall / Reinstall Verification Instructions

To manually test on a physical Android phone:

1. **Step 1: Install & Write Test Memory Record**
   Launch LocalAgent. Execute command: `click 7`.
   Verify UI displays:
   - Event Storage: `Location: APP_PRIVATE`
   - Long-Term Memory: `Location: INTERNAL_SHARED_STORAGE (/sdcard/LocalAgent/memory/)`
   - Memory record `MEMORY_TEST_001.json` is created in `/sdcard/LocalAgent/memory/`.

2. **Step 2: Force Stop & Launch Again**
   Force stop LocalAgent from Android Settings. Reopen LocalAgent.
   Verify:
   - Recent Pipeline Event Log hydrates past `UI_CLICK` events from `agent.db`.
   - `MEMORY_TEST_001.json` remains present in durable storage.

3. **Step 3: Uninstall LocalAgent**
   Uninstall LocalAgent from phone.
   *Do NOT delete the `/sdcard/LocalAgent/` folder from internal storage.*

4. **Step 4: Reinstall LocalAgent**
   Reinstall and launch LocalAgent.
   Verify:
   - `DurableMemoryStorageManager` discovers `/sdcard/LocalAgent/memory/`.
   - UI reports `Durable Records: 1`.
   - `MEMORY_TEST_001` is discovered and restored.

5. **Step 5: File Manager Inspection**
   Open the phone's native File Manager app. Navigate to `Internal storage -> LocalAgent -> memory`.
   Verify `MEMORY_TEST_001.json` is visible to the user and not hidden inside `/data/data/...`.
