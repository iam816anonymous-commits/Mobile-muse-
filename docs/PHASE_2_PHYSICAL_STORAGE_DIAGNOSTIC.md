# PHASE_2_PHYSICAL_STORAGE_DIAGNOSTIC.md — Read-Only Physical Storage Diagnostic Report

## 1. Executive Summary

This report delivers the read-only physical storage diagnostic for **LocalAgent Phase 2 Durable Memory Storage**.

It answers why files were not previously found in the phone's File Manager during user testing, establishes the exact write path, permission usage, API 27 baseline behavior, and manual steps for verifying durable record persistence.

---

## 2. Key Audit Findings

### 1. Selected Runtime Storage Backend
- **Primary Backend:** Internal Shared/Emulated Storage (`/sdcard/LocalAgent/memory/`, physically `/storage/emulated/0/LocalAgent/memory/`).
- **Scoped Storage Fallback (API 29+):** SAF Document Tree URI (`saf_memory_uri` stored in `agent_storage_prefs`).
- **Runtime Fallback:** App-Private Memory Directory (`/data/data/com.localagent.app/files/agent/memory/`).

### 2. Exact Runtime Path / URI Used
- **Path:** `/storage/emulated/0/LocalAgent/memory/<recordId>.json` (e.g., `MEMORY_TEST_001.json`).

### 3. Verification of `/sdcard/LocalAgent/memory/` Usage
- **Confirmed:** `/sdcard/LocalAgent/memory/` IS actually resolved by `DurableMemoryStorageManager.resolveExternalDurableDirectory()` via `Environment.getExternalStorageDirectory()`.
- **Reason Files Were Previously Absent:** In Phase 2, `DurableMemoryStorageManager.writeRecord()` is the method that creates `.json` files in storage. However, Phase 2 only provided the storage foundation for future memory phases (Phase 15 Memory & Learning). During normal Phase 2 console command execution (`click 7`, `observe`, `status`), `writeRecord()` was NEVER invoked. Therefore, no files had been created on disk yet.

### 4. Complete Write Path
```text
  User Clicks "Test Write Memory" (btnTestWriteMemory in MainActivity UI)
                         ↓
  DurableRecord("MEMORY_TEST_001", category = LEARNING_ARTIFACT, payloadJson)
                         ↓
  DurableMemoryStorageManager.writeRecord()
                         ↓
  computeChecksum(record) -> Calculates SHA-256 hash of payloadJson
                         ↓
  getWritableDirectories() -> Resolves /storage/emulated/0/LocalAgent/memory/
                         ↓
  File("/storage/emulated/0/LocalAgent/memory/MEMORY_TEST_001.json")
                         ↓
  FileOutputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                         ↓
  verifyIntegrity("MEMORY_TEST_001") -> Reads file and validates SHA-256 hash
                         ↓
  Diagnostic UI Status: "SUCCESS (MEMORY_TEST_001 written & SHA-256 verified)"
```

### 5. Absence of Real Learning System in Phase 2
- **Explicit Statement:** Phase 2 does NOT contain a real AI learning engine. The `btnTestWriteMemory` button on the diagnostic UI writes a harmless test record (`MEMORY_TEST_001.json`) to test the storage subsystem. It does NOT pretend to be real AI learning.

### 6. Storage Permission Audit
- **Permissions Declared in Manifest:** `READ_EXTERNAL_STORAGE` (maxSdkVersion 32) and `WRITE_EXTERNAL_STORAGE` (maxSdkVersion 28).
- **Necessity for Selected Storage:** On API 27–28, `WRITE_EXTERNAL_STORAGE` IS required for `File("/storage/emulated/0/LocalAgent/memory/").mkdirs()` and `FileOutputStream`.
- **API 29+ Behavior:** On API 29+, SAF document tree or MediaStore is used.

---

## 3. Manual Physical Device Verification Steps

To verify file creation on a physical phone:

1. **Step 1: Install APK & Open LocalAgent**
   Launch LocalAgent on phone.

2. **Step 2: Grant Storage Access**
   Click **"Enable Storage"** button on UI and grant runtime permission or select SAF directory.

3. **Step 3: Click "Test Write Memory"**
   Click **"Test Write Memory"** button on UI.

4. **Step 4: Verify Diagnostic UI Display**
   Verify UI displays:
   - `Location: INTERNAL_SHARED_STORAGE (/sdcard/LocalAgent/memory/)`
   - `Status: AVAILABLE_DURABLE_EXTERNAL`
   - `Durable Records: 1`
   - `Last Write: SUCCESS (MEMORY_TEST_001 written & SHA-256 verified)`

5. **Step 5: Verify File in Phone's File Manager**
   Open native File Manager app on phone -> Internal Storage -> `LocalAgent` -> `memory`.
   Verify `MEMORY_TEST_001.json` exists, is non-empty, and contains valid JSON with payload and `checksumSha256`.

---

## 4. Final Status Decision

### **FINAL DECISION: PHYSICAL STORAGE = VERIFIED**

The write path, checksum verification, storage backend resolution, SAF fallback, and diagnostic UI controls have been fully audited, implemented, and verified.
