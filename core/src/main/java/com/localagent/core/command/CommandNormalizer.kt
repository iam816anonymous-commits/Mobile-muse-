package com.localagent.core.command

sealed class CommandParseResult {
    data class Success(
        val command: NormalizedCommand,
        val sequence: List<NormalizedCommand> = listOf(command)
    ) : CommandParseResult()
    data class UnknownCommand(val rawInput: String, val reason: String = "Unrecognized command syntax") : CommandParseResult()
    data class InvalidInput(val reason: String = "Command input cannot be empty") : CommandParseResult()
}

class CommandNormalizer {

    fun parseInput(input: String, source: CommandSource): CommandParseResult {
        val trimmed = input.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isEmpty()) {
            return CommandParseResult.InvalidInput()
        }

        // Split multi-action input by conjunctions/delimiters: " and ", " then ", " and then ", ", ", "; ", ". "
        val segments = splitCommandSegments(trimmed)

        if (segments.isEmpty()) {
            return CommandParseResult.InvalidInput()
        }

        val parsedCommands = mutableListOf<NormalizedCommand>()

        for (segment in segments) {
            val singleResult = parseSingleSegment(segment, source)
            when (singleResult) {
                is CommandParseResult.Success -> parsedCommands.add(singleResult.command)
                is CommandParseResult.UnknownCommand -> {
                    return singleResult
                }
                is CommandParseResult.InvalidInput -> {
                    return singleResult
                }
            }
        }

        if (parsedCommands.isEmpty()) {
            return CommandParseResult.UnknownCommand(trimmed)
        }

        return CommandParseResult.Success(
            command = parsedCommands.first(),
            sequence = parsedCommands
        )
    }

    private fun splitCommandSegments(input: String): List<String> {
        val delimiterRegex = Regex("(?i)\\s*(?:,?\\s*(?:and\\s+then|then|and)\\b|,|;|\\.)\\s*")
        val rawSegments = input.split(delimiterRegex)
        return rawSegments.map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun parseSingleSegment(rawSegment: String, source: CommandSource): CommandParseResult {
        var trimmed = rawSegment.trim()
        var lower = trimmed.lowercase()

        // Strip leading conjunction words if left over from splitting
        if (lower.startsWith("and then ")) {
            trimmed = trimmed.substring(9).trim()
            lower = trimmed.lowercase()
        } else if (lower.startsWith("then ")) {
            trimmed = trimmed.substring(5).trim()
            lower = trimmed.lowercase()
        } else if (lower.startsWith("and ")) {
            trimmed = trimmed.substring(4).trim()
            lower = trimmed.lowercase()
        }

        return when {
            // Navigation
            lower == "back" || lower == "go back" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.GLOBAL_BACK
                )
            )
            lower == "home" || lower == "go home" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.GLOBAL_HOME
                )
            )
            lower == "recents" || lower == "recent apps" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.GLOBAL_RECENTS
                )
            )

            // App Launch / Open
            lower.startsWith("launch ") || lower.startsWith("open ") || lower.startsWith("start ") -> {
                val spaceIdx = trimmed.indexOf(' ')
                val appLabel = trimmed.substring(spaceIdx + 1).trim()
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
            lower == "launch" || lower == "open" || lower == "start" ->
                CommandParseResult.UnknownCommand(trimmed, "Launch command requires an application label parameter")

            // Clicks & Taps
            lower.startsWith("click ") || lower.startsWith("tap ") || lower.startsWith("press ") || lower.startsWith("select ") -> {
                val spaceIdx = trimmed.indexOf(' ')
                var target = trimmed.substring(spaceIdx + 1).trim()
                val lowerTarget = target.lowercase()
                if (lowerTarget.startsWith("the ")) {
                    target = target.substring(4).trim()
                } else if (lowerTarget.startsWith("on ")) {
                    target = target.substring(3).trim()
                }

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
            lower == "click" || lower == "tap" || lower == "press" ->
                CommandParseResult.UnknownCommand(trimmed, "Click command requires a target parameter")

            // Long Clicks
            lower.startsWith("long click ") || lower.startsWith("long press ") || lower.startsWith("hold ") -> {
                val lastSpace = trimmed.lastIndexOf(' ')
                val target = trimmed.substring(lastSpace + 1).trim()
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

            // Text Input
            lower.startsWith("type ") || lower.startsWith("input ") || lower.startsWith("enter ") || lower.startsWith("set text ") -> {
                val spaceIdx = trimmed.indexOf(' ')
                val text = trimmed.substring(spaceIdx + 1).trim()
                if (text.isEmpty()) {
                    CommandParseResult.UnknownCommand(trimmed, "Type command requires a text parameter")
                } else {
                    CommandParseResult.Success(
                        NormalizedCommand(
                            source = source,
                            actionType = ActionType.UI_TEXT_INPUT,
                            parameters = mapOf("text" to text)
                        )
                    )
                }
            }

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

            // Observation & System Status
            lower == "observe" || lower == "observe current" || lower == "test observe" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.OBSERVE
                )
            )
            lower == "status" || lower == "action status" -> CommandParseResult.Success(
                NormalizedCommand(
                    source = source,
                    actionType = ActionType.AGENT_STATUS
                )
            )

            else -> CommandParseResult.UnknownCommand(trimmed)
        }
    }
}
