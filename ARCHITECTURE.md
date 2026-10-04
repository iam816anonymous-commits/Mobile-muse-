# ARCHITECTURE.md — LocalAgent Core Architecture Specification

## 1. Executive Summary & Core Philosophy

**LocalAgent** is a modular, low-RAM, offline-first Android device agent capable of observing, reasoning about, and safely executing actions on Android devices. It prioritizes **deterministic automation first**, with optional AI learning and planning layers sitting above it.

### Core Architectural Mandates
1. **Deterministic Core Without LLM:** The execution engine must function 100% deterministically without requiring an LLM or cloud connectivity.
2. **One Command → One Execution Pipeline:** Every input channel (Console, Movable Overlay, Voice, Automated Test Harness, Future AI Planner, Future Browser Agent) normalizes user intent into a single unified `NormalizedCommand`. There are **no separate action executors** for different input surfaces.
3. **API 27 Baseline (Android 8.1):** The core agent targets Android 8.1 / API level 27 as its primary baseline for low-RAM devices while gracefully incorporating modern API compatibility adapters up to API 36+.
4. **No Fake Success:** `performAction() == true` from Android Accessibility APIs is treated only as `DISPATCHED`. Complete execution requires live node reacquisition, ancestor traversal, and post-action UI observation diff verification.
5. **Low-RAM First:** Memory footprint must be kept minimal through event-driven processing, lazy initialization, short-lived AccessibilityNodeInfo snapshot primitives, and bounded local storage.

---

## 2. High-Level Architecture Diagram

```text
                     ┌────────────────────────────────────────────────────────┐
                     │                     INPUT SURFACES                     │
                     │  Console  │  Movable Overlay  │  Voice  │  Test Harness│
                     │  Future AI Planner   │   Future Browser Learning       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               INPUT NORMALIZATION LAYER                │
                     │   Command / Intent Normalizer → NormalizedCommand       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               UNIVERSAL EXECUTION CORE                 │
                     │  CommandRegistry ──► GoalDispatcher ──► ActionPlanner  │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │                  CAPABILITY RESOLVER                   │
                     │  Check Prerequisites, Permissions, and API Tiers       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │                  TARGET RESOLVER                       │
                     │  Live Node Acquisition ──► Ancestor Traversal          │
                     │  (Clickable / Long-Clickable / Scrollable / Editable)  │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │                  ACTION EXECUTORS                      │
                     │ Accessibility Engine │ System APIs │ Hardware Controls │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               OBSERVATION & VERIFICATION               │
                     │  Fresh UI Snapshot Capture ──► State-Diff Engine      │
                     │  (SUCCESS / DISPATCHED_UNVERIFIED / TARGET_NOT_FOUND)  │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               PERSISTENCE & AUDIT LOGGING              │
                     │  Structured EventLogger ──► SQLite/Room Event DB       │
                     │  Episodic Memory / Procedural Workflow Storage         │
                     └────────────────────────────────────────────────────────┘
```

---

## 3. Detailed Component Architecture

### 3.1 Input Normalization & Universal Command Pipeline
Regardless of origin, every user or agent action is translated into a standardized data model:

```kotlin
data class NormalizedCommand(
    val id: String = UUID.randomUUID().toString(),
    val source: CommandSource, // CONSOLE, OVERLAY, VOICE, AUTOMATION, AI, BROWSER
    val actionType: ActionType, // CLICK, LONG_CLICK, TEXT_INPUT, SCROLL_FORWARD, BACK, HOME, etc.
    val targetSelector: TargetSelector?,
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
```

1. **`CommandRegistry`**: Maintains the catalog of known action types, required permissions, syntax rules, and capability mappings.
2. **`GoalDispatcher`**: Queues and sequences incoming `NormalizedCommand` objects, ensuring thread-safe, non-overlapping action dispatch.
3. **`ActionPlanner`**: Decomposes multi-step intents into sequential deterministic sub-commands.
4. **`CapabilityResolver`**: Queries the `PermissionManager` and device API capability registry to confirm whether the required action is runnable on the current device and state.

