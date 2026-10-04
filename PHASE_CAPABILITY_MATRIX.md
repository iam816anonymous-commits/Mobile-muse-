# PHASE_CAPABILITY_MATRIX.md — Phase-Gated Capability Matrix

## 1. Executive Summary & Core Rule

Every capability introduced by **LocalAgent** belongs to a specific phase and must be **fully testable on a physical Android device** within that same phase.

### Core Architectural Mandates
1. **Phase-Gated Capabilities:** Capabilities, required permissions, and special access MUST NOT be requested prematurely in earlier phases.
2. **No Dormant Permission Bloat:** Application startup requests ONLY permissions required by capabilities active in the current phase.
3. **Explicit Capability Lifecycle:** Capabilities transition through explicit lifecycle states (`DECLARED`, `AVAILABLE`, `UNAVAILABLE`, `PERMISSION_REQUIRED`, `USER_REQUESTED`, `GRANTED`, `DENIED`, `READY`, `BLOCKED`).

---

## 2. 23-Phase Capability & Permission Matrix

| Phase | Capability ID | Category | Required Permissions | Special Access / Service | Hardware Required | Min API | Phase Testability |
|---|---|---|---|---|---|---|---|
| **Phase 0** | `PLANNING_SPEC` | System | None | None | None | 27 | Specification Audit |
| **Phase 1** | `CORE_COMMAND_NORMALIZER` | System | None | None | CPU / JVM | 27 | Unit Tests |
| **Phase 1** | `GOAL_DISPATCHER` | System | None | None | CPU / JVM | 27 | Unit Tests |
| **Phase 2** | `EVENT_LOGGING` | System | None | None | Internal Storage | 27 | Unit Tests & App-Private DB |
| **Phase 2** | `DURABLE_STORAGE` | Storage | `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` | SAF Document Tree | Shared Storage | 27 | Physical Device & Uninstall Survival |
| **Phase 3** | `PERMISSION_CENTER` | System | Dynamic Runtime Checks | Settings Intent Handler | Device Display | 27 | Physical Device Permission Flow |
| **Phase 4** | `ACCESSIBILITY_SERVICE` | System | None | `AccessibilityService` Binding | Device Display | 27 | Physical Device Service Binding |
| **Phase 5** | `UI_OBSERVE` | UI Control | None | `AccessibilityService` | Display | 27 | Physical Device Snapshot Capture |
| **Phase 6** | `TARGET_RESOLVER` | UI Control | None | `AccessibilityService` | Display | 27 | Physical Device Ancestor Resolution |
| **Phase 7** | `UI_CLICK` | UI Control | None | `AccessibilityService` | Touch Screen | 27 | Physical Device Click Verification |
| **Phase 7** | `GLOBAL_BACK` | Navigation | None | `AccessibilityService` | Display | 27 | Physical Device Navigation Diff |
| **Phase 7** | `GLOBAL_HOME` | Navigation | None | `AccessibilityService` | Display | 27 | Physical Device Home Launch |
| **Phase 7** | `GLOBAL_RECENTS` | Navigation | None | `AccessibilityService` | Display | 27 | Physical Device Recents Window Diff |
| **Phase 8** | `APP_LAUNCH` | System | `<queries>` Package Visibility | None | Device Apps | 27 | Physical Device App Launching |
| **Phase 9** | `COMMAND_CONSOLE` | System | None | None | Touch Screen | 27 | Physical Device Console Input |
| **Phase 10** | `AUTOMATED_TEST_CENTER` | System | None | None | CPU / Storage | 27 | In-App Test Suite Execution |
| **Phase 11** | `MOVABLE_OVERLAY` | UI Control | None | `SYSTEM_ALERT_WINDOW` | Overlay Window | 27 | Physical Floating Overlay Drag |
| **Phase 12** | `HARDWARE_VOLUME` | Hardware | None | `AudioManager` | Audio Speaker | 27 | Physical Volume Modification |
| **Phase 12** | `HARDWARE_BRIGHTNESS` | Hardware | `WRITE_SETTINGS` | `Settings.System` | Screen Backlight | 27 | Physical Backlight Adjustment |
| **Phase 12** | `HARDWARE_FLASHLIGHT` | Hardware | `CAMERA` | `CameraManager` | LED Flashlight | 27 | Physical Flashlight Toggle |
| **Phase 13** | `SPEECH_STT` | Voice | `RECORD_AUDIO` | `SpeechRecognizer` | Microphone | 27 | Physical Voice Recognition |
| **Phase 13** | `SPEECH_TTS` | Voice | None | `TextToSpeech` | Speaker | 27 | Physical Voice Synthesis |
| **Phase 14** | `WORKFLOW_EXECUTION` | Automation | None | `AccessibilityService` | CPU / Display | 27 | Physical Multi-Step Workflow |
| **Phase 15** | `EPISODIC_MEMORY` | Memory | None | `DurableMemoryStorageProvider` | Shared Storage | 27 | Physical Re-Validation Workflow |
| **Phase 16** | `BROWSER_RESEARCH` | Research | None | Accessibility / Web Browser | Display / Internet | 27 | Physical Web Scraping & Tagging |
| **Phase 17** | `EXTERNAL_KNOWLEDGE` | Research | None | SAF Document Picker | Internal Storage | 27 | Physical Chat Export Import |
| **Phase 18** | `STRUCTURED_SOLVER` | Solver | None | `AccessibilityService` | Display / Grid | 27 | Physical Sudoku Board Solving |
| **Phase 19** | `TRIP_RESEARCH` | Research | `INTERNET` | Web / API Adapters | Internet / Storage | 27 | Physical Travel Itinerary Plan |
| **Phase 20** | `AI_PLANNING` | AI | `INTERNET` | Pluggable LLM Provider | Internet / CPU | 27 | Physical AI Plan Execution |
| **Phase 21** | `RESOURCE_HARDENING` | System | None | Low Memory Killer (LMK) | 1 GB RAM Hardware | 27 | Physical LMK Stress Kill Resume |
| **Phase 22** | `MASTER_CERTIFICATION`| System | All Granted Permissions | Master Diagnostic Suite | Physical Hardware | 27 | Full Device Certification Suite |
