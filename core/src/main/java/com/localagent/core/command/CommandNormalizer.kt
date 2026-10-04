package com.localagent.core.command

sealed class CommandParseResult {
    data class Success(val command: NormalizedCommand) : CommandParseResult()
    data class UnknownCommand(val rawInput: String, val reason: String = "Unrecognized command syntax") : CommandParseResult()
    data class InvalidInput(val reason: String = "Command input cannot be empty") : CommandParseResult()
}

class CommandNormalizer {

    fun parseInput(input: String, source: CommandSource): CommandParseResult {
        val trimmed = input.trim().replaceAll("\\s+", " ")
        if (trimmed.isEmpty()) {
            return CommandParseResult.InvalidInput()
        }

        val lower = trimmed.lowercase()

        return when {
            // Navigation
            lower == "back" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.GLOBAL_BACK
                )
            )
            lower == "home" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.GLOBAL_HOME
                )
            )
            lower == "recents" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.GLOBAL_RECENTS
                )
            )

            // Clicks
            lower.startsWith("click ") -> {
                val target = trimmed.substring(6).trim()
                if (target.isEmpty()) {
                    CommandParseResult.UnknownCommand(trimmed, "Click command requires a target parameter")
                } else {
                    CommandParseResult.Success(
                        NormalizedCommand(
                            source = source,
                            actionType = ActionType.UI_CLICK,
                            targetSelector = TargetSelector.ByText(target)
                        )
                    )
                }
            }
            lower == "click" -> CommandParseResult.UnknownCommand(trimmed, "Click command requires a target parameter")

            lower.startsWith("long click ") -> {
                val target = trimmed.substring(11).trim()
                if (target.isEmpty()) {
                    CommandParseResult.UnknownCommand(trimmed, "Long click command requires a target parameter")
                } else {
                    CommandParseResult.Success(
                        NormalizedCommand(
                            source = source,
                            actionType = ActionType.UI_LONG_CLICK,
                            targetSelector = TargetSelector.ByText(target)
                        )
                    )
                }
            }
            lower == "long click" -> CommandParseResult.UnknownCommand(trimmed, "Long click command requires a target parameter")

            // Scrolling
            lower == "scroll down" || lower == "scroll forward" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.UI_SCROLL_FORWARD
                )
            )
            lower == "scroll up" || lower == "scroll backward" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.UI_SCROLL_BACKWARD
                )
            )
            lower == "scroll" -> CommandParseResult.UnknownCommand(trimmed, "Scroll command requires a direction parameter (e.g. scroll up, scroll down)")

            // Observation & Diagnostics
            lower == "observe" || lower == "observe current" || lower == "test observe" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.UI_SCROLL_FORWARD,
                    parameters = mapOf("intent" to "OBSERVE")
                )
            )
            lower == "status" || lower == "action status" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.UI_SCROLL_FORWARD,
                    parameters = mapOf("intent" to "STATUS")
                )
            )

            // App Launch
            lower.startsWith("launch ") -> {
                val appLabel = trimmed.substring(7).trim()
                if (appLabel.isEmpty()) {
                    CommandParseResult.UnknownCommand(trimmed, "Launch command requires an application label parameter")
                } else {
                    CommandParseResult.Success(
                        NormalizedCommand(
                            source = source,
                            actionType = ActionType.APP_LAUNCH,
                            parameters = mapOf("appLabel" to appLabel)
                        )
                    )
                }
            }
            lower == "launch" -> CommandParseResult.UnknownCommand(trimmed, "Launch command requires an application label parameter")

            else -> CommandParseResult.UnknownCommand(trimmed)
        }
    }

    private fun String.replaceAll(regex: String, replacement: String): String {
        return this.replace(Regex(regex), replacement)
    }
}
