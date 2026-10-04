package com.localagent.core.command

sealed class CommandParseResult {
    data class Success(val command: NormalizedCommand) : CommandParseResult()
    data class UnknownCommand(val rawInput: String, val reason: String = "Unrecognized command syntax") : CommandParseResult()
    data class InvalidInput(val reason: String = "Command input cannot be empty") : CommandParseResult()
}

class CommandNormalizer {

    fun parseInput(input: String, source: CommandSource): CommandParseResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return CommandParseResult.InvalidInput()
        }

        val lower = trimmed.lowercase()

        return when {
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
            lower == "observe" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.UI_SCROLL_FORWARD, // Mapped for observation trigger request
                    parameters = mapOf("intent" to "OBSERVE")
                )
            )
            lower.startsWith("click ") -> {
                val target = trimmed.substring(6).trim()
                if (target.isEmpty()) {
                    CommandParseResult.UnknownCommand(trimmed, "Click command requires target parameter")
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
            lower.startsWith("launch ") -> {
                val appLabel = trimmed.substring(7).trim()
                if (appLabel.isEmpty()) {
                    CommandParseResult.UnknownCommand(trimmed, "Launch command requires application label parameter")
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
            else -> CommandParseResult.UnknownCommand(trimmed)
        }
    }
}
