# CAPABILITY_MATRIX.md — LocalAgent Capability Matrix & API Tier Specifications

## 1. Executive Overview

This matrix defines every supported capability in **LocalAgent**, mapping each capability to its required Android API, required permission, risk level, fallback strategy, verification method, resource cost classification, and API compatibility across **Android 8.1 (API 27) through Android 15+ (API 36+)**.

---

## 2. Capability Rule Contract Structure (`CapabilityRule`)

Every capability in the system is defined by a formal `CapabilityRule` data class used by the `CapabilityRegistry` to evaluate execution eligibility before dispatch:

```kotlin
data class CapabilityRule(
    val capabilityId: String,
    val name: String,
    val minApi: Int,
    val maxApi: Int = Int.MAX_VALUE,
    val targetSdkConstraints: TargetSdkConstraint?,
    val requiredPermissions: List<String>,
    val requiredSpecialAccess: List<SpecialAccessType>,
    val requiredPrivilege: PrivilegeLevel = PrivilegeLevel.NONE, // NONE, USER_CONSENT, DEVICE_ADMIN, ROOT
    val directApiAvailable: Boolean,
    val fallbackStrategy: FallbackStrategyType?,
    val verificationStrategy: VerificationStrategyType,
    val riskLevel: ActionRiskLevel
)

enum class TargetSdkConstraint {
    QUERIES_MANIFEST_REQUIRED, // API 30+ package visibility
    SCOPED_STORAGE_REQUIRED,   // API 29+ storage restrictions
    BACKGROUND_ACTIVITY_RESTRICTED // API 29+ overlay activity launches
}

enum class PrivilegeLevel { NONE, USER_CONSENT, DEVICE_ADMIN, SYSTEM_DEVICE_OWNER, ROOT }
enum class FallbackStrategyType { GESTURE_DISPATCH, SETTINGS_PANEL_UI_FLOW, MEDIA_PROJECTION, SAF_DOCUMENT_PICKER, CONSOLE_UI_ONLY }
enum class VerificationStrategyType { TARGET_CLICK, TEXT_INPUT, SCROLL, TOGGLE, NAVIGATION_BACK, LAUNCHER_HOME, HARDWARE_DIFF }
```

---

## 3. Universal Capability Matrix

| Capability ID | Category | Min API | Android API / Mechanism | Required Permission | Special Access | Risk Level | Direct API Available | Fallback Strategy | Resource Cost |
|---|---|---|---|---|---|---|---|---|---|
| `GLOBAL_BACK` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_BACK)` | Accessibility Service | Accessibility | LOW | Yes | None | LOW |
| `GLOBAL_HOME` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)` | Accessibility Service | Accessibility | LOW | Yes | Home Launcher Intent | LOW |
| `GLOBAL_RECENTS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS)` | Accessibility Service | Accessibility | LOW | Yes | None | LOW |
| `GLOBAL_NOTIFICATIONS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)` | Accessibility Service | Accessibility | LOW | Yes | None | LOW |
| `GLOBAL_QUICK_SETTINGS` | Navigation | API 27 | `AccessibilityService.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)` | Accessibility Service | Accessibility | LOW | Yes | None | LOW |
| `GLOBAL_LOCK_SCREEN` | Navigation | API 28 | `performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)` | Accessibility Service | Accessibility | MEDIUM | Yes | Device Admin Lock | LOW |
| `GLOBAL_TAKE_SCREENSHOT` | Navigation | API 28 | `performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)` | Accessibility / MediaProjection | MediaProjection Session | LOW | Yes | MediaProjection Session | MEDIUM |
| `UI_CLICK` | UI Control | API 27 | Live node + `findClickableAncestor()` + `ACTION_CLICK` | Accessibility Service | Accessibility | LOW | Yes | Gesture Dispatch (`dispatchGesture`) | LOW |
| `UI_LONG_CLICK` | UI Control | API 27 | Live node + `findLongClickableAncestor()` + `ACTION_LONG_CLICK` | Accessibility Service | Accessibility | LOW | Yes | Long Press Gesture | LOW |
| `UI_TEXT_INPUT` | UI Control | API 27 | Live node + `findEditableTarget()` + `ACTION_SET_TEXT` | Accessibility Service | Accessibility | MEDIUM | Yes | Focus + Key Events | LOW |
| `UI_SCROLL_FORWARD` | UI Control | API 27 | Live node + `findScrollableAncestor()` + `ACTION_SCROLL_FORWARD` | Accessibility Service | Accessibility | LOW | Yes | Swipe Gesture Up | LOW |
| `UI_SCROLL_BACKWARD` | UI Control | API 27 | Live node + `findScrollableAncestor()` + `ACTION_SCROLL_BACKWARD` | Accessibility Service | Accessibility | LOW | Yes | Swipe Gesture Down | LOW |
| `APP_LAUNCH` | System | API 27 | `PackageManager.getLaunchIntentForPackage()` + `startActivity()` | None (`<queries>` on API 30+) | None | LOW | Yes | Accessibility UI Click on Launcher Icon | LOW |
| `OVERLAY_SHOW` | UI Surface | API 27 | `WindowManager.addView()` with `TYPE_APPLICATION_OVERLAY` | `SYSTEM_ALERT_WINDOW` | System Overlay | LOW | Yes | Console UI Only | LOW |
| `HARDWARE_VOLUME` | Hardware | API 27 | `AudioManager.setStreamVolume()` | None | None | LOW | Yes | None | LOW |
| `HARDWARE_BRIGHTNESS` | Hardware | API 27 | `Settings.System.putInt(SCREEN_BRIGHTNESS)` | `WRITE_SETTINGS` | Write Settings | MEDIUM | Yes | None | LOW |
| `HARDWARE_WIFI_TOGGLE` | Hardware | API 27 | Direct API (API 27–28) / UI Automation (API 29+) | `CHANGE_WIFI_STATE` / A11y | Accessibility (API 29+) | MEDIUM | No (API 29+) | Settings Panel UI Flow | LOW |
| `SPEECH_STT` | Voice | API 27 | `SpeechRecognizer.createSpeechRecognizer()` | `RECORD_AUDIO` | None | LOW | Yes | Manual Keyboard Input | MEDIUM |
| `SPEECH_TTS` | Voice | API 27 | `TextToSpeech` engine synthesis | None | None | LOW | Yes | Silent Execution | LOW |
| `STRUCTURED_PROBLEM_SOLVER`| Solver | API 27 | Grid/board observation + deterministic solver (Sudoku) | Accessibility Service | Accessibility | LOW | Yes | None | MEDIUM |
| `TRIP_RESEARCH_ENGINE` | Research | API 27 | Web research + travel fact extraction | `INTERNET` | None | LOW | Yes | Manual Search | MEDIUM |
| `EXTERNAL_KNOWLEDGE_IMPORT`| Research | API 27 | Parser for ChatGPT/Gemini exports | SAF Document Picker | None | LOW | Yes | Text Copy/Paste | LOW |
