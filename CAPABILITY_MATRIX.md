# CAPABILITY_MATRIX.md — LocalAgent Capability Matrix & API Tier Specifications

## 1. Executive Overview

This matrix defines every supported capability in **LocalAgent**, mapping each capability to its required Android API, required permission, fallback strategy, verification method, resource cost classification, and API compatibility across **Android 8.1 (API 27) through Android 15+ (API 36+)**.

### Capability Resource Cost Tiers
- **LOW:** Memory footprint < 5 MB, CPU < 2%, execution duration < 100ms.
- **MEDIUM:** Memory footprint 5–20 MB, CPU 2–10%, execution duration 100ms–500ms.
- **HIGH:** Memory footprint > 20 MB, CPU > 10%, execution duration > 500ms.

---

## 2. Universal Capability Matrix

| Capability ID | Category | Min API | API 27 Implementation | API 28–36+ Implementation | Required Permission | Fallback Strategy | Verification Method | Resource Cost |
|---|---|---|---|---|---|---|---|---|
| `GLOBAL_BACK` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_BACK)` | Identical | Accessibility Service | None | Foreground Window / Package Diff | LOW |
| `GLOBAL_HOME` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)` | Identical | Accessibility Service | Home Launcher Intent | Foreground Package == Home Launcher | LOW |
| `GLOBAL_RECENTS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS)` | Identical | Accessibility Service | None | Recents Window State Diff | LOW |
| `GLOBAL_NOTIFICATIONS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)` | Identical | Accessibility Service | None | Notification Shade Window Diff | LOW |
| `GLOBAL_QUICK_SETTINGS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)` | Identical | Accessibility Service | None | Quick Settings Window Diff | LOW |
| `GLOBAL_LOCK_SCREEN` | Navigation | API 28 | Not Available (Returns `UNSUPPORTED_API`) | `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | Accessibility Service | Device Admin Lock | Display Power State Off | LOW |
| `GLOBAL_TAKE_SCREENSHOT` | Navigation | API 28 | MediaProjection Capture API | `performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)` | Accessibility / MediaProjection | MediaProjection Session | Image Bitmap Generated & Saved | MEDIUM |
| `UI_CLICK` | UI Control | API 27 | Live node acquisition + `findClickableAncestor()` + `ACTION_CLICK` | Identical | Accessibility Service | Gesture Dispatch (`dispatchGesture`) | UI Snapshot Diff (Text/Window change) | LOW |
| `UI_LONG_CLICK` | UI Control | API 27 | Live node acquisition + `findLongClickableAncestor()` + `ACTION_LONG_CLICK` | Identical | Accessibility Service | Long Press Gesture | UI Snapshot Diff (ContextMenu change) | LOW |
| `UI_TEXT_INPUT` | UI Control | API 27 | Live node acquisition + `findEditableTarget()` + `ACTION_SET_TEXT` | Identical | Accessibility Service | Focus + Key Events | Node Text == Expected String | LOW |
| `UI_SCROLL_FORWARD` | UI Control | API 27 | Live node acquisition + `findScrollableAncestor()` + `ACTION_SCROLL_FORWARD` | Identical | Accessibility Service | Swipe Gesture Up | Visible Nodes / Scroll Position Diff | LOW |
| `UI_SCROLL_BACKWARD` | UI Control | API 27 | Live node acquisition + `findScrollableAncestor()` + `ACTION_SCROLL_BACKWARD` | Identical | Accessibility Service | Swipe Gesture Down | Visible Nodes / Scroll Position Diff | LOW |
| `UI_SWIPE_GESTURE` | UI Control | API 27 | `AccessibilityService.dispatchGesture()` Path Stroke | Identical | Accessibility Service | None | UI Snapshot Diff | LOW |
| `APP_LAUNCH` | System | API 27 | `PackageManager.getLaunchIntentForPackage()` + `startActivity()` | API 30+ requires `<queries>` declaration in Manifest | None | Accessibility UI Click on Launcher Icon | Foreground Package == Target Package | LOW |
| `APP_RESOLVE` | System | API 27 | `PackageManager.queryIntentActivities()` | API 30+ `<queries>` matching | None | Fuzzy Label Matcher | Resolved Package Name != null | LOW |
| `OVERLAY_SHOW` | UI Surface | API 27 | `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` | Identical | `SYSTEM_ALERT_WINDOW` | Console UI Only | Window Attached & Visible | LOW |
| `OVERLAY_MOVE` | UI Surface | API 27 | `WindowManager.updateViewLayout()` with Touch Listener | Identical | `SYSTEM_ALERT_WINDOW` | None | Overlay View Coordinates Updated | LOW |
| `HARDWARE_VOLUME` | Hardware | API 27 | `AudioManager.setStreamVolume()` | Identical | None | None | `AudioManager.getStreamVolume()` Diff | LOW |
| `HARDWARE_BRIGHTNESS` | Hardware | API 27 | `Settings.System.putInt(SCREEN_BRIGHTNESS)` | Identical | `WRITE_SETTINGS` | None | `Settings.System.getInt()` Diff | LOW |
| `HARDWARE_FLASHLIGHT` | Hardware | API 23 | `CameraManager.setTorchMode()` | Identical | Camera Permission | None | Torch State Callback | LOW |
| `HARDWARE_MEDIA_CONTROL`| Hardware | API 27 | `AudioManager.dispatchMediaKeyEvent()` | Identical | None | Accessibility Key Event | Audio Output State Change | LOW |
| `SPEECH_STT` | Voice | API 27 | `SpeechRecognizer.createSpeechRecognizer()` (System online/offline) | API 31+ `SpeechRecognizer.isOnDeviceRecognitionAvailable()` | `RECORD_AUDIO` | Manual Text Command | Transcript String Returned | MEDIUM |
| `SPEECH_TTS` | Voice | API 27 | `TextToSpeech` engine synthesis | Identical | None | Silent Execution | `UtteranceProgressListener.onDone()` | LOW |
| `WORKFLOW_EXECUTE` | Automation | API 27 | Step-by-step sequential dispatcher | Identical | Inherited from actions | Manual Step-by-Step | Workflow Execution Result Log | LOW |
| `EPISODIC_MEMORY_STORE` | Memory | API 27 | SQLite/Room event storage | Identical | None | File Storage | DB Row Count Incremented | LOW |
| `PROCEDURAL_LEARNING` | Memory | API 27 | Sequence extractor & target validator | Identical | None | None | Revalidated Target Match | LOW |
| `BROWSER_NAVIGATE` | Research | API 27 | App launch Chrome/Browser + URL intent | Identical | None | None | Browser Package Foreground | LOW |
| `EXTERNAL_KNOWLEDGE_IMPORT`| Research | API 27 | JSON/Text Parser for exported ChatGPT/Gemini chats | Identical | `READ_EXTERNAL_STORAGE` (or SAF) | SAF Document Picker | Parsed Knowledge Entry Stored | LOW |

---

## 3. API Level Compatibility Matrix (API 27 – API 36+)

```text
Capability ID              API 27   API 28   API 29   API 30   API 31   API 32   API 33   API 34   API 35+
---------------------------------------------------------------------------------------------------------
GLOBAL_BACK                 YES      YES      YES      YES      YES      YES      YES      YES      YES
GLOBAL_HOME                 YES      YES      YES      YES      YES      YES      YES      YES      YES
GLOBAL_RECENTS              YES      YES      YES      YES      YES      YES      YES      YES      YES
GLOBAL_NOTIFICATIONS        YES      YES      YES      YES      YES      YES      YES      YES      YES
GLOBAL_QUICK_SETTINGS       YES      YES      YES      YES      YES      YES      YES      YES      YES
GLOBAL_LOCK_SCREEN          NO*      YES      YES      YES      YES      YES      YES      YES      YES
GLOBAL_TAKE_SCREENSHOT      ALT**    YES      YES      YES      YES      YES      YES      YES      YES
UI_CLICK                    YES      YES      YES      YES      YES      YES      YES      YES      YES
UI_LONG_CLICK               YES      YES      YES      YES      YES      YES      YES      YES      YES
UI_TEXT_INPUT               YES      YES      YES      YES      YES      YES      YES      YES      YES
UI_SCROLL_FORWARD           YES      YES      YES      YES      YES      YES      YES      YES      YES
UI_SCROLL_BACKWARD          YES      YES      YES      YES      YES      YES      YES      YES      YES
APP_LAUNCH                  YES      YES      YES      MANUAL***MANUAL   MANUAL   MANUAL   MANUAL   MANUAL
OVERLAY_SHOW                YES      YES      YES      YES      YES      YES      YES      YES      YES
HARDWARE_VOLUME             YES      YES      YES      YES      YES      YES      YES      YES      YES
HARDWARE_BRIGHTNESS         YES      YES      YES      YES      YES      YES      YES      YES      YES
SPEECH_STT (Offline)        NO****   NO       NO       NO       YES      YES      YES      YES      YES
SPEECH_TTS                  YES      YES      YES      YES      YES      YES      YES      YES      YES
```

### Compatibility Legend & Notes
- **YES:** Native API supported directly.
- **NO\* (GLOBAL_LOCK_SCREEN):** `GLOBAL_ACTION_LOCK_SCREEN` added in API 28. On API 27, fallback uses Device Policy Manager or displays lock guidance.
- **ALT\*\* (GLOBAL_TAKE_SCREENSHOT):** `GLOBAL_ACTION_TAKE_SCREENSHOT` added in API 28. On API 27, uses MediaProjection capture API session.
- **MANUAL\*\*\* (APP_LAUNCH):** API 30+ enforces package visibility restrictions (`<queries>` manifest declarations required for target packages).
- **NO\*\*\*\* (SPEECH_STT Offline):** On-device offline STT API (`SpeechRecognizer.isOnDeviceRecognitionAvailable()`) added in API 31. On API 27–30, uses system default recognizer (which may require network if offline speech packs aren't pre-installed by OEM).
