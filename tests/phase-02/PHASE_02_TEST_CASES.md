# PHASE_02_TEST_CASES.md — Phase 2 Persistent Storage & Unified Logging Test Cases

## 1. Overview

This document specifies the test cases for Phase 2 (Persistent Storage & Unified Logging) of LocalAgent.

---

## 2. Test Cases

### P2-LOG-001
- **Requirement:** Operational Event Persistence in Room Database (`agent.db`)
- **Purpose:** Proves `RoomEventRepository` persists structured events (`AgentEvent`) to `agent.db` and queries them correctly by session and timestamp.
- **Preconditions:** In-memory or file-backed Room `AgentDatabase` initialized.
- **Input:** Insert `AgentEvent` with session ID, subsystem, event type, and metadata JSON.
- **Expected Result:** `queryEvents()` returns persisted event with matching fields.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `app/src/test/java/com/localagent/app/storage/RoomEventRepositoryTest.kt` -> `testInsertAndQueryEvent()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.storage.RoomEventRepositoryTest"`
- **Permissions:** NONE (Simulated in-memory / cache DB)
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P2-CAP-001
- **Requirement:** Automated Storage Footprint Pruning & Retention Cap
- **Purpose:** Proves database pruning removes oldest events when row count exceeds 50,000 or file footprint exceeds 30 MB.
- **Preconditions:** Insert 20 events with timestamp ordering.
- **Input:** Execute `pruneEvents(maxRowsToKeep = 10)`.
- **Expected Result:** 10 oldest events deleted; event count reduced to 10.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `app/src/test/java/com/localagent/app/storage/RoomEventRepositoryTest.kt` -> `testPruningPolicy()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.storage.RoomEventRepositoryTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P2-SEC-001
- **Requirement:** Sensitive Data Sanitization Prior to Persistence
- **Purpose:** Proves passwords, tokens, PINs, and secret keys in metadata JSON are redacted before SQL insertion.
- **Preconditions:** Event created with metadata `{"password":"secret123","pin":"4321"}`.
- **Input:** Persist event via `RoomEventRepository.insertEvent()`.
- **Expected Result:** Metadata queried from database contains `[REDACTED]` and no plaintext secret.
- **Test Type:** AUTOMATED_UNIT
- **Executable Test Location:** `app/src/test/java/com/localagent/app/storage/RoomEventRepositoryTest.kt` -> `testSanitizationOfSensitiveMetadata()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.storage.RoomEventRepositoryTest"`
- **Permissions:** NONE
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P2-UI-001
- **Requirement:** Diagnostic UI Event Stream Hydration from Database
- **Purpose:** Proves Diagnostic UI (`EventLogActivity`) queries past event history from `agent.db` upon launch and populates `tvEventLogStream`.
- **Preconditions:** Insert historical event `HISTORICAL_EVENT_001` into `agent.db`.
- **Input:** Launch `EventLogActivity` via Robolectric.
- **Expected Result:** `tvEventLogStream.text` displays `HISTORICAL_EVENT_001`.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/ui/MainActivityEventHydrationTest.kt` -> `testEventHistoryHydratedFromDatabaseOnLaunch()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.ui.MainActivityEventHydrationTest"`
- **Permissions:** Simulated UI environment
- **Hardware:** NONE
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P2-MEM-001
- **Requirement:** Durable Long-Term Memory Record Persistence & SHA-256 Checksum Verification
- **Purpose:** Proves `DurableMemoryStorageManager` writes JSON memory records with SHA-256 checksums, detects tampering, and list-filters records by category.
- **Preconditions:** External durable memory directory available.
- **Input:** Write `DurableRecord`, tamper with file payload directly on disk, verify integrity.
- **Expected Result:** Read verifies checksum; tampered file returns `CHECKSUM_MISMATCH`.
- **Test Type:** AUTOMATED_ROBOLECTRIC
- **Executable Test Location:** `app/src/test/java/com/localagent/app/storage/DurableMemoryStorageManagerTest.kt` -> `testIntegrityCheckDetectsTampering()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.storage.DurableMemoryStorageManagerTest"`
- **Permissions:** Simulated storage checks
- **Hardware:** Simulated file system
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P2-MEM-002
- **Requirement:** Durable Memory Survival Across App-Private Directory Deletion (Simulation)
- **Purpose:** Proves long-term memory stored in internal shared storage (`/sdcard/LocalAgent/memory/`) survives even if app-private directory (`/data/data/com.localagent.app/files/`) is wiped.
- **Note:** *This is an automated Robolectric simulation test that verifies logic across app-private directory deletion; it does NOT prove actual physical Android OS uninstall/reinstall survival.*
- **Preconditions:** `DurableMemoryStorageManager` writes record to custom external directory.
- **Input:** Delete `context.filesDir/agent/memory` completely, then call `readRecord()`.
- **Expected Result:** Record is discovered and restored from durable external storage.
- **Test Type:** AUTOMATED_ROBOLECTRIC_SIMULATION
- **Executable Test Location:** `app/src/test/java/com/localagent/app/storage/DurableMemoryStorageManagerTest.kt` -> `testDurableMemorySurvivesAppPrivateDirectoryDeletion()`
- **Execution Command:** `./gradlew :app:testDebugUnitTest --tests "com.localagent.app.storage.DurableMemoryStorageManagerTest"`
- **Permissions:** Simulated storage checks
- **Hardware:** Simulated file system
- **Evidence:** JUnit XML report (`app/build/test-results/testDebugUnitTest/`)
- **Status:** PASS

### P2-DEV-001
- **Requirement:** Physical Phone Manual Uninstall/Reinstall Survival Procedure
- **Purpose:** Verifies that a memory record `MEMORY_TEST_001` written to `/sdcard/LocalAgent/memory/` on a physical phone survives force-stop, process kill, and complete app uninstall/reinstall.
- **Preconditions:** Physical phone running Android 8.1 API 27 or newer with LocalAgent installed.
- **Input:** Write record on phone, force stop app, uninstall app, reinstall APK, launch app.
- **Expected Result:** Reinstalled app discovers `MEMORY_TEST_001.json` and reports `Durable Records: 1` on UI.
- **Test Type:** PHYSICAL_DEVICE
- **Executable Test Location:** Manual Procedure (`PHYSICAL_DEVICE_TEST_PLAN.md` Section 2)
- **Execution Command:** Manual phone installation & File Manager inspection
- **Permissions:** `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, SAF Document Tree (Real physical device grants required)
- **Hardware:** Physical Android Smartphone
- **Evidence:** Pending physical phone test execution
- **Status:** NOT_RUN
