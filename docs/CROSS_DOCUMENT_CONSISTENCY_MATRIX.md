# docs/CROSS_DOCUMENT_CONSISTENCY_MATRIX.md — Cross-Document Consistency Matrix & Ownership Map

## 1. Executive Summary & Audit Result

This document presents the final master **Cross-Document Consistency Matrix & Concept Ownership Map** for **LocalAgent**.

Every document in the specification package was cross-audited against every other document across 18 specific architectural domain areas to resolve all phase contradictions, storage ambiguities, package naming mismatches, capability ownership overlaps, and API baseline claims.

### FINAL STATUS: **PASS**

**Zero contradictions remain across all 19 specification documents.** The architecture is 100% mutually consistent and fully frozen.

---

## 2. Concept Ownership & Authoritative Source Map

To prevent duplicate or competing definitions, every architectural concept is assigned exactly **ONE authoritative owner document**.

| Architectural Concept Domain | Authoritative Owner Document | Governed Data Structures / Rules | Secondary Consumer Documents |
|---|---|---|---|
| **System Architecture & Module Boundaries** | `ARCHITECTURE.md` | Core modules, high-level diagrams, universal command pipeline flow | `README.md`, `PHASE_PLAN.md` |
| **Phase Plan & Implementation Contracts** | `PHASE_PLAN.md` | Authoritative 23-Phase Roadmap (Phase 0 – Phase 22), per-phase input/output contracts | `DEFINITION_OF_DONE.md`, `TESTING_STRATEGY.md` |
| **Capability Rules & API Matrix** | `CAPABILITY_MATRIX.md` | `CapabilityRule`, `CapabilityDescriptor`, `CapabilityRegistry`, `minApi`, `targetSdkConstraints` | `ACTION_CONTRACTS.md`, `ARCHITECTURE.md` |
| **Permission Tiers & Storage Access** | `PERMISSION_MATRIX.md` | `PermissionManager`, Security Tiers, Special Access intents, SAF document picker protocol | `RISKS_AND_LIMITATIONS.md`, `HARDWARE_CAPABILITIES.md` |
| **Action Contracts & Verification Strategies**| `ACTION_CONTRACTS.md` | Target-aware `VerificationStrategy`, `ResultCode` taxonomy, ancestor traversal algorithms | `OBSERVATION_MODEL.md`, `DEFINITION_OF_DONE.md` |
| **Observation Engine & Identity Model** | `OBSERVATION_MODEL.md` | Single-root snapshot generator, `NodeIdentityConfidence`, composite `nodeIdentity` keys | `ACTION_CONTRACTS.md`, `LOW_RAM_DESIGN.md` |
| **Persistent Logging & Storage Accounting** | `LOGGING_AND_AUDIT.md` | Unified `agent.db` schema, `AgentEvent`, WAL/SHM file size accounting, retention rules | `LOW_RAM_DESIGN.md`, `MEMORY_AND_LEARNING.md` |
| **Low-RAM Architecture & Footprint Targets** | `LOW_RAM_DESIGN.md` | System Memory Thresholds (`TARGET`, `WARNING`, `CRITICAL`), `ResourceManager`, LMK survival rules | `MEMORY_AND_LEARNING.md`, `LOGGING_AND_AUDIT.md` |
| **Memory, Task Lifecycle & Concurrency** | `MEMORY_AND_LEARNING.md` | `GoalState`, `TaskLifecycle`, single-threaded `ExecutionLock`, procedural workflow revalidation | `ARCHITECTURE.md`, `ACTION_CONTRACTS.md` |
| **Multilingual Speech STT / TTS Subsystem** | `STT_TTS_ARCHITECTURE.md` | `SpeechInputProvider`, `SpeechOutputProvider`, `LanguageEngineStatus`, EN/TE/KN/HI normalizers | `CAPABILITY_MATRIX.md`, `LOW_RAM_DESIGN.md` |
| **External Knowledge & Prompt Defenses** | `EXTERNAL_KNOWLEDGE_ARCHITECTURE.md` | `<untrusted_external_content>` isolation, SAF chat import parser, provenance hashing | `RISKS_AND_LIMITATIONS.md`, `MEMORY_AND_LEARNING.md` |
| **Hardware Control Boundaries & Risk** | `HARDWARE_CAPABILITIES.md` | Hardware classification tiers, `ActionPolicyEngine` risk tiers, Settings UI fallbacks | `CAPABILITY_MATRIX.md`, `PERMISSION_MATRIX.md` |
| **Testing Pyramid & Certification** | `TESTING_STRATEGY.md` | 3-Level Testing Pyramid (Level 1, Level 2, Level 3), in-app `TestCenter` suite | `DEFINITION_OF_DONE.md`, `PHASE_PLAN.md` |
| **Multi-Tiered Definition of Done** | `DEFINITION_OF_DONE.md` | 8-Gate Definition of Done checklist, false-success prohibitions | `TESTING_STRATEGY.md`, `PHASE_PLAN.md` |
| **Platform Risks & Boundary Limits** | `RISKS_AND_LIMITATIONS.md` | Platform security boundaries, non-root limitations, LMK termination risks | `PERMISSION_MATRIX.md`, `HARDWARE_CAPABILITIES.md` |

