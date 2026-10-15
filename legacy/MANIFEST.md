# Legacy Execution & Observation Pipeline Archive Manifest

- **Source Commit:** `97523492a9f698698e7524071e6f33b17f6beec0`
- **Baseline Tag:** `v0.0.1`
- **Purpose:** Non-compiled reference archive for historical execution, observation, snapshot, and verification pipeline contracts.

## Archived Source Files & SHA-256 Hashes

1. `legacy/app/src/main/java/com/localagent/app/accessibility/AgentAccessibilityService.kt`
   - **SHA-256:** `b99618aa4ffdbe88c379cc2ec361d65be32ff20d213584f1a7776652e8a92f02`
   - **Description:** Original accessibility service snapshot extraction, window candidate scoring, and snapshot publication logic.

2. `legacy/app/src/main/java/com/localagent/app/accessibility/UiActionExecutor.kt`
   - **SHA-256:** `f35a0ae065c72dbe83a037d113c55913226b3bbffa870294b60d579e33f952eb`
   - **Description:** Original UI action execution pipeline, live target re-acquisition, and post-action snapshot capture.

3. `legacy/app/src/main/java/com/localagent/app/accessibility/CommandExecutionCoordinator.kt`
   - **SHA-256:** `e90eee54de28d8498aa60b96ec20feb69872963a9b6b88fca1dbd7479ffd03f4`
   - **Description:** Original command execution coordinator and action request mapping.

4. `legacy/app/src/main/java/com/localagent/app/system/AppControlEngine.kt`
   - **SHA-256:** `df3159d4a5a7ed262a421fc581b071a1f244dcdb1dcf7fc194b55fdec2a57b1a`
   - **Description:** Original AppResolver, AppLauncher, and LaunchVerifier implementation.

5. `legacy/app/src/main/java/com/localagent/app/ui/ObservationActivity.kt`
   - **SHA-256:** `7d2b4d151d10db67ba2e3b02b9dbfc72918559ec146eb205df79fca52147291a`
   - **Description:** Original ObservationActivity View A, View B, and View C data binding.

6. `legacy/core/src/main/java/com/localagent/core/action/ActionDomainModels.kt`
   - **SHA-256:** `ae016cbb4a5ae4c79b05d78e0f1eea0132591f42f40df771ab2f3e5e7dedccc2`
   - **Description:** Original ActionRequest, VerificationResult, and ActionExecutionResult domain contracts.

7. `legacy/core/src/main/java/com/localagent/core/action/VerificationStrategies.kt`
   - **SHA-256:** `2c8216ef9a1f643bc53ea6abcf5f4e1c150ef77d2df21805eae2f845f5dcc1eb`
   - **Description:** Original TargetAwareVerificationStrategy and NavigationAwareVerificationStrategy.

8. `legacy/core/src/main/java/com/localagent/core/observation/ObservationDomainModels.kt`
   - **SHA-256:** `382e7b122c3338ff82b0800ea1b758be90eef1e86b20e5543c38f6ec9a79144d`
   - **Description:** Original ObservationNode, ObservationBounds, and ObservationSnapshot models.
