# HARDWARE_CAPABILITY_MATRIX.md — Hardware Capabilities & Fallback Strategy Matrix

## 1. Executive Summary

Hardware capabilities in **LocalAgent** (Volume, Brightness, Flashlight, Media Controls, Screen State, Wi-Fi) are classified by API support tiers. The agent uses direct system APIs whenever available and falls back to Settings UI automation via Accessibility Service when direct APIs are restricted on newer Android releases.

---

## 2. Hardware Capability Classification Tiers

| Hardware Capability | Direct System API | Required Permission / Special Access | Direct API Supported API Range | Fallback Strategy (API 29+) | Risk Level | Phase |
|---|---|---|---|---|---|---|
| **Volume Control** | `AudioManager` (`setStreamVolume`) | None | API 27 – API 36+ | None Required | `MEDIUM` | Phase 12 |
| **Media Play / Pause** | `AudioManager` (`dispatchMediaKeyEvent`) | None | API 27 – API 36+ | None Required | `LOW` | Phase 12 |
| **Screen Backlight** | `Settings.System.putInt` | `WRITE_SETTINGS` | API 27 – API 36+ | Open Display Settings UI | `MEDIUM` | Phase 12 |
| **Flashlight Toggle** | `CameraManager.setTorchMode` | `CAMERA` | API 27 – API 36+ | None Required | `LOW` | Phase 12 |
| **Device Vibration** | `Vibrator` (`vibrate`) | `VIBRATE` | API 27 – API 36+ | None Required | `LOW` | Phase 12 |
| **Wi-Fi Toggle** | `WifiManager.setWifiEnabled` | `CHANGE_WIFI_STATE` | API 27 – API 28 | Settings Intent / Quick Settings UI | `MEDIUM` | Phase 12 |
| **Bluetooth Toggle** | `BluetoothAdapter.enable` | `BLUETOOTH_ADMIN` / `BLUETOOTH_CONNECT` | API 27 – API 30 | Quick Settings UI | `MEDIUM` | Phase 12 |
| **Screen Lock** | `AccessibilityService.performGlobalAction` | `AccessibilityService` | API 28 – API 36+ | DevicePolicyManager (`lockNow`) | `HIGH` | Phase 12 |
| **Screenshot Capture** | `AccessibilityService.performGlobalAction` | `AccessibilityService` | API 28 – API 36+ | MediaProjection API | `LOW` | Phase 12 |
| **Microphone Input** | `SpeechRecognizer` | `RECORD_AUDIO` | API 27 – API 36+ | None Required | `MEDIUM` | Phase 13 |
| **Audio Output** | `TextToSpeech` | None | API 27 – API 36+ | System Speaker | `LOW` | Phase 13 |

---

## 3. Fallback Automation Rules
1. **Direct API Attempt:** Attempt direct system API call first.
2. **Permission Check:** If direct API throws `SecurityException` or returns false, verify permission status.
3. **UI Automation Fallback:** If permission is restricted or API is deprecated on API 29+ (e.g. `WifiManager.setWifiEnabled`), launch system Settings or Quick Settings overlay and perform UI click/toggle via `AccessibilityService`.
4. **Explicit Reporting:** If both direct API and UI fallback fail, return `HARDWARE_UNAVAILABLE` or `PERMISSION_REQUIRED`.
