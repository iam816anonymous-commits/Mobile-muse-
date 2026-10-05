# PHYSICAL_DEVICE_TEST_PLAN.md — Physical Phone Manual Verification Procedures

## 1. Executive Summary

This document specifies the exact physical-device manual test procedures for every capability across the **LocalAgent** phase roadmap.

A capability is NOT verified merely because unit tests pass. Each procedure defines the setup, expected grant/deny UI states, execution results, and uninstall/reinstall recovery behavior on real Android hardware.

---

## 2. Phase 2 — Durable Long-Term Memory Storage Test Procedure

### Test 2.1: Write Memory & Process Restart
1. **Target Capability:** `DURABLE_STORAGE`
2. **Setup:** Install LocalAgent APK on physical phone (Android 8.1 API 27 or newer).
3. **Execution:** Open LocalAgent. Click "Test Write Memory".
4. **Expected UI Display:**
   - EVENT STORAGE: `Location: APP_PRIVATE (/data/data/.../files/agent/)`
   - LONG-TERM MEMORY: `Location: INTERNAL_SHARED_STORAGE (/sdcard/LocalAgent/memory/)`, `Status: AVAILABLE_DURABLE_EXTERNAL`, `Durable Records: 1`, `Last Write: SUCCESS`
5. **Process Kill:** Force stop LocalAgent from Android Settings -> Apps -> LocalAgent -> Force Stop. Reopen LocalAgent.
6. **Verification:** Confirm diagnostic UI hydrates recent events from `agent.db` AND discovers `Durable Records: 1`.

### Test 2.2: App Uninstall & Reinstall Memory Survival
1. **Execution:** Uninstall LocalAgent from phone. (*Do NOT delete the `/sdcard/LocalAgent/` folder*).
2. **Reinstall:** Install LocalAgent APK again. Launch app.
3. **Verification:** Confirm diagnostic UI reports `Location: INTERNAL_SHARED_STORAGE` and `Durable Records: 1`. Long-term memory records survived uninstall.
4. **File Manager Inspection:** Open phone's File Manager -> Internal Storage -> `LocalAgent/memory/`. Confirm `MEMORY_TEST_001.json` is visible.

---

## 3. Phase 3.1 – 3.3 — Accessibility Observation, Evidence & Permission Test Procedures

### Test 3.1: Service Unbound State
1. **Setup:** Install LocalAgent APK on physical phone. Do NOT enable Accessibility Service yet.
2. **Launch:** Open LocalAgent.
3. **Expected UI Display:**
   - ACCESSIBILITY OBSERVATION SERVICE: `Status: SERVICE_UNBOUND | Active Pkg: None`
4. **Command Trigger:** Type `observe` in command input or click "Current UI Observation".
5. **Expected Result:** Output displays `Command: OBSERVE | Status: ACCESSIBILITY_UNAVAILABLE | Reason: Accessibility Service unbound (Open Settings to enable)`. App does NOT crash.

### Test 3.2: Service Enablement & Live UI Observation
1. **Enablement:** Tap "Permission Center" on `MainActivity`, then click "Open Accessibility Settings".
2. **Android Settings:** In Android Accessibility Settings, find "LocalAgent Read-Only UI Observation Service" and enable toggle.
3. **Return:** Return to LocalAgent.
4. **Expected UI Display:** `Status: READY (BOUND)`.
5. **Observation Execution:** Click "Current UI Observation" button.
6. **Expected Result:**
   - Output displays `Command: OBSERVE | Status: SUCCESS_VERIFIED | Pkg: com.localagent.app | Nodes: <count> | Truncated: false`.
   - Structured JSON node hierarchy appears in UI.
   - Event log records `[OBSERVATION] OBSERVATION_STARTED` and `OBSERVATION_COMPLETED`.
   - **Zero Action Execution:** Phone performs NO clicks, scrolls, or back/home dispatches.

### Test 3.3 (P3.1-DEV-EXT-001): External Chrome + Android Recents Preservation Procedure
- **Status:** **NOT_RUN** (Pending physical phone test execution)
1. Launch LocalAgent on physical phone.
2. Verify Accessibility Service = `READY (BOUND)`.
3. Open Google Chrome manually.
4. Wait until Chrome is clearly in the foreground.
5. Open `ExternalObservationActivity` in LocalAgent.
6. Click "Capture External UI".
7. Verify validated target package = `com.android.chrome` (or valid Chrome package).
8. Verify node tree corresponds to Chrome UI hierarchy.
9. Open Android Recents manually (`com.android.systemui`).
10. Verify `com.android.systemui` is REJECTED as an external candidate and does NOT replace the stored Chrome snapshot.
11. Return to LocalAgent.
12. Open `ExternalObservationActivity`.
13. Verify Last Valid External Snapshot still shows `com.android.chrome`.
14. Vertically and horizontally scroll through the complete external node tree.
15. Confirm no automatic click, scroll, typing, or navigation occurred in Chrome (Read-Only guarantee).

