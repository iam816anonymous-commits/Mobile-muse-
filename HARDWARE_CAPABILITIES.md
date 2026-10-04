# HARDWARE_CAPABILITIES.md — Hardware Control & System API Boundaries

## 1. Executive Summary & Security Boundaries

Hardware and system controls in **LocalAgent** allow the agent to manage device settings, media playback, volume, display, and basic connectivity. However, Android's security architecture places strict limits on what third-party non-root applications can alter.

This document classifies every planned hardware capability into explicit support categories, detailing API requirements, system permissions, risk levels, user confirmation policies, and platform restrictions across **Android 8.1 (API 27) through Android 15+ (API 36+)**.

---

## 2. Hardware Capability Classification Tiers

All hardware features are strictly categorized into one of five classification tiers:

1. **SUPPORTED:** Fully controllable via standard Android public APIs or Accessibility APIs.
2. **LIMITED:** Supported, but requires special user-granted access (e.g., `WRITE_SETTINGS`) or interactive user consent.
3. **OEM-DEPENDENT:** API exists in Android SDK, but behavior varies based on OEM custom ROMs (Samsung, Xiaomi, OPPO, Vivo, Google Pixel).
4. **ADB / DEVELOPMENT-ONLY:** Requires ADB shell privileges (`adb shell pm grant ...`) or developer options enabled; used only during automated device testing.
5. **NOT POSSIBLE THROUGH NORMAL APPS (RESTRICTED):** Privileged system operation blocked by Android OS security model for third-party non-root apps. **Never fake execution for these.**

---

## 3. Hardware Capability Matrix & Risk Policy

| Hardware Capability | Support Tier | Min API | Android Mechanism | Permission / Access | Risk Level | User Confirmation Policy |
|---|---|---|---|---|---|---|
| **Volume Up / Down / Mute** | SUPPORTED | API 27 | `AudioManager.setStreamVolume()` | None | LOW | Automatic Execution |
| **Media Play / Pause / Next / Prev** | SUPPORTED | API 27 | `AudioManager.dispatchMediaKeyEvent()` | None | LOW | Automatic Execution |
| **Display Brightness Adjust** | LIMITED | API 27 | `Settings.System.putInt(SCREEN_BRIGHTNESS)` | `WRITE_SETTINGS` | MEDIUM | Automatic (if granted) |
| **Flashlight / Torch On / Off** | SUPPORTED | API 23 | `CameraManager.setTorchMode()` | `CAMERA` (API 27) | LOW | Automatic Execution |
| **Vibration Feedback** | SUPPORTED | API 27 | `Vibrator.vibrate(VibrationEffect)` | `VIBRATE` | LOW | Automatic Execution |
| **Screen Auto-Rotate Toggle** | LIMITED | API 27 | `Settings.System.putInt(ACCELEROMETER_ROTATION)` | `WRITE_SETTINGS` | LOW | Automatic (if granted) |
| **Ringer Mode (Silent/Vibrate/Normal)**| LIMITED | API 27 | `AudioManager.setRingerMode()` | Do Not Disturb Access | MEDIUM | Policy-Dependent |
| **Bluetooth Toggle** | LIMITED | API 27–32 | `BluetoothAdapter.enable()` / `disable()` | `BLUETOOTH_ADMIN` | MEDIUM | Policy-Dependent |
| **Wi-Fi Toggle** | RESTRICTED (API 29+) | API 27–28 | Direct API (API 27–28) / UI Automation (API 29+) | `CHANGE_WIFI_STATE` / A11y | MEDIUM | Policy-Dependent |
| **Airplane Mode Toggle** | RESTRICTED | API 17+ | Settings Page Intent + Accessibility UI Click | Accessibility Service | HIGH | Interactive Confirmation |
| **Power Off / Reboot Device** | RESTRICTED | API 27+ | Power Menu Dialog via Accessibility (`GLOBAL_ACTION_POWER_DIALOG`) | Accessibility Service | HIGH | Interactive Confirmation |
| **Lock Screen Display** | SUPPORTED | API 28 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | Accessibility Service | LOW | Automatic Execution |
| **Screen Off / On Detection** | SUPPORTED | API 27 | BroadcastReceiver (`ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`) | None | LOW | Passive Monitoring |
| **NFC Toggle** | RESTRICTED | API 27+ | NFC Settings Panel Intent + Accessibility | Accessibility Service | HIGH | Interactive Confirmation |
| **Cellular Mobile Data Toggle** | RESTRICTED | API 27+ | Quick Settings Panel Intent + Accessibility | Accessibility Service | HIGH | Interactive Confirmation |

---

## 4. Hardware Command Implementations & Risk Policy Integration

### 4.1 Brightness Control Protocol
1. Check `Settings.System.canWrite(context)`.
2. If `false`, return `HardwareCapabilityResult.PermissionRequired("WRITE_SETTINGS", Settings.ACTION_MANAGE_WRITE_SETTINGS)`.
3. If `true`, disable auto-brightness if enabled:
   `Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)`
4. Set target brightness integer value (0 to 255):
   `Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetValue)`
5. Verify new value by reading back `Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)`.

### 4.2 Wi-Fi & Connectivity Strategy (API 27 vs API 29+)
- **API 27–28 (Android 8.1–9.0):** Execute direct API toggle via `WifiManager.setWifiEnabled(boolean)`.
- **API 29+ (Android 10+):** `WifiManager.setWifiEnabled()` is blocked for non-system apps.
- **UI Automation Adapter Strategy (API 29+):**
  1. Evaluate risk via `ActionPolicyEngine` (MEDIUM risk).
  2. Launch Wi-Fi Settings Panel (`Settings.Panel.ACTION_WIFI`).
  3. Use `ObservationSnapshot` to resolve Wi-Fi switch target node.
  4. Dispatch `UI_CLICK` action on actionable ancestor.
  5. Verify Wi-Fi state diff.
