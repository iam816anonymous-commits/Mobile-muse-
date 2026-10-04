# PERMISSION_MATRIX.md — LocalAgent Permission Tiers & Special Access Matrix

## 1. Executive Summary & Permission Architecture

Android enforces a multi-tiered security model distinguishing standard manifest permissions, runtime permissions, and special app access permissions. **LocalAgent** manages all permissions centrally via `PermissionManager`.

### Permission Tiers
- **Tier 0 (No Permission Required):** In-app UI, local database storage, internal command processing, state machines.
- **Tier 1 (Normal Runtime Permissions):** Requested via standard OS dialogs in context (`RECORD_AUDIO`, `POST_NOTIFICATIONS`, `READ_EXTERNAL_STORAGE`).
- **Tier 2 (Special Access / Settings Flow):** Granted via dedicated Android Settings screens (`Accessibility Service`, `System Overlay`, `Usage Access`, `Write System Settings`, `Notification Listener`).
- **Tier 3 (Privileged / Restricted):** Device Administrator, Root, or ADB-only actions.

---

## 2. Comprehensive Permission Matrix

| Permission Identifier | Tier | Manifest Permission Constant | Feature / Capability Dependent | Granted via | Passive Degradation Behavior if Denied | API Constraints |
|---|---|---|---|---|---|---|
| **Accessibility Service** | Tier 2 | `android.permission.BIND_ACCESSIBILITY_SERVICE` | Global Navigation, UI Target Resolution, Click/Scroll Execution | System Settings → Accessibility → LocalAgent | Console, Movable Overlay, Settings UI, EventLogger, Voice STT, and Research remain 100% operational. A11y actions return `ACCESSIBILITY_UNAVAILABLE`. | API 27+ |
| **System Overlay** | Tier 2 | `android.permission.SYSTEM_ALERT_WINDOW` | Movable Overlay Surface (`TYPE_APPLICATION_OVERLAY`) | System Settings → Display over other apps | Movable Overlay surface disabled; Agent controls operate solely through Console UI Activity. | API 27+ |
| **Microphone / Audio** | Tier 1 | `android.permission.RECORD_AUDIO` | Voice Command Input (`SpeechRecognizer`) | Standard Runtime Dialog | Speech input disabled; Console keyboard text input remains fully available. | API 27+ |
| **Notifications Post** | Tier 1 | `android.permission.POST_NOTIFICATIONS` | Agent Status & Workflow Notifications | Standard Runtime Dialog | Status updates displayed inside Console UI log view only. | API 33+ (No-op on API 27–32) |
| **Write System Settings**| Tier 2 | `android.permission.WRITE_SETTINGS` | Hardware Brightness Control, Screen Timeout Modification | System Settings → Modify system settings | Brightness adjustments fail safely returning `PERMISSION_REQUIRED`. | API 27+ |
| **Usage Access** | Tier 2 | `android.permission.PACKAGE_USAGE_STATS` | Foreground App Statistics, Recent App Usage History | System Settings → Usage Access | Foreground package detection relies strictly on passive Accessibility events. | API 27+ |
| **Notification Listener**| Tier 2 | `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | Reading incoming notification text for workflows | System Settings → Notification Access | Notification triggers disabled. | API 27+ (Restricted on low-RAM API 27 devices) |
| **Media Projection** | Tier 2 | `android.media.projection.MediaProjection` | Screen Capture on API 27 (Screenshot fallback) | Interactive MediaProjection User Consent Prompt | Screenshot capability returns `PERMISSION_DENIED`. | API 27+ |
| **Read External Storage**| Tier 1 | `android.permission.READ_EXTERNAL_STORAGE` | Importing exported ChatGPT/Gemini chat files | Standard Runtime Dialog / SAF Document Picker | Document picker restricted to Storage Access Framework (SAF). | API 27–32 (SAF used on API 33+) |
| **Camera** | Tier 1 | `android.permission.CAMERA` | Flashlight / Torch control on older hardware | Standard Runtime Dialog | Flashlight control returns `CAPABILITY_UNAVAILABLE`. | API 27+ |

---

## 3. Special Access Handling & In-App Permission Center

Because Tier 2 Special Access permissions cannot be requested via standard runtime dialogs, `PermissionManager` implements an in-app **Permission Center UI flow**:

```text
               ┌───────────────────────────────────────────────┐
               │         Permission Center UI Display          │
               │  List: Accessibility, Overlay, Usage, Write   │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │    User Clicks "Enable [Permission]"          │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │   Launch Explicit Settings Intent             │
               │   e.g., Settings.ACTION_ACCESSIBILITY_SETTINGS│
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │   User Toggles Permission in Settings & Returns│
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │   Activity.onResume() Lifecycle Callback      │
               │   Re-check `PermissionManager.checkAll()`     │
               └───────────────────────┬───────────────────────┘
                                       │
                                       ▼
               ┌───────────────────────────────────────────────┐
               │   Update UI Badges & Log Permission Event     │
               └───────────────────────────────────────────────┘
```

---

## 4. Passive Degradation Rules

1. **Accessibility Disconnected:** Non-accessibility components remain 100% operational (Console UI, Voice STT, Movable Overlay surface, Settings UI, EventLogger, Web Research Engine). Commands requiring Accessibility fail safely with `ResultCode.ACCESSIBILITY_UNAVAILABLE`.
2. **Overlay Denied:** Agent execution engine runs at 100% functionality via Console UI, Voice input, or background workflows. Overlay surface simply remains hidden.
3. **Microphone Denied:** Voice STT module gracefully disables speech input; speech button in Console UI prompts for runtime permission or falls back to keyboard typing.
4. **Write Settings Denied:** Hardware brightness commands fail safely returning `ResultCode.PERMISSION_REQUIRED`, logging instructions for enabling `WRITE_SETTINGS`.
