package com.localagent.core.command

enum class CommandSource {
    CONSOLE,
    OVERLAY,
    VOICE,
    AUTOMATION,
    AI,
    TEST_HARNESS,
    BROWSER
}

enum class ActionType {
    // Navigation
    GLOBAL_BACK,
    GLOBAL_HOME,
    GLOBAL_RECENTS,
    GLOBAL_NOTIFICATIONS,
    GLOBAL_QUICK_SETTINGS,
    GLOBAL_LOCK_SCREEN,
    GLOBAL_TAKE_SCREENSHOT,

    // UI Control
    UI_CLICK,
    UI_LONG_CLICK,
    UI_TEXT_INPUT,
    UI_SCROLL_FORWARD,
    UI_SCROLL_BACKWARD,

    // System
    APP_LAUNCH,
    APP_RESOLVE,
    OVERLAY_SHOW,
    OVERLAY_MOVE,

    // Hardware
    HARDWARE_VOLUME,
    HARDWARE_BRIGHTNESS,
    HARDWARE_FLASHLIGHT,
    HARDWARE_MEDIA,
    HARDWARE_WIFI_TOGGLE,

    // Speech
    SPEECH_STT,
    SPEECH_TTS,

    // Solver & Research
    STRUCTURED_PROBLEM_SOLVER,
    TRIP_RESEARCH_ENGINE,
    EXTERNAL_KNOWLEDGE_IMPORT
}

sealed class TargetSelector {
    data class ByViewId(val viewIdResourceName: String) : TargetSelector()
    data class ByText(val text: String, val exactMatch: Boolean = true) : TargetSelector()
    data class ByContentDescription(val contentDescription: String, val exactMatch: Boolean = true) : TargetSelector()
    data class ByNodeIdentityKey(val nodeIdentityKey: String) : TargetSelector()
    data class ByCoordinates(val x: Int, val y: Int) : TargetSelector()
    object None : TargetSelector()
}

data class NormalizedCommand(
    val id: String = java.util.UUID.randomUUID().toString(),
    val source: CommandSource,
    val actionType: ActionType,
    val targetSelector: TargetSelector = TargetSelector.None,
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
