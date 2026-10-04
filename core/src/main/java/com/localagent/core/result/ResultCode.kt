package com.localagent.core.result

enum class ResultCode {
    SUCCESS_VERIFIED,
    DISPATCHED_BUT_NOT_VERIFIED,
    NO_EFFECT_EXPECTED,
    NO_SCROLL_POSSIBLE,
    ACTION_FAILED,
    TARGET_NOT_FOUND,
    TARGET_NOT_ACTIONABLE,
    TARGET_STALE,
    ACCESSIBILITY_UNAVAILABLE,
    PERMISSION_REQUIRED,
    POLICY_BLOCKED,
    CAPABILITY_UNAVAILABLE,
    UNKNOWN_COMMAND,
    TIMEOUT,
    CANCELLED,
    INTERRUPTED,
    RECOVERY_REQUIRED
}

sealed class VerificationResult {
    object SuccessVerified : VerificationResult()
    data class DispatchedButNotVerified(val reason: String) : VerificationResult()
    data class NoScrollPossible(val reason: String) : VerificationResult()
    data class Failed(val reason: String) : VerificationResult()
}