### 3.2 Accessibility & Target Resolution Subsystem
- **Live Node Acquisition**: Never caches `AccessibilityNodeInfo` references across execution bounds. Live nodes are fetched on-demand via `AccessibilityService.rootInActiveWindow`.
- **Actionable Ancestor Traversal**:
  - `CLICK`: Search target node; if not clickable, traverse up the node hierarchy until a node with `isClickable == true` is found.
  - `LONG_CLICK`: Search target node; if not long-clickable, traverse up to `isLongClickable == true`.
  - `SCROLL`: Search target node; if not scrollable, traverse up to `isScrollable == true`.
  - `TEXT_INPUT`: Search target node; locate `isEditable == true` target or perform focus + text replacement.
- **Node Release**: Immediately recycle/release `AccessibilityNodeInfo` objects to avoid memory leaks on Android 8.1.

### 3.3 Observation & Verification Subsystem
- **Observation Snapshot**: Captures a lightweight, immutable representation of the UI tree containing only primitives (`package`, `activity`, `text`, `contentDescription`, `resourceId`, `bounds`, `flags`).
- **State-Diff Engine**: Compares `Pre-Action Snapshot` and `Post-Action Snapshot` after a settle delay (e.g., 200ms–500ms).
- **Result Classification**:
  - `SUCCESS_VERIFIED`: Action dispatched and expected state change observed (e.g., window changed, text modified, checkbox toggled).
  - `DISPATCHED_UNVERIFIED`: Action dispatched (`performAction()` returned true), but no observable UI diff occurred within timeout.
  - `ACTION_FAILED`: `performAction()` returned false.
  - `TARGET_NOT_FOUND`: Target selector failed to match any live node.
  - `CAPABILITY_UNAVAILABLE`: Prerequisites or permissions missing.

### 3.4 Storage & Event Audit Subsystem
- **App-Private Storage (`/data/data/com.localagent.app/files/agent/`)**:
  - `logs/agent-events.db`: SQLite/Room database storing all structured event logs.
  - `memory/workflows.json`: Procedural sequence definitions.
  - `evidence/`: Optional diagnostic snapshots (JSON UI trees / failure screenshots).
- **Log Rotation Policy**: Enforces bounded storage (e.g., max 20 MB total log size), rolling over logs daily or when size thresholds are met, automatically compressing or purging expired logs.

---

## 4. Proposed Repository & Directory Structure

