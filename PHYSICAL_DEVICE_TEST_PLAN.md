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

## 3. Phase 3.1 — Read-Only Accessibility Observation Test Procedure

### Test 3.1: Service Unbound State
1. **Setup:** Install LocalAgent APK on physical phone. Do NOT enable Accessibility Service yet.
2. **Launch:** Open LocalAgent.
3. **Expected UI Display:**
   - ACCESSIBILITY OBSERVATION SERVICE: `Status: SERVICE_UNBOUND | Active Pkg: None`
4. **Command Trigger:** Type `observe` in command input or click "Observe UI".
5. **Expected Result:** Output displays `Command: OBSERVE | Status: ACCESSIBILITY_UNAVAILABLE | Reason: Accessibility Service unbound (Open Settings to enable)`. App does NOT crash.

### Test 3.2: Service Enablement & Live UI Observation
1. **Enablement:** Click "Open Accessibility Settings" button on LocalAgent UI.
2. **Android Settings:** In Android Accessibility Settings, find "LocalAgent Read-Only UI Observation Service" and enable toggle.
3. **Return:** Return to LocalAgent.
4. **Expected UI Display:** `Status: READY (BOUND)`.
5. **Observation Execution:** Click "Observe UI" button.
6. **Expected Result:**
   - Output displays `Command: OBSERVE | Status: SUCCESS_VERIFIED | Pkg: com.localagent.app | Nodes: <count> | Truncated: false`.
   - Structured JSON node hierarchy appears in UI.
   - Event log records `[OBSERVATION] OBSERVATION_STARTED` and `OBSERVATION_COMPLETED`.
   - **Zero Action Execution:** Phone performs NO clicks, scrolls, or back/home dispatches.

---

## 4. Phase 11 — Movable Overlay Test Procedure

### Test 11.1: Overlay Display & Cross-Channel Parity
1. **Grant Permission:** Grant "Draw over other apps" (System Alert Window).
2. **Launch Overlay:** Click "Show Overlay".
3. **Execution:** Drag floating icon across screen. Click `BACK` button on overlay.
4. **Parity Verification:** Verify execution trace in `agent.db` matches Console `back` command identically.

---

## 5. Phase 12 — Hardware Controls Test Procedure

### Test 12.1: Volume & Backlight Adjustment
1. **Volume Command:** Type `volume 5` or adjust slider.
2. **Expected Result:** Media volume adjusts physically on device.
3. **Brightness Command:** Type `brightness 80`.
4. **Expected Result:** Grant `WRITE_SETTINGS` if prompted. Backlight adjusts physically.