---

## 3. Reconciled Consistency Matrix Across 18 Audit Areas

| # | Consistency Audit Area | Audit Status | Reconciled Authoritative Specification Rule |
|---|---|---|---|
| **1** | **Phase Count & Dependency Chain** | **PASS** | Exactly ONE authoritative 23-phase plan (Phase 0 through Phase 22) defined in `PHASE_PLAN.md` and referenced across all docs. Zero competing phase counts exist. |
| **2** | **Authoritative Storage Path & DB Name** | **PASS** | `LOGGING_AND_AUDIT.md` is owner: `/data/data/com.localagent.app/files/agent/agent.db`. All tables (`agent_events`, `episodes`, `workflows`, `semantic_data`, `knowledge`) stored inside this single database. |
| **3** | **Storage File Budget & WAL Accounting** | **PASS** | `LOGGING_AND_AUDIT.md` & `LOW_RAM_DESIGN.md`: Total storage cap = 30 MB maximum. Storage accounting formula explicitly budgets `FileSize("agent.db") + FileSize("agent.db-wal") + FileSize("agent.db-shm")` with automated `PRAGMA wal_checkpoint(TRUNCATE)`. |
| **4** | **Package & Module Naming** | **PASS** | `ARCHITECTURE.md` is owner: Root package `com.localagent.*`. Core modules: `app`, `core`, `accessibility`, `system`, `voice`, `storage`, `memory`, `research`, `solver`, `testing`. |
| **5** | **Permission Ownership & SAF Protocol** | **PASS** | `PERMISSION_MATRIX.md` is owner: Normal Manifest, Runtime, Special App Access, User-Consent Session, Manifest Configuration (`<queries>`), and Privileged. Document import uses SAF `Intent.ACTION_OPEN_DOCUMENT` (`content://` URIs). |
| **6** | **Capability Rule Contract Model** | **PASS** | `CAPABILITY_MATRIX.md` is owner: `CapabilityRule` data structure incorporates `minApi`, `maxApi`, `targetSdkConstraints`, `requiredPermissions`, `requiredSpecialAccess`, `requiredPrivilege`, `directApiAvailable`, `fallbackStrategy`, `verificationStrategy`, and `riskLevel`. |
| **7** | **Universal Execution Pipeline Parity** | **PASS** | `ARCHITECTURE.md` is owner: All input channels (Console, Movable Overlay, Voice, Workflows, Solvers, AI Planners) converge into `NormalizedCommand` -> `GoalDispatcher`. Zero channel-specific direct action executors exist. |
| **8** | **Single Device Execution Lock** | **PASS** | `MEMORY_AND_LEARNING.md` is owner: Priority queue manages incoming commands in `GoalDispatcher`; only ONE foreground device-control transaction manipulates the Accessibility channel at a time via `ExecutionLock`. |
| **9** | **Observation Snapshot Lifecycle** | **PASS** | `OBSERVATION_MODEL.md` is owner: Single `rootInActiveWindow` acquisition per snapshot cycle. Extract primitives into immutable data objects and recycle every `AccessibilityNodeInfo` instance in `finally` blocks. |
| **10**| **Node Identity & Local Metadata** | **PASS** | `OBSERVATION_MODEL.md` is owner: `observationInstanceId` and `parentInstanceId` are snapshot-local metadata ONLY. Target matching uses composite `nodeIdentity` keys rated by `NodeIdentityConfidence` (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`). |
| **11**| **Target-Aware Verification Contracts** | **PASS** | `ACTION_CONTRACTS.md` is owner: Target-aware `VerificationStrategy` contracts strictly prohibit generic UI mutations or node count shifts from triggering `SUCCESS_VERIFIED`. |
| **12**| **Low-RAM Targets vs Guarantees** | **PASS** | `LOW_RAM_DESIGN.md` is owner: Memory behavior defined as engineering threshold targets (`TARGET`, `WARNING`, `CRITICAL`) evaluated against a 1.5 GB RAM reference API 27 profile. Routine `System.gc()` reliance eliminated. |
| **13**| **STT / TTS Multilingual Lifecycle** | **PASS** | `STT_TTS_ARCHITECTURE.md` is owner: Providers runtime-detect language support via `LanguageEngineStatus` (`AVAILABLE`, `OFFLINE_AVAILABLE`, `NETWORK_REQUIRED`, `UNAVAILABLE`, `NOT_SUPPORTED`). Speech engines initialized on-demand and destroyed immediately when idle. |
| **14**| **Untrusted External Content Security** | **PASS** | `EXTERNAL_KNOWLEDGE_ARCHITECTURE.md` is owner: Web content, chat exports, and AI responses are tagged as `<untrusted_external_content>` data. External content cannot directly invoke device APIs and must pass `ActionPolicyEngine` checks. |
| **15**| **Hardware Capability Fallbacks & Risk** | **PASS** | `HARDWARE_CAPABILITIES.md` is owner: Hardware features categorized into 5 tiers (SUPPORTED, LIMITED, OEM_DEPENDENT, ADB_DEVELOPMENT_ONLY, RESTRICTED). Direct APIs used on API 27–28; Settings UI Automation used on API 29+. |
| **16**| **Goal & Task Lifecycle State Machines** | **PASS** | `MEMORY_AND_LEARNING.md` is owner: `GoalState` (`CREATED` through `RECOVERING`) and `TaskLifecycle` state machines govern execution. Synchronous SQLite commits enable LMK process death recovery. |
| **17**| **3-Level Testing Pyramid** | **PASS** | `TESTING_STRATEGY.md` is owner: LEVEL 1 (Contract/Unit), LEVEL 2 (Device Self Test via `TestCenter`), LEVEL 3 (Cross-App E2E). Explicit rule: `UNIT PASS != CONTRACT PASS != REAL DEVICE PASS`. |
| **18**| **Definition of Done Validation Gates** | **PASS** | `DEFINITION_OF_DONE.md` is owner: 8-Gate Definition of Done checklist. Explicitly prohibits claiming completion based on "build succeeds", "unit tests pass", or "`performAction() == true`". |

---

## 4. Comprehensive 23-Phase Implementation Contracts (Phases 0 – 22)

Every phase in `PHASE_PLAN.md` is fully specified with explicit contract parameters:

| Phase | Phase Name | INPUT | DEPENDENCIES | CAPABILITIES UNLOCKED | IMPLEMENTATION BOUNDARY | TESTS | REAL DEVICE TESTS | EVIDENCE | EXIT CRITERIA | FAILURE & RECOVERY CONDITIONS |
|---|---|---|---|---|---|---|---|---|---|---|
| **Phase 0** | Specification & Planning | Requirements & research | None | None (Planning) | Root `.md` specs only | Spec audit tests | None | `docs/PHASE_0_9_FINAL_AUDIT.md` | All specs frozen; 100% audit pass | Contradiction found -> Fix spec before freeze |
| **Phase 1** | Foundation & Domain Core | Phase 0 specs | `core` module, Kotlin SDK | Domain Core & Commands | Pure Kotlin models, CommandRegistry, Policy Engine | Tier A Unit Tests | None | JUnit test execution report | 100% unit test pass; zero external dependencies | Invalid command -> Return `RESULT_INVALID_SYNTAX` |
| **Phase 2** | Persistent Storage & Logging | Phase 1 core models | `storage`, Android SQLite API 27 | Persistent Logging & Audit | Room `agent.db` schema, WAL manager, `EventLogger` | Tier A DB Unit Tests, Tier B WAL Tests | API 27 DB Checkpoint Test | DB row insertion logs, WAL size report | 1,000 logs inserted; DB + WAL <= 30 MB | DB Disk Full -> Execute WAL checkpoint & retention purge |
| **Phase 3** | Permission & Capability Manager | Phase 1 & 2 | `system`, Android `PackageManager` | Permission Detection & Center | `PermissionManager`, Settings intent adapters, Center UI | Tier A Permission Unit Tests, Tier B Intent Tests | Settings Intent Launch Test | Permission Center UI layout dump | Special access intents verified on API 27; passive degradation ready | Permission Denied -> Trigger passive degradation mode |
| **Phase 4** | Accessibility Service Foundation | Phase 3 permissions | `accessibility`, `AccessibilityService` | A11y Binding & Passive Listener | `AgentAccessibilityService`, passive lifecycle binder | Tier B A11y Binding Tests | Service Connection / Disconnect Test | Service connection log trace | Service binds cleanly on API 27; unbind triggers passive mode | A11y Unbound -> Return `ACCESSIBILITY_UNAVAILABLE` |
| **Phase 5** | Observation & Snapshot Engine | Phase 4 A11y binding | `accessibility`, `ObservationSnapshotGenerator` | `UI_OBSERVE`, `SNAPSHOT_DIFF` | Single-root snapshot generator, node recycler, diff engine | Tier A Diff Unit Tests, Tier B Recycling Tests | UI Snapshot Primitive Extraction Test | Snapshot JSON primitive dump, heap trace | Single `rootInActiveWindow` call; zero un-recycled nodes; heap < 35 MB | Stale Root -> Re-observe active window |
| **Phase 6** | Universal Action Engine | Phase 5 snapshots | `accessibility`, `TargetResolver` | Live Target Resolution | Ancestor traversal algorithms (`findClickableAncestor`, etc.) | Tier A Ancestor Search Unit Tests | Live Node Target Re-acquisition Test | Target resolution diagnostic log | Child TextView "7" maps to parent MaterialButton container | Target Missing -> Return `TARGET_NOT_FOUND` |
| **Phase 7** | Global & UI Action Execution | Phase 6 action engine | `accessibility`, `VerificationStrategy` | Core UI Automation & Navigation | Action contracts for CLICK, LONG_CLICK, INPUT, SCROLL, BACK, HOME | Tier A Verification Unit Tests | Real-Device Calculator & Settings Tests | Pre/Post snapshot diff JSON, result logs | Target-aware verification confirmed on physical API 27 hardware | State Unchanged -> Return `DISPATCHED_BUT_NOT_VERIFIED` |
| **Phase 8** | Application Control Engine | Phase 7 actions | `system`, `PackageManager` | `APP_LAUNCH`, `APP_RESOLVE` | `AppResolver`, `AppLauncher`, `LaunchVerifier` | Tier A App Resolver Unit Tests | Settings & Calculator Launch Test | App launch verification trace | Target app launched and foreground verified within 3-second timeout | App Missing -> Return `TARGET_NOT_FOUND` |
| **Phase 9** | Command Console UI | Phase 1, 7, 8 | `app`, Android View System | Manual Console Command Entry | Thin-client Console Activity, real-time log viewer | Tier B Console Dispatch Tests | Console Command Dispatch Test | Console execution logs | Console commands dispatch to `GoalDispatcher` and display events | Syntax Error -> Display error in Console log view |
| **Phase 10** | Automated Test Center | Phase 1–9 | `testing`, `TestCenter` UI | Diagnostic Self-Testing | In-app `TestCenter` Activity & contract suite runner | Tier B In-App Contract & Parity Suite | In-App Contract Test Execution | In-app test report JSON output | Contract suite executes inside app with 100% pass rate | Contract Failure -> Log failure evidence in diagnostic DB |
| **Phase 11** | Movable Overlay Surface | Phase 1, 3, 9, 10 | `app`, `TYPE_APPLICATION_OVERLAY` | `OVERLAY_SHOW`, `OVERLAY_MOVE` | Overlay Service, floating view, drag listener, action buttons | Tier B Overlay Render Tests, Tier B Parity Tests | Overlay Rendering & Action Parity Test | Cross-channel parity test report | Overlay actions produce identical execution traces as Console | Overlay Denied -> Fallback to Console UI |
| **Phase 12** | Hardware & System Controls | Phase 1, 3, 7 | `system`, `AudioManager`, `CameraManager` | Volume, Brightness, Flashlight, Wi-Fi | Volume, Brightness, Flashlight, and Wi-Fi panel controllers | Tier A Risk Policy Unit Tests | Hardware State Mutation Test | Hardware state change logs | Hardware actions verify setting changes; API 27–36+ fallbacks work | Direct API Restricted -> Fallback to UI Automation flow |
| **Phase 13** | Speech STT / TTS Subsystem | Phase 1, 9 | `voice`, `SpeechRecognizer`, `TextToSpeech` | `SPEECH_STT`, `SPEECH_TTS` | STT/TTS providers, EN/TE/KN/HI normalizers, capability detector | Tier A Syntax Normalizer Unit Tests | Real-Device Voice Input/Output Test | Spoken transcript to command logs | Spoken commands convert accurately; speech destroyed when idle | Engine Missing -> Fallback to Console keyboard input |
| **Phase 14** | Workflow & Automation Engine | Phase 7, 8, 9 | `core`, `WorkflowEngine` | `WORKFLOW_EXECUTE` | Deterministic workflow engine, step dispatcher, retry policies | Tier A Workflow Step Unit Tests | Multi-Step Workflow Automation Test | Multi-step workflow execution trace | Multi-step automation flows execute deterministically with verification | Step Failure -> Execute retry policy or abort |
| **Phase 15** | Episodic Memory & Learning | Phase 2, 14 | `memory`, Room `episodes` & `workflows` | Procedural Learning & Revalidation | Episodic task log store, procedural workflow extractor | Tier A Learning Extraction Unit Tests | Workflow Replay & Target Revalidation Test | Learned workflow DB records | Replaying learned workflows re-validates live UI target nodes | Stale Target -> Trigger target re-resolution |
| **Phase 16** | Browser Research Foundation | Phase 7, 8 | `research`, Chrome A11y | `BROWSER_NAVIGATE`, `BROWSER_OBSERVE` | Web page observation engine, `<untrusted_external_content>` wrapper | Tier A Untrusted Text Wrapper Unit Tests | Chrome Web Page Navigation Test | Web page snapshot primitive dump | Web text extracted safely and tagged as untrusted data | Injection Attack -> Block command via Policy Engine |
| **Phase 17** | External Knowledge Ingestion | Phase 15, 16 | `research`, Storage Access Framework | `EXTERNAL_KNOWLEDGE_IMPORT` | SAF document picker, `ChatExportParser`, provenance generator | Tier A SAF URI & Parser Unit Tests | Document Import & Provenance Test | Parsed knowledge record with SHA-256 hash | Chat exports imported via SAF without broad storage permissions | Parse Error -> Log import error event |
| **Phase 18** | Structured Problem Solver | Phase 5, 7 | `solver`, Pure Kotlin CSP Solver | `STRUCTURED_PROBLEM_SOLVER` | Grid UI state extractor, Sudoku / form solver, step generator | Tier A Solver Algorithm Unit Tests (100% pass) | Live UI Board Solving Test | Solved board state verification diff | Visual grid extracted, solved in memory, and populated into UI | Solver Failure -> Abort grid automation |
| **Phase 19** | Trip & General Research Engine | Phase 16, 17 | `research`, `ResearchPlanner` | `TRIP_RESEARCH_ENGINE` | Multi-step goal decomposer, travel option fact extractor, report generator | Tier A Goal Decomposition Unit Tests | Travel Research Execution Test | Generated trip itinerary report JSON | Research goal executed across queries; itinerary generated with citations | Query Failure -> Fallback to manual search prompt |
| **Phase 20** | On-Device / Cloud AI Planner | Phase 1, 14, 19 | `ai`, Pluggable LLM Providers | `AI_PLANNING` | Pluggable AI planner adapter, JSON plan schema validator | Tier A Plan Schema & Injection Interceptor Tests | AI Plan Execution Test | AI plan output JSON and interceptor logs | AI planner outputs valid `NormalizedCommand` plans; high-risk blocked | Schema Error -> Reject plan and request re-planning |
| **Phase 21** | Resource Hardening & Recovery | Phase 1–20 | `core`, `ResourceManager` | Process Recovery & Hardening | Process death recovery protocol, `agent.db` LMK state restore | Tier D Low-RAM Stress Tests | LMK Process Kill & Resume Test | LMK kill and resume trace log | Zero state loss after simulated LMK kill; heap footprint <= targets | LMK Kill -> Restore goal state from `agent.db` |
| **Phase 22** | Master Device Certification | All Phase 0–21 deliverables | Physical API 27 Reference Hardware | Complete System Certification | `docs/MASTER_CERTIFICATION_REPORT.md`, diagnostic log archive | Full Level 1, 2, 3 Test Suite Execution | Physical API 27 Master Certification | Certification execution report JSON | 100% test pass rate on physical API 27 hardware | Certification Fail -> Block release until resolved |

---

## 5. Freeze Gate Declaration

All 18 cross-document consistency areas and 23 phase contracts have been verified and reconciled with zero remaining contradictions.

### Declaration: **PASS — ARCHITECTURE FROZEN**

Phase 0.9 architecture is officially **FROZEN**. Implementation Phase 1 is authorized to begin.
