# ARCHITECTURE.md — LocalAgent Core Architecture Specification

## 1. Executive Summary & Core Philosophy

**LocalAgent** is a modular, low-RAM, offline-first Android device agent capable of observing, reasoning about, and safely executing actions on Android devices. It prioritizes **deterministic automation first**, with optional AI learning, research, and planning layers sitting above it.

### Core Architectural Mandates
1. **Deterministic Core Without LLM:** The execution engine must function 100% deterministically without requiring an LLM or cloud connectivity.
2. **One Command → One Execution Pipeline:** Every input channel (Console, Movable Overlay, Voice, Automated Test Harness, Future AI Planner, Future Browser Agent) normalizes user intent into a single unified `NormalizedCommand`. There are **no separate action executors** for different input surfaces.
3. **API 27 Baseline (Android 8.1):** The core agent targets Android 8.1 / API level 27 as its primary baseline for low-RAM devices while gracefully incorporating modern API compatibility adapters up to API 36+.
4. **No Fake Success:** `performAction() == true` from Android Accessibility APIs is treated only as `DISPATCHED`. Complete execution requires live node reacquisition, ancestor traversal, and target-aware post-action UI observation diff verification.
5. **Low-RAM First Across All Phases:** Memory footprint is kept minimal through event-driven processing, lazy initialization, short-lived AccessibilityNodeInfo snapshot primitives, immediate `.recycle()` calls, and bounded local storage with strict WAL/journal accounting.

---

## 2. High-Level Architecture Diagram & Universal Execution Pipeline

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
                     │          UNIVERSAL CONCURRENCY DISPATCHER              │
                     │  GoalDispatcher (Priority Queue + ExecutionLock)       │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │         CAPABILITY REGISTRY & POLICY ENGINE            │
                     │  CapabilityRegistry ──► ActionPolicyEngine             │
                     │  (Risk Levels: LOW, MEDIUM, HIGH, CRITICAL)             │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │                  TARGET RESOLVER                       │
                     │  Single-Root Live Acquisition ──► Ancestor Traversal  │
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
                     │  Single Root Snapshot Capture ──► VerificationStrategy │
                     │  (SUCCESS_VERIFIED / DISPATCHED_UNVERIFIED / FAILED)   │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │               PERSISTENCE & AUDIT LOGGING              │
                     │  Structured EventLogger ──► Unified agent.db           │
                     │  (Episodic, Procedural, Semantic, Knowledge Tables)    │
                     └───────────────────────────┘
```

---

## 3. Capability Registry & Action Policy Subsystem

### 3.1 Capability Registry
The **`CapabilityRegistry`** acts as the central source of truth for querying available agent capabilities on the current device state:

```kotlin
data class CapabilityDescriptor(
    val id: String, // e.g., "UI_CLICK", "GLOBAL_BACK", "STRUCTURED_PROBLEM_SOLVER", "TRIP_RESEARCH_ENGINE"
    val name: String,
    val category: CapabilityCategory, // NAVIGATION, UI_CONTROL, SYSTEM, HARDWARE, VOICE, RESEARCH, SOLVER, AI
    val minApi: Int,
    val requiredPermissions: List<String>,
    val requiredSpecialAccess: List<SpecialAccessType>,
    val riskLevel: ActionRiskLevel,
    val resourceCost: ResourceCostTier,
    val executionAdapter: String,
    val verificationStrategy: String,
    val fallbackStrategy: String?,
    val isAvailableOnCurrentDevice: Boolean
)

enum class ActionRiskLevel {
    LOW,       // Observe, scroll, click non-sensitive elements, open public app
    MEDIUM,    // Toggle Wi-Fi, change brightness, adjust volume, input text in non-sensitive fields
    HIGH,      // Send messages, delete files, modify system settings, grant app access
    CRITICAL   // Financial purchases, security setting changes, factory reset, credential input
}
```

### 3.2 Action Policy Engine
Before any action is dispatched, the **`ActionPolicyEngine`** evaluates the command:

```text
NormalizedCommand
       ↓
CapabilityDescriptor Lookup
       ↓
Risk Level Evaluation
       ├─► LOW: Direct Dispatch
       ├─► MEDIUM: Logged Dispatch + Policy Check
       ├─► HIGH: User Interactive Confirmation Required
       └─► CRITICAL: Explicit Auth / Strict User Confirmation Required
```

---

## 4. Specialized Reasoning Subsystems

### 4.1 Structured Problem Solver Subsystem (e.g., Sudoku, Puzzle Games, Form Solving)
The agent includes a dedicated **Structured Problem Solver** subsystem for domain-specific UI automation tasks:
1. **Board / UI Observation:** Capture live UI primitive snapshot.
2. **State Construction:** Convert visual grid / elements into a structured domain model (e.g., 9x9 Sudoku matrix).
3. **Deterministic Solver Execution:** Run deterministic backtracking or constraint satisfaction solver in pure Kotlin.
4. **Action Sequence Generation:** Generate normalized `UI_CLICK` / `UI_TEXT_INPUT` commands.
5. **Step-by-Step Verification:** Execute and verify state changes after each placement.

### 4.2 Trip & General Research Engine
Separate from browser page observation, the **Research & Planning Engine** coordinates multi-step research goals (e.g., *"Plan a 3-day trip from Bengaluru to Hampi under ₹10,000"*):
1. **Goal Decomposition:** Deconstruct request into search queries, destination criteria, and constraints.
2. **Source Gathering:** Execute web queries via browser or API adapters.
3. **Fact & Constraint Extraction:** Extract structured travel options, prices, and places into `agent.db`.
4. **Plan Generation:** Build a structured itinerary with citations.
5. **User Review & Approval:** Present research report before executing any booking or reservation actions.

---

## 5. Proposed Repository & Directory Structure

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
├── core/                       # Pure Kotlin Domain Core (Commands, Dispatcher, Policy Engine, Capability Registry)
├── accessibility/              # Android Accessibility Engine Module (Service, Observation, Ancestor Resolver)
├── system/                     # Permissions, System APIs, Hardware Controls & App Launchers
├── voice/                      # STT & TTS Integration Module (Multilingual EN, TE, KN, HI)
├── storage/                    # Unified Local Storage Module (SQLite agent.db & File Management)
├── memory/                     # Episodic, Semantic, Procedural Workflows & Knowledge Base
├── research/                   # Web Research, Trip Planning & External Knowledge Ingestion
├── solver/                     # Structured Problem Solver (Sudoku, Grid & Form Solvers)
├── docs/                       # Phase 0.9 Audit & Freeze Deliverables
└── testing/                    # Master Automated Test Harness & Diagnostic Test Center UI
```
