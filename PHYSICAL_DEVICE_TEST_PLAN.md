# PHYSICAL_DEVICE_TEST_PLAN.md — Physical Phone Manual Verification Procedures

## 1. Executive Summary

This document specifies the exact physical-device manual test procedures for every capability across the **LocalAgent** phase roadmap.

A capability is NOT verified merely because unit tests pass. Each procedure defines the setup, expected grant/deny UI states, execution results, and uninstall/reinstall recovery behavior on real Android hardware.

---

## 2. Phase 2 — Durable Long-Term Memory Storage Test Procedure

### Test 2.1: Write Memory & Process Restart
1. **Target Capability:** `DURABLE_STORAGE`
2. **Setup:** Install LocalAgent APK on physical phone (Android 8.1 API 27 or newer).
3. **Execution:** Open LocalAgent. Enter command: `click 7`.
4. **Expected UI Display:**
   - EVENT STORAGE: `Location: APP_PRIVATE (/data/data/.../files/agent/)`
   - LONG-TERM MEMORY: `Location: INTERNAL_SHARED_STORAGE (/sdcard/LocalAgent/memory/)`, `Status: AVAILABLE_DURABLE_EXTERNAL`, `Durable Records: 1`
5. **Process Kill:** Force stop LocalAgent from Android Settings -> Apps -> LocalAgent -> Force Stop. Reopen LocalAgent.
6. **Verification:** Confirm diagnostic UI hydrates recent events from `agent.db` AND discovers `Durable Records: 1`.

### Test 2.2: App Uninstall & Reinstall Memory Survival
1. **Execution:** Uninstall LocalAgent from phone. (*Do NOT delete the `/sdcard/LocalAgent/` folder*).
2. **Reinstall:** Install LocalAgent APK again. Launch app.
3. **Verification:** Confirm diagnostic UI reports `Location: INTERNAL_SHARED_STORAGE` and `Durable Records: 1`. Long-term memory records survived uninstall.
4. **File Manager Inspection:** Open phone's File Manager -> Internal Storage -> `LocalAgent/memory/`. Confirm `.json` record files are visible.

---

## 3. Phase 3 — Permission & Capability Center Test Procedure

### Test 3.1: Runtime Storage Permission Grant / Deny
1. **Setup:** Open LocalAgent Permission Center UI.
2. **Grant Flow:** Click "Enable Durable Storage Access". Grant permission dialog or select SAF document tree folder.
3. **Expected Result:** Status transitions from `PERMISSION_REQUIRED` -> `READY`.
4. **Deny Flow:** Revoke permission in Settings. Reopen app.
5. **Expected Result:** Status transitions to `PERMISSION_REQUIRED` or `APP_PRIVATE_ONLY`. App remains 100% functional without crashing.

---

## 4. Phase 4 & 7 — Accessibility Service & Global Action Test Procedure

### Test 4.1: Accessibility Unbound vs Bound Execution
1. **Unbound State:** Launch app before enabling Accessibility. Type `back` in Console.
2. **Expected Result:** Returns `ACCESSIBILITY_UNAVAILABLE | Reason: Accessibility Service unbound`. App does NOT crash.
3. **Enable Service:** Go to Settings -> Accessibility -> LocalAgent -> Enable.
4. **Bound Execution:** Type `back`.
5. **Expected Result:** Phone executes global Back navigation. Screen changes to previous window. Result logged as `SUCCESS_VERIFIED`.

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
