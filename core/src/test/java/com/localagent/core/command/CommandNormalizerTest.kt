package com.localagent.core.command

import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandNormalizerTest {

    private lateinit var normalizer: CommandNormalizer

    @Before
    fun setUp() {
        normalizer = CommandNormalizer()
    }

    @Test
    fun testValidNavigationCommands() {
        val back = normalizer.parseInput("back", CommandSource.CONSOLE)
        assertTrue(back is CommandParseResult.Success)
        assertEquals(ActionType.GLOBAL_BACK, back.command.actionType)

        val home = normalizer.parseInput("home", CommandSource.CONSOLE)
        assertTrue(home is CommandParseResult.Success)
        assertEquals(ActionType.GLOBAL_HOME, home.command.actionType)

        val recents = normalizer.parseInput("recents", CommandSource.CONSOLE)
        assertTrue(recents is CommandParseResult.Success)
        assertEquals(ActionType.GLOBAL_RECENTS, recents.command.actionType)
    }

    @Test
    fun testValidClickAndLongClickCommands() {
        val click = normalizer.parseInput("click 7", CommandSource.CONSOLE)
        assertTrue(click is CommandParseResult.Success)
        assertEquals(ActionType.UI_CLICK, click.command.actionType)
        assertEquals(TargetSelector.ByText("7"), click.command.targetSelector)

        val longClick = normalizer.parseInput("long click 7", CommandSource.CONSOLE)
        assertTrue(longClick is CommandParseResult.Success)
        assertEquals(ActionType.UI_LONG_CLICK, longClick.command.actionType)
        assertEquals(TargetSelector.ByText("7"), longClick.command.targetSelector)
    }

    @Test
    fun testValidScrollCommands() {
        val scrollUp = normalizer.parseInput("scroll up", CommandSource.CONSOLE)
        assertTrue(scrollUp is CommandParseResult.Success)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, scrollUp.command.actionType)

        val scrollBackward = normalizer.parseInput("scroll backward", CommandSource.CONSOLE)
        assertTrue(scrollBackward is CommandParseResult.Success)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, scrollBackward.command.actionType)

        val scrollDown = normalizer.parseInput("scroll down", CommandSource.CONSOLE)
        assertTrue(scrollDown is CommandParseResult.Success)
        assertEquals(ActionType.UI_SCROLL_FORWARD, scrollDown.command.actionType)

        val scrollForward = normalizer.parseInput("scroll forward", CommandSource.CONSOLE)
        assertTrue(scrollForward is CommandParseResult.Success)
        assertEquals(ActionType.UI_SCROLL_FORWARD, scrollForward.command.actionType)
    }

    @Test
    fun testValidObserveAndStatusCommands() {
        val observe1 = normalizer.parseInput("observe", CommandSource.CONSOLE)
        assertTrue(observe1 is CommandParseResult.Success)
        assertEquals(ActionType.OBSERVE, observe1.command.actionType)

        val observe2 = normalizer.parseInput("observe current", CommandSource.CONSOLE)
        assertTrue(observe2 is CommandParseResult.Success)
        assertEquals(ActionType.OBSERVE, observe2.command.actionType)

        val observe3 = normalizer.parseInput("test observe", CommandSource.CONSOLE)
        assertTrue(observe3 is CommandParseResult.Success)
        assertEquals(ActionType.OBSERVE, observe3.command.actionType)

        val status1 = normalizer.parseInput("status", CommandSource.CONSOLE)
        assertTrue(status1 is CommandParseResult.Success)
        assertEquals(ActionType.AGENT_STATUS, status1.command.actionType)

        val status2 = normalizer.parseInput("action status", CommandSource.CONSOLE)
        assertTrue(status2 is CommandParseResult.Success)
        assertEquals(ActionType.AGENT_STATUS, status2.command.actionType)
    }

    @Test
    fun testValidLaunchCommand() {
        val launch = normalizer.parseInput("launch Settings", CommandSource.CONSOLE)
        assertTrue(launch is CommandParseResult.Success)
        assertEquals(ActionType.APP_LAUNCH, launch.command.actionType)
        assertEquals("Settings", launch.command.parameters["appLabel"])
    }

    @Test
    fun testMultiActionCommandSequences() {
        // 1. "launch calculator and click 7"
        val launchAndClick = normalizer.parseInput("launch calculator and click 7", CommandSource.CONSOLE)
        assertTrue(launchAndClick is CommandParseResult.Success)
        assertEquals(2, launchAndClick.sequence.size)
        assertEquals(ActionType.APP_LAUNCH, launchAndClick.sequence[0].actionType)
        assertEquals("calculator", launchAndClick.sequence[0].parameters["appLabel"])
        assertEquals(ActionType.UI_CLICK, launchAndClick.sequence[1].actionType)
        assertEquals(TargetSelector.ByText("7"), launchAndClick.sequence[1].targetSelector)

        // 2. "open calculator, then tap 7, and click 8"
        val threeStep = normalizer.parseInput("open calculator, then tap 7, and click 8", CommandSource.CONSOLE)
        assertTrue(threeStep is CommandParseResult.Success)
        assertEquals(3, threeStep.sequence.size)
        assertEquals(ActionType.APP_LAUNCH, threeStep.sequence[0].actionType)
        assertEquals("calculator", threeStep.sequence[0].parameters["appLabel"])
        assertEquals(ActionType.UI_CLICK, threeStep.sequence[1].actionType)
        assertEquals(TargetSelector.ByText("7"), threeStep.sequence[1].targetSelector)
        assertEquals(ActionType.UI_CLICK, threeStep.sequence[2].actionType)
        assertEquals(TargetSelector.ByText("8"), threeStep.sequence[2].targetSelector)

        // 3. "open settings and scroll down"
        val launchAndScroll = normalizer.parseInput("open settings and scroll down", CommandSource.CONSOLE)
        println("DEBUG launchAndScroll: $launchAndScroll")
        assertTrue(launchAndScroll is CommandParseResult.Success)
        assertEquals(2, launchAndScroll.sequence.size)
        assertEquals(ActionType.APP_LAUNCH, launchAndScroll.sequence[0].actionType)
        assertEquals("settings", launchAndScroll.sequence[0].parameters["appLabel"])
        assertEquals(ActionType.UI_SCROLL_FORWARD, launchAndScroll.sequence[1].actionType)
    }

    @Test
    fun testUnknownCommandsReturnUnknownCommand() {
        val unknown1 = normalizer.parseInput("not real cmd", CommandSource.CONSOLE)
        assertTrue(unknown1 is CommandParseResult.UnknownCommand)
        assertEquals("not real cmd", unknown1.rawInput)

        val unknown2 = normalizer.parseInput("xyz abc 123", CommandSource.CONSOLE)
        assertTrue(unknown2 is CommandParseResult.UnknownCommand)

        val missingClickTarget = normalizer.parseInput("click", CommandSource.CONSOLE)
        assertTrue(missingClickTarget is CommandParseResult.UnknownCommand)

        val missingLongClickTarget = normalizer.parseInput("long click", CommandSource.CONSOLE)
        assertTrue(missingLongClickTarget is CommandParseResult.UnknownCommand)

        val missingScrollDir = normalizer.parseInput("scroll", CommandSource.CONSOLE)
        assertTrue(missingScrollDir is CommandParseResult.UnknownCommand)

        val missingLaunchApp = normalizer.parseInput("launch", CommandSource.CONSOLE)
        assertTrue(missingLaunchApp is CommandParseResult.UnknownCommand)
    }

    @Test
    fun testWhitespaceAndCaseNormalization() {
        val spaces = normalizer.parseInput("   click   7   ", CommandSource.CONSOLE)
        assertTrue(spaces is CommandParseResult.Success)
        assertEquals(ActionType.UI_CLICK, spaces.command.actionType)

        val uppercase = normalizer.parseInput("SCROLL DOWN", CommandSource.CONSOLE)
        assertTrue(uppercase is CommandParseResult.Success)
        assertEquals(ActionType.UI_SCROLL_FORWARD, uppercase.command.actionType)
    }

    @Test
    fun testEmptyInputHandlingReturnsInvalidInput() {
        val empty = normalizer.parseInput("", CommandSource.CONSOLE)
        assertTrue(empty is CommandParseResult.InvalidInput)

        val whitespace = normalizer.parseInput("    ", CommandSource.CONSOLE)
        assertTrue(whitespace is CommandParseResult.InvalidInput)
    }
}
