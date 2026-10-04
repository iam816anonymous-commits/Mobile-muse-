# CAPABILITY_MATRIX.md — LocalAgent Capability Matrix & API Tier Specifications

## 1. Executive Overview

This matrix defines every supported capability in **LocalAgent**, mapping each capability to its required Android API, required permission, risk level, fallback strategy, verification method, resource cost classification, and API compatibility across **Android 8.1 (API 27) through Android 15+ (API 36+)**.

---

## 2. Universal Capability Matrix

| Capability ID | Category | Min API | Android API / Mechanism | Required Permission | Risk Level | Fallback Strategy | Verification Strategy | Resource Cost |
|---|---|---|---|---|---|---|---|---|
| `GLOBAL_BACK` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_BACK)` | Accessibility Service | LOW | None | Navigation-aware window/activity diff | LOW |
| `GLOBAL_HOME` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)` | Accessibility Service | LOW | Home Launcher Intent | Foreground Package == Home Launcher | LOW |
| `GLOBAL_RECENTS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS)` | Accessibility Service | LOW | None | Recents Window State Diff | LOW |
| `GLOBAL_NOTIFICATIONS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)` | Accessibility Service | LOW | None | Notification Shade Window Diff | LOW |
| `GLOBAL_QUICK_SETTINGS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)` | Accessibility Service | LOW | None | Quick Settings Window Diff | LOW |
| `GLOBAL_LOCK_SCREEN` | Navigation | API 28 | `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | Accessibility Service | MEDIUM | Device Admin Lock | Display Power State Off | LOW |
| `GLOBAL_TAKE_SCREENSHOT` | Navigation | API 28 | `performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)` | Accessibility / MediaProjection | LOW | MediaProjection Session | Image File Generated & Saved | MEDIUM |
| `UI_CLICK` | UI Control | API 27 | Live node + `findClickableAncestor()` + `ACTION_CLICK` | Accessibility Service | LOW | Gesture Dispatch (`dispatchGesture`) | Click Verification Strategy (Text/Window/Checked Diff) | LOW |
| `UI_LONG_CLICK` | UI Control | API 27 | Live node + `findLongClickableAncestor()` + `ACTION_LONG_CLICK` | Accessibility Service | LOW | Long Press Gesture | ContextMenu / Selection / Window Diff | LOW |
| `UI_TEXT_INPUT` | UI Control | API 27 | Live node + `findEditableTarget()` + `ACTION_SET_TEXT` | Accessibility Service | MEDIUM | Focus + Key Events | Target Field Text == Input String | LOW |
| `UI_SCROLL_FORWARD` | UI Control | API 27 | Live node + `findScrollableAncestor()` + `ACTION_SCROLL_FORWARD` | Accessibility Service | LOW | Swipe Gesture Up | Scroll Container Position & Node Shift Diff | LOW |
| `UI_SCROLL_BACKWARD` | UI Control | API 27 | Live node + `findScrollableAncestor()` + `ACTION_SCROLL_BACKWARD` | Accessibility Service | LOW | Swipe Gesture Down | Scroll Container Position & Node Shift Diff | LOW |
| `APP_LAUNCH` | System | API 27 | `PackageManager.getLaunchIntentForPackage()` + `startActivity()` | None (`<queries>` on API 30+) | LOW | Accessibility UI Click on Launcher Icon | Foreground Package == Target Package | LOW |
| `OVERLAY_SHOW` | UI Surface | API 27 | `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` | `SYSTEM_ALERT_WINDOW` | LOW | Console UI Only | Window Attached & Visible | LOW |
| `HARDWARE_VOLUME` | Hardware | API 27 | `AudioManager.setStreamVolume()` | None | LOW | None | `AudioManager.getStreamVolume()` Diff | LOW |
| `HARDWARE_BRIGHTNESS` | Hardware | API 27 | `Settings.System.putInt(SCREEN_BRIGHTNESS)` | `WRITE_SETTINGS` | MEDIUM | None | `Settings.System.getInt()` Diff | LOW |
| `HARDWARE_WIFI_TOGGLE` | Hardware | API 27 | Direct API (API 27–28) / UI Automation (API 29+) | `CHANGE_WIFI_STATE` / A11y | MEDIUM | Settings Panel UI Flow | Wi-Fi State Broadcast / Panel Diff | LOW |
| `SPEECH_STT` | Voice | API 27 | `SpeechRecognizer.createSpeechRecognizer()` | `RECORD_AUDIO` | LOW | Manual Text Input | Non-empty Transcript Returned | MEDIUM |
| `SPEECH_TTS` | Voice | API 27 | `TextToSpeech` engine synthesis | None | LOW | Silent Execution | Utterance Callback `onDone()` | LOW |
| `STRUCTURED_PROBLEM_SOLVER`| Solver | API 27 | Grid/board observation + deterministic solver (Sudoku) | Accessibility Service | LOW | None | Grid State Verification | MEDIUM |
| `TRIP_RESEARCH_ENGINE` | Research | API 27 | Web research + travel fact extraction | None | LOW | Manual Search | Research Report Generated with Citations | MEDIUM |
| `EXTERNAL_KNOWLEDGE_IMPORT`| Research | API 27 | Parser for ChatGPT/Gemini exports | SAF Document Picker | LOW | Text Copy/Paste | Provenance Hash & Stored Entry in DB | LOW |