### Test 3.4 (P3.2-DEV-EVID-001): Phase 3.2 Observation Evidence Generation & Provenance Hash Procedure
- **Status:** **NOT_RUN** (Pending physical phone test execution)
1. Launch LocalAgent on physical phone.
2. Verify Accessibility Service = `READY (BOUND)`.
3. Open Google Chrome manually.
4. Return to LocalAgent and open `EvidenceActivity`.
5. Click "Capture Live Evidence".
6. Verify Evidence ID, Snapshot ID, Package (`com.localagent.app` for Current, `com.android.chrome` for External), Node count, and Depth are displayed.
7. Verify SHA-256 Provenance Hash is generated and rendered.
8. Vertically and horizontally scroll through the complete evidence primitives stream.
9. Confirm zero action execution or interaction was dispatched against observed applications.

### Test 3.5 (P3.3-DEV-PERM-001): Phase 3.3 Permission Center & Categorized Inventory Procedure
- **Status:** **NOT_RUN** (Pending physical phone test execution)
1. Launch LocalAgent on physical phone.
2. Tap `Permission Center` button on `MainActivity`.
3. Verify `PermissionActivity` displays 3 distinct sections: `SECTION 1: REQUIRED NOW`, `SECTION 2: AVAILABLE / OPTIONAL NOW`, and `SECTION 3: FUTURE PHASE PERMISSIONS`.
4. Verify `SECTION 1` displays Accessibility Service with status `ACTION REQUIRED — SERVICE_UNBOUND` and active "Open Accessibility Settings" button.
5. Verify `SECTION 2` displays Storage & SAF Document Access with active "Test Launch SAF File Picker" button.
6. Verify `SECTION 3` displays 7 future-phase permissions (`Overlay`, `Write Settings`, `Usage Access`, `Notification Listener`, `Microphone`, `Post Notifications`, `Camera`) marked as `NOT CURRENTLY REQUIRED — INVENTORY ONLY` with NO active request buttons.
7. Tap `Open Accessibility Settings` in `SECTION 1`. Verify Android System Accessibility Settings screen opens.
8. Enable LocalAgent Accessibility Service and return to LocalAgent. Verify Accessibility status updates to `AVAILABLE / GRANTED`.
9. Disable LocalAgent Accessibility Service in Android Settings and return to LocalAgent.
10. Open `CurrentObservationActivity`, `ExternalObservationActivity`, or `EvidenceActivity`. Verify status banner displays `Status: SERVICE_UNBOUND (Passive Degradation) — Tap Permission Center to Enable`.
11. Confirm no duplicate `Open Accessibility Settings` buttons exist on observation or evidence screens.

---

## 4. Phase 5 — Observation & Snapshot Engine Physical Procedure

### Test 5.1 (P5-DEV-SNAP-001): Single-Root, Node Identity, Stop Control & Heap Footprint Procedure
- **Status:** **NOT_RUN** (Pending physical phone test execution)
1. Launch LocalAgent on physical phone (Android 8.1 API 27 baseline).
2. Ensure Accessibility Service is enabled in Android Settings.
3. Open `CurrentObservationActivity`.
4. Tap `Start Current Observation`. Verify node tree renders with `nodeIdentity`, `identityConfidence` (`EXACT`, `HIGH`, `MEDIUM`), and `bounds` (`[left,top,right,bottom] (WxH)`).
5. Tap `Stop Observation`. Verify Toast displays "Current observation stopped. Service remains active." Verify Accessibility Service remains bound (`AgentAccessibilityService.isBound == true`).
6. Open `ExternalObservationActivity`.
7. Launch Google Calculator or Chrome manually in foreground, then return to LocalAgent.
8. Tap `Start External Observation`. Verify external node tree displays Calculator or Chrome hierarchy.
9. Tap `Stop Observation`. Verify Toast displays "External app observation stopped. Service remains active."
10. Attach Android Studio Profiler or run `adb shell dumpsys meminfo com.localagent.app`.
11. Perform 10 consecutive observation cycles.
12. Confirm process heap footprint remains strictly < 35 MB on API 27.
13. Confirm zero action dispatches (clicks, scrolls, typing, or navigation) occurred.

---

## 5. Phase 11 — Movable Overlay Test Procedure

### Test 11.1: Overlay Display & Cross-Channel Parity
1. **Grant Permission:** Grant "Draw over other apps" (System Alert Window).
2. **Launch Overlay:** Click "Show Overlay".
3. **Execution:** Drag floating icon across screen. Click `BACK` button on overlay.
4. **Parity Verification:** Verify execution trace in `agent.db` matches Console `back` command identically.

---

## 6. Phase 12 — Hardware Controls Test Procedure

### Test 12.1: Volume & Backlight Adjustment
1. **Volume Command:** Type `volume 5` or adjust slider.
2. **Expected Result:** Media volume adjusts physically on device.
3. **Brightness Command:** Type `brightness 80`.
4. **Expected Result:** Grant `WRITE_SETTINGS` if prompted. Backlight adjusts physically.
