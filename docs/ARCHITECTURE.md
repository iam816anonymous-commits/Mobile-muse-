# ARCHITECTURE.md — LocalAgent Architectural Specification

LocalAgent is a low-RAM, offline-first Android device agent operating via a single universal execution pipeline.

## Core System Flow
```text
Input Channel (Console / Overlay / Voice / Workflow / AI)
    ↓
NormalizedCommand
    ↓
GoalDispatcher (Atomic ExecutionLock)
    ↓
ActionPolicyEngine (Risk Evaluation)
    ↓
CapabilityRegistry & Target Resolver
    ↓
Android System / Accessibility APIs
    ↓
Observation & Verification
    ↓
UnifiedEventLogger (agent.db / Durable Memory)
```

## Module Architecture
- `:core` — Pure Kotlin domain models, normalizer, policy engine, dispatcher, diff engine, and contracts.
- `:app` — Android Application, AccessibilityService, Room SQLite database, System Permission Manager, and View-based diagnostic UI Activities.
