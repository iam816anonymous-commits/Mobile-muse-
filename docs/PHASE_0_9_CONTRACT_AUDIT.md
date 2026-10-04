# docs/PHASE_0_9_CONTRACT_AUDIT.md — Phase 0.9 Final Contract Audit & Freeze Gate Deliverables

## 1. Executive Summary

This document presents the formal **Phase 0.9 Final Contract Audit & Freeze Gate Deliverable** for **LocalAgent**.

Every contract, verification algorithm, API baseline parameter, permission tier, persistence budget, testing level, and state machine in the Phase 0.9 specification package has been subjected to a final targeted audit to eliminate all false-success paths, technical ambiguities, and cross-module contradictions.

### Final Freeze Gate Status: **READY_FOR_FREEZE**

The Phase 0.9 specification package is **OFFICIALLY FROZEN** as the immutable implementation contract for Phase 1 through Phase 22.

---

## 2. Contradiction Resolution Matrix

| Audit Area | Initial Finding / Contradiction | Final Corrected Contract | Document Location |
|---|---|---|---|
| **Action Verification** | Generic "UI change occurred" or node count shift triggered `SUCCESS_VERIFIED`. | Generic UI mutations strictly prohibited. Target-aware strategies (`TargetClickStrategy`, `ToggleVerificationStrategy`, `TextInputStrategy`, `ScrollVerificationStrategy`, `NavigationAwareBackStrategy`) require target-specific mutations or explicitly declared destination states. | `ACTION_CONTRACTS.md` |
| **Node Identity** | Snapshot instance IDs were confused with cross-snapshot node identity. | `observationInstanceId` and `parentInstanceId` are snapshot-local ONLY. Cross-snapshot identity uses composite `nodeIdentity` keys rated by `NodeIdentityConfidence` (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`). | `OBSERVATION_MODEL.md` |
| **Observation Lifecycle** | Node extraction made multiple calls to `rootInActiveWindow`. | Single-root capture protocol: `rootInActiveWindow` called **EXACTLY ONCE** per snapshot cycle. Native node references recycled deterministically in `finally` blocks. | `OBSERVATION_MODEL.md` |
| **Storage & Persistence** | Multiple separate SQLite DB files proposed without WAL file size accounting. | All persistent tables (`agent_events`, `episodes`, `workflows`, `semantic_data`, `knowledge`) consolidated into single `agent.db` SQLite database. Storage limits strictly budget for `agent.db` + `agent.db-wal` + `agent.db-shm` under a 30 MB maximum cap. | `LOGGING_AND_AUDIT.md` & `LOW_RAM_DESIGN.md` |
| **SDK & API Strategy** | `minSdk 27` baseline was conflated with `targetSdk` strategy. | Explicitly separated `minSdk = 27` (hard minimum runtime baseline), `compileSdk` (latest modern SDK), and `targetSdk` (Google Play requirements). `CapabilityRule` data model incorporates `targetSdkConstraints`. | `CAPABILITY_MATRIX.md` & `ARCHITECTURE.md` |
| **Permission Security** | Broad `READ_EXTERNAL_STORAGE` required for document import; `<queries>` treated as runtime permission. | Document import uses Storage Access Framework (SAF) `Intent.ACTION_OPEN_DOCUMENT` (`content://` URIs). `<queries>` configured as Manifest Configuration. Special access permissions use Settings intent flows. | `PERMISSION_MATRIX.md` |
| **Concurrency Control** | Concurrent device manipulation possible across surfaces. | `ExecutionLock` policy enforced in `GoalDispatcher`. Priority queue manages incoming commands; only ONE foreground device-control transaction manipulates Accessibility channel at a time. | `MEMORY_AND_LEARNING.md` |
| **Testing Pyramid** | Unit test passes conflated with real device execution. | 3-Level Testing Pyramid established (LEVEL 1: Unit/Contract, LEVEL 2: Device Self Test via `TestCenter`, LEVEL 3: Cross-App E2E). Explicit rule: `UNIT PASS != CONTRACT PASS != REAL DEVICE PASS`. | `TESTING_STRATEGY.md` |
| **Phase Plan** | Duplicate or competing phase definitions. | Reconciled into exactly ONE authoritative 23-phase dependency chain (Phase 0 through Phase 22) across all documents. | `PHASE_PLAN.md` |

---

## 3. Capability-to-Test Matrix

| Capability ID | LEVEL 1: Unit / Contract Test | LEVEL 2: Device Self Test | LEVEL 3: Cross-App E2E Test |
|---|---|---|---|
| `GLOBAL_BACK` | `NavigationAwareBackStrategyTest` | `A11yServiceGlobalBackTest` | `SettingsSubpageBackE2ETest` |
| `GLOBAL_HOME` | `HomeVerificationStrategyTest` | `A11yServiceGlobalHomeTest` | `HomeLauncherReturnE2ETest` |
| `GLOBAL_RECENTS` | `RecentsVerificationStrategyTest` | `A11yServiceGlobalRecentsTest` | `RecentsWindowDiffE2ETest` |
| `UI_CLICK` | `TargetClickStrategyTest` | `TargetResolverAncestorTest` | `CalculatorDigit7ClickE2ETest` |
| `UI_LONG_CLICK` | `FlexibleLongClickStrategyTest` | `LongClickableAncestorTest` | `TextContextMenuLongClickE2ETest` |
| `UI_TEXT_INPUT` | `TextInputStrategyTest` | `EditableTargetResolverTest` | `SearchInputE2ETest` |
| `UI_SCROLL_FORWARD` | `ScrollVerificationStrategyTest` | `ScrollContainerDiffTest` | `SettingsListScrollE2ETest` |
| `APP_LAUNCH` | `AppResolverTest` | `AppLauncherIntentTest` | `LaunchSettingsAndVerifyE2ETest` |
| `OVERLAY_SHOW` | `OverlayCommandMappingTest` | `WindowManagerOverlayRenderTest` | `OverlayParityE2ETest` |
| `HARDWARE_VOLUME` | `HardwareRiskPolicyTest` | `AudioManagerVolumeAdapterTest` | `VolumeChangeE2ETest` |
| `HARDWARE_BRIGHTNESS` | `WriteSettingsPermissionTest` | `BrightnessSystemAdapterTest` | `BrightnessChangeE2ETest` |
| `SPEECH_STT` | `MultilingualSyntaxNormalizerTest` | `SpeechRecognizerProviderTest` | `SpokenCommandExecutionE2ETest` |
| `STRUCTURED_PROBLEM_SOLVER`| `SudokuSolverAlgorithmTest` | `GridStateExtractionTest` | `SudokuBoardSolveE2ETest` |
| `TRIP_RESEARCH_ENGINE` | `GoalDecompositionTest` | `ResearchPlannerTest` | `TripResearchExecutionE2ETest` |

---

## 4. Permission & API Matrix

| Identifier | Security Classification | Min API | Manifest / Special Access | Direct API vs Fallback | Passive Degradation Behavior |
|---|---|---|---|---|---|
| `INTERNET` | Normal Manifest Permission | API 27 | `android.permission.INTERNET` | Direct API | Network research disabled; local storage/STT operate |
| `<queries>` | Manifest Configuration | API 30+ | `<queries>` in Manifest | Manifest Config | App discovery restricted to pre-declared packages |
| `Accessibility Service` | Special App Access | API 27 | `BIND_ACCESSIBILITY_SERVICE` | Direct API / Gesture Fallback | A11y actions return `ACCESSIBILITY_UNAVAILABLE`; non-A11y UI operates |
| `System Overlay` | Special App Access | API 27 | `SYSTEM_ALERT_WINDOW` | Direct API | Overlay surface hidden; Console UI remains active |
| `Write System Settings` | Special App Access | API 27 | `WRITE_SETTINGS` | Direct API | Brightness/timeout commands return `PERMISSION_REQUIRED` |
| `Usage Access` | Special App Access | API 27 | `PACKAGE_USAGE_STATS` | Direct API | Foreground package uses passive Accessibility events |
| `Notification Listener` | Special App Access | API 27 | `BIND_NOTIFICATION_LISTENER_SERVICE` | Direct API | Notification triggers disabled |
| `Microphone / Audio` | Runtime Permission | API 27 | `RECORD_AUDIO` | Direct API | Speech input disabled; keyboard input active |
| `Document Import (SAF)` | User-Consent Session | API 27 | `Intent.ACTION_OPEN_DOCUMENT` | SAF Stream Reader | SAF document picker restricted; broad storage permission NOT required |
| `MediaProjection` | User-Consent Session | API 27 | `MediaProjection` Prompt | Session Capture | Screenshots return `PERMISSION_DENIED` |

---

## 5. Universal Action Contract Table

| Action Type | Target Selector Requirement | Ancestor Traversal | Executor Adapter | Verification Strategy | Allowed Result Codes |
|---|---|---|---|---|---|
| `UI_CLICK` | `NodeIdentity` (EXACT / HIGH / MEDIUM) | `findClickableAncestor()` | `AccessibilityActionAdapter` | `TargetClickStrategy` | `SUCCESS_VERIFIED`, `DISPATCHED_BUT_NOT_VERIFIED`, `ACTION_FAILED`, `TARGET_NOT_FOUND` |
| `UI_LONG_CLICK` | `NodeIdentity` (EXACT / HIGH) | `findLongClickableAncestor()` | `AccessibilityActionAdapter` | `FlexibleLongClickStrategy` | `SUCCESS_VERIFIED`, `DISPATCHED_BUT_NOT_VERIFIED`, `ACTION_FAILED`, `TARGET_NOT_FOUND` |
| `UI_TEXT_INPUT` | `NodeIdentity` (Editable Target) | `findEditableTarget()` | `AccessibilityActionAdapter` | `TextInputStrategy` | `SUCCESS_VERIFIED`, `DISPATCHED_BUT_NOT_VERIFIED`, `TARGET_NOT_FOUND` |
| `UI_SCROLL_FORWARD` | Target Scroll Container | `findScrollableAncestor()` | `AccessibilityActionAdapter` | `ScrollVerificationStrategy` | `SUCCESS_VERIFIED`, `DISPATCHED_BUT_NOT_VERIFIED`, `NO_SCROLL_POSSIBLE`, `ACTION_FAILED` |
| `UI_SCROLL_BACKWARD` | Target Scroll Container | `findScrollableAncestor()` | `AccessibilityActionAdapter` | `ScrollVerificationStrategy` | `SUCCESS_VERIFIED`, `DISPATCHED_BUT_NOT_VERIFIED`, `NO_SCROLL_POSSIBLE`, `ACTION_FAILED` |
| `GLOBAL_BACK` | None | None | `GlobalActionAdapter` | `NavigationAwareBackStrategy` | `SUCCESS_VERIFIED`, `DISPATCHED_BUT_NOT_VERIFIED`, `ACCESSIBILITY_UNAVAILABLE` |
| `GLOBAL_HOME` | None | None | `GlobalActionAdapter` | Launcher Package Match | `SUCCESS_VERIFIED`, `ACCESSIBILITY_UNAVAILABLE` |
| `GLOBAL_RECENTS` | None | None | `GlobalActionAdapter` | Recents Window State Diff | `SUCCESS_VERIFIED`, `ACCESSIBILITY_UNAVAILABLE` |
| `APP_LAUNCH` | App Label / Package Name | None | `AppLauncher` | Foreground Package Match | `SUCCESS_VERIFIED`, `TARGET_NOT_FOUND`, `TIMEOUT` |

---

## 6. Verification Strategy Matrix

| Strategy Class | Target Inputs | Primary Verification Criteria | Unsafe generic fallbacks PROHIBITED |
|---|---|---|---|
| `TargetClickStrategy` | Target Identity, Expected View ID, Expected Package/Window | Target text/checked state mutated OR expected destination reached | Prohibits arbitrary node additions or background UI changes |
| `ToggleVerificationStrategy` | Target Identity, Expected Checked State | Target `isChecked` matches expected boolean | Prohibits unchecked toggle reporting |
| `TextInputStrategy` | Target Field Identity, Expected String | Target field text matches expected string exactly | Prohibits verifying against unrelated editable/focused fields |
| `ScrollVerificationStrategy` | Container Identity, Direction | Container descendant nodes shifted or newly visible descendants appeared; boundary returns `NO_SCROLL_POSSIBLE` | Prohibits global bounds changes or unrelated node additions |
| `NavigationAwareBackStrategy` | Expected Previous Package/Window | Transition to previous activity/window, package, dismissed layer/popup, or launcher | Prohibits total node count shifts alone |

---

## 7. Logging Event Taxonomy

| Event Subsystem | Event Type | Trigger Condition | Mandatory Metadata Fields |
|---|---|---|---|
| `COMMAND` | `COMMAND_RECEIVED` | Input received from surface (Console, Overlay, Voice, AI) | `rawInput`, `sourceChannel`, `normalizedCommand` |
| `COMMAND` | `POLICY_EVALUATED` | Action risk evaluated by `ActionPolicyEngine` | `capabilityId`, `riskLevel`, `isPolicyApproved` |
| `OBSERVATION` | `SNAPSHOT_CAPTURED` | Single-root snapshot generated | `packageName`, `windowId`, `totalNodeCount`, `durationMs` |
| `OBSERVATION` | `TARGET_RESOLVED` | Target node matched in live tree | `targetIdentityKey`, `confidence`, `traversedAncestorClass` |
| `ACTION` | `ACTION_DISPATCHED` | Android API `performAction()` executed | `actionType`, `targetIdentityKey`, `performActionResult` |
| `ACTION` | `VERIFICATION_COMPLETED` | `VerificationStrategy` evaluation completed | `verificationCode`, `resultStatus`, `diffSummary` |
| `PERMISSION` | `PERMISSION_STATUS_CHANGED` | Permission or special access status updated | `permissionName`, `isGranted`, `degradationState` |
| `SYSTEM` | `LMK_INTERRUPTION_DETECTED` | App restarted after Low Memory Killer termination | `interruptedGoalId`, `recoveredTaskId`, `restoredState` |

---

## 8. Low-RAM Resource Budget Matrix

| Agent State | TARGET Heap Footprint | WARNING Threshold | CRITICAL Threshold | Resource Lifecycle Action on Threshold |
|---|---|---|---|---|
| **IDLE** | **< 15 MB** | **15 MB – 25 MB** | **> 25 MB** | Flush snapshot diff caches; clear transient command logs |
| **EXECUTING** | **< 35 MB** | **35 MB – 50 MB** | **> 50 MB** | Drop diff history; force immediate node primitive recycling |
| **VOICE_ACTIVE** | **< 45 MB** | **45 MB – 60 MB** | **> 60 MB** | Destroy `SpeechRecognizer` immediately; shutdown TTS |
| **LOW_MEMORY_DEGRADED** | **< 10 MB** | **10 MB – 15 MB** | **> 15 MB** | Clear all in-memory caches; commit `agent.db` WAL |

---

## 9. Final Freeze Gate Declaration

All 23 specification checklist criteria are satisfied. No false-success paths, ambiguities, or cross-document contradictions remain.

### Declaration: **READY_FOR_FREEZE**

Phase 0.9 architecture is officially **FROZEN**. Implementation Phase 1 is authorized to begin.
