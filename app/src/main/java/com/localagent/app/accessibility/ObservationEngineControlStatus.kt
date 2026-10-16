package com.localagent.app.accessibility

enum class ObservationEngineState {
    IDLE,
    OBSERVING,
    STOPPING,
    STOPPED,
    UNAVAILABLE
}

data class ObservationEngineControlStatus(
    val currentUiState: ObservationEngineState = ObservationEngineState.IDLE,
    val externalAppState: ObservationEngineState = ObservationEngineState.IDLE,
    val isAccessibilityBound: Boolean = false,
    val lastStateChangeTimestamp: Long = System.currentTimeMillis()
)
