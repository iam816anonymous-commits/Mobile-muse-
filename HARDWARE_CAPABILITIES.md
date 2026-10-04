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

## 3. Comprehensive Hardware Capability Matrix

| Hardware Capability | Support Tier | Min API | Android API / Mechanism | Required Permission / Access | Risk Level | User Confirmation Policy | Platform Limitation & OEM Behavior |
|---|---|---|---|---|---|---|---|
| **Volume Up / Down / Mute** | SUPPORTED | API 27 | `AudioManager.setStreamVolume()` | None | LOW | Automatic Execution | Stream volume changes instantly. Mute supported via `ADJUST_MUTE` or volume 0. |
| **Media Play / Pause / Next / Prev** | SUPPORTED | API 27 | `AudioManager.dispatchMediaKeyEvent()` | None | LOW | Automatic Execution | Dispatches `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`. |
| **Display Brightness Adjust** | LIMITED | API 27 | `Settings.System.putInt(SCREEN_BRIGHTNESS)` | `WRITE_SETTINGS` | MEDIUM | Automatic (if granted) | Auto-brightness must be turned off (`SCREEN_BRIGHTNESS_MODE_MANUAL`) to take effect. |
| **Flashlight / Torch On / Off** | SUPPORTED | API 23 | `CameraManager.setTorchMode()` | `CAMERA` (API 27) | LOW | Automatic Execution | Fails if camera hardware is in use by another application. |
| **Vibration Feedback** | SUPPORTED | API 27 | `Vibrator.vibrate(VibrationEffect)` | `VIBRATE` | LOW | Automatic Execution | Works universally across all API levels. |
| **Screen Auto-Rotate Toggle** | LIMITED | API 27 | `Settings.System.putInt(ACCELEROMETER_ROTATION)` | `WRITE_SETTINGS` | LOW | Automatic (if granted) | Controls system auto-rotate toggle. |
| **Ringer Mode (Silent/Vibrate/Normal)**| LIMITED | API 27 | `AudioManager.setRingerMode()` | Do Not Disturb Access | MEDIUM | Policy-Dependent | Setting silent mode requires Do Not Disturb access on API 24+. |
| **Bluetooth Toggle (API 27–32)** | LIMITED | API 27–32 | `BluetoothAdapter.enable()` / `disable()` | `BLUETOOTH_ADMIN` | MEDIUM | Policy-Dependent | **Direct API blocked on API 33+ (Android 13+)**. |
| **Bluetooth Toggle (API 33+)** | LIMITED | API 33+ | System Intent / Quick Settings UI Flow | Accessibility Service | MEDIUM | Policy-Dependent | Uses Quick Settings UI Automation flow. |
| **Wi-Fi Toggle (API 27–28)** | SUPPORTED | API 27–28 | `WifiManager.setWifiEnabled()` | `CHANGE_WIFI_STATE` | MEDIUM | Policy-Dependent | Direct API toggle supported on Android 8.1–9.0. |
| **Wi-Fi Toggle (API 29+)** | RESTRICTED | API 29+ | Settings Panel Intent (`Settings.Panel.ACTION_WIFI`) + A11y | Accessibility Service | MEDIUM | Policy-Dependent | **Direct API strictly blocked on Android 10+**. Switched to UI Automation flow. |
| **Airplane Mode Toggle** | RESTRICTED | API 17+ | Settings Page Intent + Accessibility UI Click | Accessibility Service | HIGH | Interactive Confirmation | Programmatic modification blocked since API 17. Must use Settings UI Automation. |
| **Power Off / Reboot Device** | RESTRICTED | API 27+ | Power Menu Dialog via Accessibility (`GLOBAL_ACTION_POWER_DIALOG`) | Accessibility Service | HIGH | Interactive Confirmation | Third-party non-root apps cannot reboot device directly. Uses Power Menu UI. |
| **Lock Screen Display** | SUPPORTED | API 28 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | Accessibility Service | LOW | Automatic Execution | On API 27, fallback uses legacy Device Policy Manager `lockNow()`. |
| **Screen Off / On Detection** | SUPPORTED | API 27 | BroadcastReceiver (`ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`) | None | LOW | Passive Monitoring | Passive event monitoring. |
| **Do Not Disturb Toggle** | LIMITED | API 27 | `NotificationManager.setInterruptionFilter()` | `ACCESS_NOTIFICATION_POLICY` | MEDIUM | Policy-Dependent | Requires user to grant Notification Policy access in System Settings. |
| **Location / GPS Toggle** | RESTRICTED | API 27+ | Location Settings Intent + Accessibility UI Click | Accessibility Service | HIGH | Interactive Confirmation | Direct API toggle blocked for non-system apps. |
| **NFC Toggle** | RESTRICTED | API 27+ | NFC Settings Panel Intent + Accessibility | Accessibility Service | HIGH | Interactive Confirmation | Direct API toggle blocked for non-system apps. |
| **Mobile Data Toggle** | RESTRICTED | API 27+ | Quick Settings Panel Intent + Accessibility | Accessibility Service | HIGH | Interactive Confirmation | Direct API toggle blocked for non-system apps. |
| **Wi-Fi Hotspot Toggle** | RESTRICTED | API 27+ | Tethering Settings Intent + Accessibility UI Click | Accessibility Service | HIGH | Interactive Confirmation | Direct API toggle blocked for non-system apps. |

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
