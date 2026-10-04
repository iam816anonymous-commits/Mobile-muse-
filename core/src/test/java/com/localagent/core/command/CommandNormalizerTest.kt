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
    fun testUnknownCommandReturnsUnknownCommandResult() {
        val result1 = normalizer.parseInput("not real cmd", CommandSource.CONSOLE)
        assertTrue(result1 is CommandParseResult.UnknownCommand)
        assertEquals("not real cmd", result1.rawInput)

        val result2 = normalizer.parseInput("xyz abc 123", CommandSource.CONSOLE)
        assertTrue(result2 is CommandParseResult.UnknownCommand)
        assertEquals("xyz abc 123", result2.rawInput)
    }

    @Test
    fun testKnownCommandsRecognized() {
        val backResult = normalizer.parseInput("back", CommandSource.CONSOLE)
        assertTrue(backResult is CommandParseResult.Success)
        assertEquals(ActionType.GLOBAL_BACK, backResult.command.actionType)

        val homeResult = normalizer.parseInput("home", CommandSource.CONSOLE)
        assertTrue(homeResult is CommandParseResult.Success)
        assertEquals(ActionType.GLOBAL_HOME, homeResult.command.actionType)

        val clickResult = normalizer.parseInput("click 7", CommandSource.CONSOLE)
        assertTrue(clickResult is CommandParseResult.Success)
        assertEquals(ActionType.UI_CLICK, clickResult.command.actionType)

        val launchResult = normalizer.parseInput("launch Settings", CommandSource.CONSOLE)
        assertTrue(launchResult is CommandParseResult.Success)
        assertEquals(ActionType.APP_LAUNCH, launchResult.command.actionType)
    }

    @Test
    fun testEmptyAndWhitespaceInputHandling() {
        val emptyResult = normalizer.parseInput("", CommandSource.CONSOLE)
        assertTrue(emptyResult is CommandParseResult.InvalidInput)

        val whitespaceResult = normalizer.parseInput("   ", CommandSource.CONSOLE)
        assertTrue(whitespaceResult is CommandParseResult.InvalidInput)
    }

    @Test
    fun testMalformedClickOrLaunchReturnsUnknownCommand() {
        val malformedClick = normalizer.parseInput("click ", CommandSource.CONSOLE)
        assertTrue(malformedClick is CommandParseResult.UnknownCommand)

        val malformedLaunch = normalizer.parseInput("launch ", CommandSource.CONSOLE)
        assertTrue(malformedLaunch is CommandParseResult.UnknownCommand)
    }
}
