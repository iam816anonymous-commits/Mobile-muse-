# RISKS_AND_LIMITATIONS.md — Architectural Risks & Platform Limitations

## 1. Overview & Security Risk Posture

Operating an automated device agent on Android involves strict platform security boundaries, background execution restrictions, API deprecations, low-RAM hardware constraints, and potential adversarial indirect prompt injection risks. This document details known risks, platform limitations across API levels 27 through 36+, and architectural mitigations.

---

## 2. Android Security & Security Model Boundaries

### 2.1 Special Access Permission Boundaries
Unlike standard runtime permissions, special app access permissions cannot be granted programmatically via dialogs.
- **Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`):** Requires manual user activation in system settings.
- **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`):** Requires explicit user toggle in Android Settings.
- **Write System Settings (`WRITE_SETTINGS`):** Required to modify system brightness or timeout.
- **Usage Access (`PACKAGE_USAGE_STATS`):** Required to query foreground app usage history.
- **Notification Access (`NotificationListenerService`):** System special access. Android Q (API 29) and below can prevent notification listeners from obtaining access on low-RAM devices.

### 2.2 Privileged Operations (Explicitly Unsupported / Restricted)
The agent **must not fake execution** for actions that Android restricts to root or Device Owner/System apps:
- Programmatic silent app installation/uninstallation.
- Direct hardware power off / reboot without user confirmation dialogs.
- Toggling Airplane Mode programmatically (restricted since API 17).
- Programmatically granting runtime permissions to other apps without user interaction.
- Intercepting secure payment input fields or reading password fields marked with `TYPE_TEXT_VARIATION_PASSWORD`.

---

## 3. Platform Limitations Across Android API Levels (API 27–36+)

| API Level | Android Version | Platform Limitation / Behavior Change | LocalAgent Mitigation / Adapter |
|---|---|---|---|
| **API 27** | Android 8.1 | Baseline target; Low RAM Killer aggressive; Notification Listener restricted on low-RAM devices; `SpeechRecognizer` on-device speech models unavailable offline. | Default baseline; event-driven lifecycle; fallback to system online STT or manual speech input. |
| **API 28** | Android 9.0 | Foreground service restrictions; non-SDK interface restrictions; background process limits. | Explicit `FOREGROUND_SERVICE` permission declaration; strict usage of public Android SDK APIs only. |
| **API 29** | Android 10 | Background activity start restrictions; Scoped Storage introduced; location/background access tightened; Wi-Fi toggle blocked directly. | Overlay launches activities via pending intents; Wi-Fi control switches to UI Automation flow (`Settings.Panel.ACTION_WIFI`). |
| **API 30** | Android 11 | Package visibility restrictions (`<queries>` tag required in manifest to discover installed apps); ONE-TIME permissions. | Declare explicit `<queries>` intents in `AndroidManifest.xml` for `AppResolver`. |
| **API 31** | Android 12 | On-device `SpeechRecognizer` API supported; exact alarm restrictions; foreground service launch restrictions. | Use `SpeechRecognizer.isOnDeviceRecognitionAvailable()` runtime checks. |
| **API 33** | Android 13 | Notification permission (`POST_NOTIFICATIONS`) required at runtime; Bluetooth toggle restricted. | Runtime permission check for `POST_NOTIFICATIONS`; Bluetooth control switches to System Intent. |
| **API 34** | Android 14 | Foreground service type requirements (`foregroundServiceType="specialUse"`). | Declare explicit foreground service types in manifest. |
| **API 35+** | Android 15+ | Edge-to-edge layout enforcement; strictest background execution and accessibility service security checks. | Window insets handling; strict verification of accessibility event sources. |

---

## 4. Architectural & Operational Risks

### 4.1 False-Positive Action Success ("Dispatched vs Verified")
- **Risk:** `AccessibilityNodeInfo.performAction(ACTION_CLICK)` returns `true`, but the app UI fails to respond.
- **Mitigation:** Action-specific `VerificationStrategy`. Every dispatch is followed by post-action observation snapshot diffing tailored to the action type.

### 4.2 Accessibility Service Disconnection & Passive Degradation
- **Risk:** Android OS kills `AgentAccessibilityService` under heavy RAM pressure.
- **Mitigation:**
  1. Passive degradation: If Accessibility is unbound, non-accessibility subsystems (Console, Movable Overlay, Settings UI, EventLogger, Voice STT) remain 100% operational.
  2. Actions requiring Accessibility return `ACCESSIBILITY_UNAVAILABLE` safely.
  3. Re-bind monitor guides user to restore accessibility service.

### 4.3 Indirect Prompt Injection via On-Screen Text & Web Pages
- **Risk:** Malicious third-party apps or websites display text attempting to hijack an AI planner.
- **Mitigation:**
  1. **Strict Data/Instruction Boundary:** External screen text and web pages are tagged as **UNTRUSTED DATA**.
  2. **Action Policy Engine:** High-risk actions (modifying settings, deleting files, purchasing) require explicit interactive user confirmation.

### 4.4 Low-RAM Process Termination & Recovery
- **Risk:** Low Memory Killer (LMK) terminates app process on 1 GB RAM devices.
- **Mitigation:**
  1. Synchronous commits to unified SQLite `agent.db` ensure zero state loss.
  2. Process recovery protocol resumes interrupted tasks after restart.