```text
LocalAgent/
├── ARCHITECTURE.md
├── PHASE_PLAN.md
├── CAPABILITY_MATRIX.md
├── PERMISSION_MATRIX.md
├── ACTION_CONTRACTS.md
├── OBSERVATION_MODEL.md
├── LOGGING_AND_AUDIT.md
├── MEMORY_AND_LEARNING.md
├── LOW_RAM_DESIGN.md
├── TESTING_STRATEGY.md
├── HARDWARE_CAPABILITIES.md
├── STT_TTS_ARCHITECTURE.md
├── EXTERNAL_KNOWLEDGE_ARCHITECTURE.md
├── RESEARCH_SOURCES.md
├── DEFINITION_OF_DONE.md
├── RISKS_AND_LIMITATIONS.md
│
├── app/                        # Android App Module (View system, UI overlays, Console UI)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/localagent/
│   │   │   │   ├── app/        # Application class, lifecycle, DI setup
│   │   │   │   ├── ui/         # Console Activity, Permission Center, Settings UI
│   │   │   │   └── overlay/    # Movable Overlay WindowManager service & controller
│   │   │   ├── res/            # Layouts (Android View System), resources, values
│   │   │   └── AndroidManifest.xml
│   │   └── test/               # App-level unit tests
│   └── build.gradle
│
├── core/                       # Pure Kotlin / Core Domain Architecture Module
│   ├── src/
│   │   ├── main/java/com/localagent/core/
│   │   │   ├── command/        # NormalizedCommand, CommandRegistry, Syntax Parsers
│   │   │   ├── execution/      # GoalDispatcher, ActionPlanner, ExecutionEngine
│   │   │   ├── result/         # ExecutionResult, ResultCode, StateDiff
│   │   │   ├── capability/     # CapabilityResolver, Prerequisites
│   │   │   ├── policy/         # Action Risk Classifier, Security Guardrails
│   │   │   └── lifecycle/      # Agent state machine (ACTIVE, IDLE, PAUSED)
│   │   └── test/               # Pure Kotlin JUnit unit tests
│   └── build.gradle
│
├── accessibility/              # Android Accessibility Engine Module
│   ├── src/
│   │   ├── main/java/com/localagent/accessibility/
│   │   │   ├── service/        # AgentAccessibilityService implementation
│   │   │   ├── observation/    # Live node capture, Snapshot Generator
│   │   │   ├── resolver/       # TargetResolver, Ancestor Traversal
│   │   │   └── actions/        # Global & Node Action Executors
│   │   └── test/
│   └── build.gradle
│
├── system/                     # Permissions, System APIs & Hardware Module
│   ├── src/
│   │   ├── main/java/com/localagent/system/
│   │   │   ├── permissions/    # PermissionManager, Special Access Checking
│   │   │   ├── apps/           # AppResolver, AppLauncher, LaunchVerifier
│   │   │   ├── hardware/       # Volume, Brightness, Flashlight, Media Controls
│   │   │   └── settings/       # System Settings Read/Write Adapters
│   │   └── test/
│   └── build.gradle
│
├── voice/                      # STT & TTS Integration Module
│   ├── src/
│   │   ├── main/java/com/localagent/voice/
│   │   │   ├── stt/            # SpeechInputProvider, On-Device Recognizer Adapter
│   │   │   └── tts/            # SpeechOutputProvider, Android TextToSpeech Adapter
│   │   └── test/
│   └── build.gradle
│
├── storage/                    # SQLite / Local Persistence Module
│   ├── src/
│   │   ├── main/java/com/localagent/storage/
│   │   │   ├── db/             # Room Database, DAOs, Entities
│   │   │   ├── logger/         # Structured EventLogger Implementation
│   │   │   └── files/          # Bounded File Storage & Retention Manager
│   │   └── test/
│   └── build.gradle
│
├── memory/                     # Memory & Procedural Workflow Engine
│   ├── src/
│   │   ├── main/java/com/localagent/memory/
│   │   │   ├── episodic/       # Task execution history
│   │   │   ├── procedural/     # Workflow definitions & revalidation
│   │   │   └── semantic/       # App package knowledge & semantic mappings
│   │   └── test/
│   └── build.gradle
│
└── testing/                    # Master Automated Device Test Harness
    ├── src/
    │   ├── main/java/com/localagent/testing/
    │   │   ├── runner/         # Device Test Suite Executor
    │   │   ├── contracts/      # Phase Test Contracts & Validation Specs
    │   │   └── evidence/       # Test Evidence Collector & Diagnostic Generator
    │   └── test/
    └── build.gradle
```

---

## 5. Key Architecture Design Patterns

1. **Adapter Pattern for API Tiers:** System APIs differ across API 27 through API 36+. All platform interactions are wrapped in platform compatibility adapters (e.g., `GlobalActionAdapter`, `VolumeControlAdapter`).
2. **Strategy Pattern for Action Resolution:** Target resolution strategies (`ViewByIdStrategy`, `ExactTextStrategy`, `ContentDescriptionStrategy`, `AncestorTraversalStrategy`) are tried in prioritized sequence.
3. **Observer Pattern for Event Logging:** Components publish structured events to `EventLogger`; storage listeners save to SQLite without blocking UI or execution threads.
4. **State Pattern for Lifecycle & Resource States:** Agent states (`IDLE`, `EXECUTING`, `OBSERVING`, `VOICE_ACTIVE`, `LOW_MEMORY_DEGRADED`) enforce power and RAM budgets.
