# PERMISSION_MATRIX.md — LocalAgent Permission Tiers & Special Access Matrix

## 1. Executive Summary & Security Architecture

Android enforces a multi-tiered security model distinguishing standard manifest permissions, runtime permissions, special app access permissions, user-consent sessions, and privileged administration. **LocalAgent** manages all permissions centrally via `PermissionManager`.

### Standardized Security Classifications
1. **NORMAL MANIFEST PERMISSION:** Granted automatically at install time (`android.permission.INTERNET`, `VIBRATE`, `CHANGE_WIFI_STATE`).
2. **RUNTIME PERMISSION:** Requested via standard OS dialogs at runtime (`RECORD_AUDIO`, `POST_NOTIFICATIONS`, `CAMERA`).
3. **SPECIAL APP ACCESS:** User-granted via dedicated Android Settings screens (`BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`, `WRITE_SETTINGS`, `PACKAGE_USAGE_STATS`, `BIND_NOTIFICATION_LISTENER_SERVICE`).
4. **USER-CONSENT SESSION:** Interactive user confirmation per session (`MediaProjection` screen capture).
5. **MANIFEST CONFIGURATION:** XML declarations (`<queries>` tag for API 30+ package visibility). NOT a permission.
6. **PRIVILEGED / DEVICE-OWNER CAPABILITY:** Restricted to enterprise provisioning or Device Owner. Legacy Device Admin is supported for API 27 screen lock fallback ONLY.

---

## 2. Comprehensive Permission & Security Matrix

| Identifier | Security Classification | Manifest Constant / Config | Capability Dependent | Granted via | Passive Degradation Behavior if Denied | API Constraints |
|---|---|---|---|---|---|---|
| **Internet Access** | Normal Manifest | `android.permission.INTERNET` | Trip Research Engine, Web Research, Online Speech Recognition | Automatic at Install | Network research disabled; local database & offline STT operate normally | API 27+ |
| **Package Visibility** | Manifest Configuration | `<queries>` tag in `AndroidManifest.xml` | App Discovery & `AppResolver` | Manifest Declaration | Unable to discover third-party apps; target package names must be typed manually | API 30+ (No-op on API 27–29) |
| **Accessibility Service** | Special App Access | `android.permission.BIND_ACCESSIBILITY_SERVICE` | Global Navigation, UI Target Resolution, Click/Scroll Execution | System Settings → Accessibility → LocalAgent | Console, Overlay surface, Settings UI, EventLogger, Voice STT, and Research remain 100% operational. A11y actions return `ACCESSIBILITY_UNAVAILABLE`. | API 27+ |
| **System Overlay** | Special App Access | `android.permission.SYSTEM_ALERT_WINDOW` | Movable Overlay Surface (`TYPE_APPLICATION_OVERLAY`) | System Settings → Display over other apps | Movable Overlay surface disabled; Agent controls operate solely through Console UI Activity. | API 27+ |
| **Write System Settings**| Special App Access | `android.permission.WRITE_SETTINGS` | Hardware Brightness Control, Screen Timeout Modification | System Settings → Modify system settings | Brightness adjustments fail safely returning `PERMISSION_REQUIRED`. | API 27+ |
| **Usage Access** | Special App Access | `android.permission.PACKAGE_USAGE_STATS` | Foreground App Statistics, Recent App Usage History | System Settings → Usage Access | Foreground package detection relies strictly on passive Accessibility events. | API 27+ |
| **Notification Listener**| Special App Access | `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` | Reading incoming notification text for workflows | System Settings → Notification Access | Notification triggers disabled. Restricted on low-RAM API 27 devices. | API 27+ |
| **Microphone / Audio** | Runtime Permission | `android.permission.RECORD_AUDIO` | Voice Command Input (`SpeechRecognizer`) | Standard OS Runtime Dialog | Speech input disabled; Console keyboard text input remains fully available. | API 27+ |
| **Notifications Post** | Runtime Permission | `android.permission.POST_NOTIFICATIONS` | Agent Status & Workflow Notifications | Standard OS Runtime Dialog | Status updates displayed inside Console UI log view only. | API 33+ (No-op on API 27–32) |
| **Camera / Torch** | Runtime Permission | `android.permission.CAMERA` | Flashlight / Torch control on API 27 baseline | Standard OS Runtime Dialog | Flashlight control returns `CAPABILITY_UNAVAILABLE`. | API 27+ |
| **Document Import** | User-Consent / SAF | `Intent.ACTION_OPEN_DOCUMENT` (Storage Access Framework) | Importing exported ChatGPT/Gemini chat files or documents | System File Picker Dialog (`content://` URI) | Document import disabled; copy/paste text input remains available. Broad `READ_EXTERNAL_STORAGE` NOT required. | API 27+ |
| **Media Projection** | User-Consent Session | `android.media.projection.MediaProjection` | Screen Capture on API 27 baseline | Interactive System Prompt | Screenshot capability returns `PERMISSION_DENIED`. | API 27+ |
| **Device Administrator**| Privileged (Legacy) | `android.app.admin.DeviceAdminReceiver` | Programmatic Lock Screen on API 27 baseline ONLY | System Settings → Device Admin Apps | Programmatic lock disabled on API 27; uses `GLOBAL_ACTION_LOCK_SCREEN` on API 28+. | API 27 baseline |

---

## 3. Storage Access & Document Import Protocol

LocalAgent **does NOT require broad `READ_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE` permissions** for importing external chat exports or user files.

### Storage Access Framework (SAF) URI Protocol
1. User clicks "Import ChatGPT/Gemini Export" in Settings or Research UI.
2. App launches SAF Document Picker:
   ```kotlin
   val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
       addCategory(Intent.CATEGORY_OPENABLE)
       type = "*/*"
       putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain"))
   }
   startActivityForResult(intent, REQUEST_CODE_IMPORT_DOCUMENT)
   ```
3. User selects file in system picker. App receives temporary `content://` URI permission.
4. `ChatExportParser` reads document content directly from stream via `contentResolver.openInputStream(uri)`.
5. Content is sanitized, assigned a provenance hash, and stored in `agent.db`. Temporary URI permission released.

---

## 4. Passive Degradation Rules

1. **Accessibility Disconnected:** Non-accessibility components remain 100% operational (Console UI, Voice STT, Movable Overlay surface, Settings UI, EventLogger, Web Research Engine, Solvers). Commands requiring Accessibility fail safely with `ResultCode.ACCESSIBILITY_UNAVAILABLE`.
2. **Overlay Denied:** Agent execution engine runs at 100% functionality via Console UI, Voice input, or background workflows. Overlay surface simply remains hidden.
3. **Microphone Denied:** Voice STT module gracefully disables speech input; speech button in Console UI prompts for runtime permission or falls back to keyboard typing.
4. **Write Settings Denied:** Hardware brightness commands fail safely returning `ResultCode.PERMISSION_REQUIRED`, logging instructions for enabling `WRITE_SETTINGS`.
