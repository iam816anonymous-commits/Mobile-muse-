# P0 From-Scratch Rebuild Implementation & CI Plan

## 1. Authoritative Requirements by Authorized Phase

### Phase 5 — Observation Engine
- Capture active permitted Android Accessibility node hierarchy.
- Differentiate LocalAgent UI from external target application windows.
- Maintain atomic generation tracking and attach `executionId` provenance to `ObservationSnapshot`.
- Safe node re-acquisition (`getLiveExternalRootNode`) when accessibility nodes are recycled or detached.
- Expose structured observation failures without fabricating synthetic nodes.

### Phase 6 — UI Action Engine
- Consume typed action requests (`UI_CLICK`, `UI_LONG_CLICK`, `UI_TEXT_INPUT`, `UI_SCROLL_FORWARD`, `UI_SCROLL_BACKWARD`, `GLOBAL_*`, `APP_LAUNCH`).
- Resolve targets from valid current observation via `TargetResolver`.
- Perform pre-action and post-action snapshot capture paired with step `executionId`.
- Evaluate semantic UI outcome state changes via `TargetAwareVerificationStrategy` and `SnapshotDiffEngine`.
- Classify explicit failure stages (`FailureStage`) and diagnostic reason messages.

### Phase 7 — Application Automation Engine
- Normalize raw text commands into `NormalizedCommand` sequence via `CommandNormalizer`.
- Orchestrate step execution sequentially via `CommandExecutionCoordinator`.
- Halt compound command sequences on step failure and mark downstream steps `SKIPPED`.
- Propagate `expectedPackageName` to action execution requests.

### Phase 8 — App Control Engine
- Dynamically resolve Home activity via `pm.resolveActivity(Intent.ACTION_MAIN + CATEGORY_HOME)`.
- Launch application packages and verify usable foreground window via `LaunchVerifier`.
- Handle transient System UI windows and overlay dialogs without false `PackageMismatch` errors.
- Display exact pre/post snapshot comparison pair in `ObservationActivity` View C matching `executionId`.

---

## 2. Proposed Architecture & Stage Dependency Graph

```
User Input / Command
  │
  ▼
CommandNormalizer (Phase 7)
  │
  ▼
CommandExecutionCoordinator (Phase 7 - Assigns executionId)
  │
  ├───► AppControlEngine / AppResolver / LaunchVerifier (Phase 8)
  │
  └───► UiActionExecutor (Phase 6)
          ├─► Live Target Resolution (LiveTargetResolver)
          ├─► Accessibility performAction
          ├─► Post-Dispatch Snapshot Capture (Phase 5 - AgentAccessibilityService)
          └─► TargetAwareVerificationStrategy (Phase 6)
                │
                ▼
  ObservationActivity View C / Room Event Persistence
```

---

## 3. Implementation & Test Strategy

- **Module Structure:** `:core` (JVM domain models, normalizer, diff engine, verification strategy) and `:app` (Android Accessibility Service, execution coordinator, app control engine, UI activities).
- **Test Strategy:**
  - Core JVM unit tests for normalization, snapshot diffing, and verification strategies.
  - Robolectric integration tests simulating Android Accessibility Framework, Window Provider, Package Manager, and SQLite Room Database.
  - Continuous Integration via GitHub Actions running compilation and test tasks on every push and pull request.

---

## 4. CI Acceptance Criteria & Known Limitations

- **CI Criteria:** Automated workflow in `.github/workflows/ci.yml` validating repository files, running Kotlin compilation, executing 157 unit/integration tests, and publishing `mobile-muse-debug-apk` artifact on success.
- **Environmental Limitations:** Headless Linux container environment cannot run physical hardware touch gestures; on-device device testing is explicitly marked `NOT RUN`.
