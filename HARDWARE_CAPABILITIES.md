# HARDWARE_CAPABILITIES.md — Hardware Control & System API Boundaries

## 1. Executive Summary & Security Boundaries

Hardware and system controls in **LocalAgent** allow the agent to manage device settings, media playback, volume, display, and basic connectivity. However, Android's security architecture places strict limits on what third-party non-root applications can alter.

This document classifies every planned hardware capability into explicit support categories, detailing API requirements, system permissions, and platform restrictions across **Android 8.1 (API 27) through Android 15+ (API 36+)**.

---

## 2. Hardware Capability Support Categories

All hardware features are strictly categorized into one of five classification tiers:

1. **SUPPORTED:** Fully controllable via standard Android public APIs or Accessibility APIs.
2. **LIMITED:** Supported, but requires special user-granted access (e.g., `WRITE_SETTINGS`) or interactive user consent.
3. **OEM-DEPENDENT:** API exists in Android SDK, but behavior varies based on OEM custom ROMs (Samsung, Xiaomi, OPPO, Vivo, Google Pixel).
4. **ADB / DEVELOPMENT-ONLY:** Requires ADB shell privileges (`adb shell pm grant ...`) or developer options enabled; used only during automated device testing.
5. **NOT POSSIBLE THROUGH NORMAL APPS (RESTRICTED):** Privileged system operation blocked by Android OS security model for third-party non-root apps. **Never fake execution for these.**

---

## 3. Hardware Capability Classification Table

| Hardware Capability | Support Category | Min API | Android API / Mechanism | Required Permission / Access | Platform Limitation & OEM Behavior |
|---|---|---|---|---|---|
| **Volume Up / Down / Mute** | SUPPORTED | API 27 | `AudioManager.setStreamVolume()` | None | Stream volume changes instantly. Mute supported via `ADJUST_MUTE` or volume 0. |
| **Media Play / Pause / Next / Prev** | SUPPORTED | API 27 | `AudioManager.dispatchMediaKeyEvent()` | None | Dispatches `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_MEDIA_NEXT`, `KEYCODE_MEDIA_PREVIOUS`. |
| **Display Brightness Adjust** | LIMITED | API 27 | `Settings.System.putInt(SCREEN_BRIGHTNESS)` | `WRITE_SETTINGS` | Auto-brightness must be turned off (`SCREEN_BRIGHTNESS_MODE_MANUAL`) to take effect. |
| **Flashlight / Torch On / Off** | SUPPORTED | API 23 | `CameraManager.setTorchMode()` | `CAMERA` permission on API 27 | Fails if camera hardware is currently in use by another application. |
| **Vibration Feedback** | SUPPORTED | API 27 | `Vibrator.vibrate(VibrationEffect)` | `VIBRATE` | Works universally across all API levels. |
| **Screen Orientation Change** | LIMITED | API 27 | `Settings.System.putInt(ACCELEROMETER_ROTATION)` | `WRITE_SETTINGS` | Controls system auto-rotate toggle. Individual app orientation override requires active window overlay. |
| **Ringer Mode (Silent / Vibrate / Normal)** | LIMITED | API 27 | `AudioManager.setRingerMode()` | `ACCESS_NOTIFICATION_POLICY` (Do Not Disturb Access) | Setting silent mode requires Do Not Disturb access on API 24+. |
| **Bluetooth Toggle** | LIMITED | API 27–32 | `BluetoothAdapter.enable()` / `disable()` | `BLUETOOTH_ADMIN` | **Deprecated & restricted in API 33+ (Android 13+)**. On API 33+, requires showing Bluetooth system dialog. |
| **Wi-Fi Toggle** | RESTRICTED (API 29+) | API 27–28 | `WifiManager.setWifiEnabled()` | `CHANGE_WIFI_STATE` | **Strictly blocked in API 29+ (Android 10+)**. Non-system apps cannot toggle Wi-Fi directly; must launch Wi-Fi settings panel. |
| **Airplane Mode Toggle** | RESTRICTED | API 17+ | `Settings.Global.putInt(AIRPLANE_MODE_ON)` | System Signature / Root | Programmatic modification blocked for third-party apps since API 17. Must guide user via Settings page or Accessibility UI click. |
| **Power Off / Reboot Device** | RESTRICTED | API 27+ | `PowerManager.reboot()` | Privileged System / Root | Third-party apps cannot reboot device. On API 28+, `GLOBAL_ACTION_POWER_DIALOG` can open the power menu via Accessibility. |
| **Lock Screen Display** | SUPPORTED (API 28+) | API 28 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | Accessibility Service | On API 27, fallback uses Device Policy Manager `lockNow()` if Device Admin is activated. |
| **Screen Off / On Detection** | SUPPORTED | API 27 | BroadcastReceiver (`ACTION_SCREEN_ON`, `ACTION_SCREEN_OFF`) | None | Receiver monitoring screen power events. |
| **NFC Toggle** | RESTRICTED | API 27+ | `NfcAdapter` | System / Root | NFC cannot be toggled programmatically by third-party applications. |
| **Cellular Data Toggle** | RESTRICTED | API 27+ | `TelephonyManager` | System Signature / Root | Direct toggle blocked. Accessibility UI interaction required to toggle mobile data in Quick Settings. |

---

## 4. Hardware Command Capability Definitions

```kotlin
sealed class HardwareCapabilityResult {
    data class Success(val message: String, val newValue: String) : HardwareCapabilityResult()
    data class PermissionRequired(val permissionName: String, val settingsIntent: String) : HardwareCapabilityResult()
    data class UnsupportedApi(val currentApi: Int, val minRequiredApi: Int) : HardwareCapabilityResult()
    data class RestrictedByPlatform(val reason: String, val alternativeGuide: String) : HardwareCapabilityResult()
}
```

### 4.1 Brightness Control Adapter Implementation Protocol
1. Check `Settings.System.canWrite(context)`.
2. If `false`, return `HardwareCapabilityResult.PermissionRequired("WRITE_SETTINGS", Settings.ACTION_MANAGE_WRITE_SETTINGS)`.
3. If `true`, disable auto-brightness if enabled:
   `Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)`
4. Set target brightness integer value (0 to 255):
   `Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, targetValue)`
5. Verify new value by reading back `Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)`.

### 4.2 Wi-Fi & Bluetooth Compatibility Strategy (API 27 vs API 29+)
- **API 27–28 (Android 8.1–9.0):** Execute direct toggle via `WifiManager.setWifiEnabled(boolean)` or `BluetoothAdapter.enable()`.
- **API 29+ (Android 10+):** `WifiManager.setWifiEnabled()` returns false and logs deprecation warning.
- **Adapter Strategy:** On API 29+, LocalAgent automatically switches mechanism from direct API toggle to **UI Automation Flow**:
  1. Open Quick Settings or Settings Wi-Fi Panel (`Settings.Panel.ACTION_WIFI`).
  2. Use `AccessibilityService` observation engine to locate Wi-Fi switch node.
  3. Dispatch `UI_CLICK` action on actionable ancestor to toggle state.
  4. Verify state diff.
