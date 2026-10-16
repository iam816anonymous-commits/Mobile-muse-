package com.localagent.core.lifecycle

enum class GoalState {
    CREATED,      // Goal received and parsed
    PLANNING,     // Goal decomposed into sequential NormalizedCommands
    READY,        // Sub-commands queued in GoalDispatcher
    EXECUTING,    // Active command dispatched to device
    WAITING,      // Settle delay or post-action observation capture
    VERIFYING,    // Action-specific VerificationStrategy evaluating UI state diff
    COMPLETED,    // Goal criteria satisfied and verified
    FAILED,       // Sub-step failed or state diff unverified after retries
    CANCELLED,    // Explicitly cancelled by user via Overlay/Console
    INTERRUPTED,  // Interrupted by Low Memory Killer (LMK) process termination
    RECOVERING    // Process restarted; re-evaluating active window to resume
}

data class GoalVerification(
    val goalId: String,
    val isGoalSatisfied: Boolean,
    val details: String = "",
    val verificationTimestamp: Long = System.currentTimeMillis()
)

enum class TaskState {
    CREATED,
    QUEUED,
    RUNNING,
    WAITING,
    PAUSED,
    CANCELLING,
    CANCELLED,
    FAILED,
    COMPLETED,
    INTERRUPTED,
    RECOVERING
}

class TaskLifecycle(
    val taskId: String,
    initialState: TaskState = TaskState.CREATED
) {
    var currentState: TaskState = initialState
        private set

    private val validTransitions = mapOf(
        TaskState.CREATED to setOf(TaskState.QUEUED, TaskState.CANCELLED),
        TaskState.QUEUED to setOf(TaskState.RUNNING, TaskState.CANCELLED, TaskState.INTERRUPTED),
        TaskState.RUNNING to setOf(TaskState.WAITING, TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLING, TaskState.INTERRUPTED),
        TaskState.WAITING to setOf(TaskState.RUNNING, TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELLING, TaskState.INTERRUPTED),
        TaskState.PAUSED to setOf(TaskState.QUEUED, TaskState.CANCELLED),
        TaskState.CANCELLING to setOf(TaskState.CANCELLED),
        TaskState.INTERRUPTED to setOf(TaskState.RECOVERING, TaskState.FAILED),
        TaskState.RECOVERING to setOf(TaskState.QUEUED, TaskState.FAILED)
    )

    fun transitionTo(newState: TaskState): Boolean {
        val allowedStates = validTransitions[currentState] ?: emptySet()
        if (newState in allowedStates) {
            currentState = newState
            return true
        }
        return false
    }
}
