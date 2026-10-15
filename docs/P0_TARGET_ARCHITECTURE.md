# P0 Target Architecture Specification

## 1. Execution & Observation Lifecycle Flow

```
User Command
  │
  ▼
CommandNormalizer (NormalizedCommand sequence)
  │
  ▼
CommandExecutionCoordinator (Assigns parent & step executionId)
  │
  ├───► AppControlEngine / AppResolver (Dynamic pm.resolveActivity for CATEGORY_HOME)
  │       └─► LaunchVerifier (Verifies exact resolved Home package name)
  │
  └───► UiActionExecutor (Assigns unique executionId)
          ├─► Pre-Dispatch Live Snapshot (Tagged with executionId)
          ├─► Live Target Resolution & Re-Acquisition
          ├─► Accessibility performAction Dispatch
          ├─► Post-Dispatch Snapshot Capture (Re-acquires fresh live root via getLiveExternalRootNode)
          └─► TargetAwareVerificationStrategy (Computes semantic diff, records FailureStage)
                │
                ▼
  AgentAccessibilityService.recordActionExecution
    └─► ActionExecutionResult (Binds executionId, preSnapshot, postSnapshot)
          │
          ├─► Room Event Database (agent.db persistence)
          └─► ObservationActivity View C (Renders pre/post snapshot pair for executionId)
```

## 2. Core Stage Contracts & Ownership

### Stage 1: Command Normalization & Coordination
- **Owner:** `CommandNormalizer` & `CommandExecutionCoordinator`
- **Contract:** Converts raw input into `NormalizedCommand` sequence. Assigns `executionId` to each step. Compound commands execute steps sequentially; if step $N$ fails, steps $N+1$ are marked `SKIPPED`.

### Stage 2: Application Control & Launch Verification
- **Owner:** `AppResolver`, `AppLauncher`, `LaunchVerifier` (`AppControlEngine.kt`)
- **Contract:** For Home launcher queries (`hilauncher`, `launcher`, `home`), `AppResolver` resolves `pm.resolveActivity(Intent.ACTION_MAIN + CATEGORY_HOME, MATCH_DEFAULT_ONLY)` to return the exact package name installed on the device (`com.transsion.hilauncher.v2`). `LaunchVerifier` verifies that exact resolved package name, treating System UI overlays and blank package names as intermediate transient states.

### Stage 3: UI Action Execution & Snapshot Provenance
- **Owner:** `UiActionExecutor`
- **Contract:** Assigns `executionId` up front. Captures `preSnapshot` tagged with `executionId`. Dispatches action against re-acquired live target. Polls for `postSnapshot` passing `request.expectedPackageName ?: preSnapshot.packageName`. Tags `postSnapshot` with `executionId`.

### Stage 4: Live Root Re-Acquisition & Snapshot Extraction
- **Owner:** `AgentAccessibilityService`
- **Contract:** `captureLiveSnapshot(targetPackageName, executionId)` evaluates active window candidates. If candidate nodes are recycled or detached during UI re-layout, calls `getLiveExternalRootNode(targetPkg)` to safely re-acquire a fresh live root node directly from interactive window candidates. Extracts `extSnap` with an advancing `generation` counter, a new `snapshotId`, and `executionId` provenance.

### Stage 5: Verification & Failure Stage Assignment
- **Owner:** `TargetAwareVerificationStrategy`
- **Contract:** Computes semantic diff between `preSnapshot` and `postSnapshot`. If `postSnapshot == null`, returns `status = VERIFICATION_FAILED` with `failureStage = FailureStage.POST_ACTION_CAPTURE` and explicit diagnostic error message.

### Stage 6: Persistence & View C Rendering
- **Owner:** `AgentAccessibilityService` & `ObservationActivity` View C
- **Contract:** `recordActionExecution` stores immutable `ActionExecutionResult` in bounded `actionExecutionHistory` and emits `ACTION_EVIDENCE_GENERATED` event. View C renders Panel A (`preSnapshot`) and Panel B (`postSnapshot`) belonging strictly to the selected `executionId`.
