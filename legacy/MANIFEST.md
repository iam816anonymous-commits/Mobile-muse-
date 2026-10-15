# Legacy Execution & Observation Pipeline Archive Manifest

- **Source Commit:** `97523492a9f698698e7524071e6f33b17f6beec0`
- **Baseline Tag:** `v0.0.1`
- **Purpose:** Non-compiled reference archive for historical execution, observation, snapshot, and verification pipeline contracts.

## Archived Source Files

1. `legacy/app/src/main/java/com/localagent/app/accessibility/AgentAccessibilityService.kt`
   - Original accessibility service snapshot extraction, window candidate scoring, and snapshot publication logic.
2. `legacy/app/src/main/java/com/localagent/app/accessibility/UiActionExecutor.kt`
   - Original UI action execution pipeline, live target re-acquisition, and post-action snapshot capture.
3. `legacy/app/src/main/java/com/localagent/app/accessibility/CommandExecutionCoordinator.kt`
   - Original command execution coordinator and action request mapping.
4. `legacy/app/src/main/java/com/localagent/app/system/AppControlEngine.kt`
   - Original AppResolver, AppLauncher, and LaunchVerifier implementation.
5. `legacy/app/src/main/java/com/localagent/app/ui/ObservationActivity.kt`
   - Original ObservationActivity View A, View B, and View C data binding.
6. `legacy/core/src/main/java/com/localagent/core/action/VerificationStrategies.kt`
   - Original TargetAwareVerificationStrategy and NavigationAwareVerificationStrategy.
7. `legacy/core/src/main/java/com/localagent/core/action/ActionDomainModels.kt`
   - Original ActionRequest, VerificationResult, and ActionExecutionResult domain contracts.
8. `legacy/core/src/main/java/com/localagent/core/observation/ObservationDomainModels.kt`
   - Original ObservationNode, ObservationBounds, and ObservationSnapshot models.
