package com.localagent.app.ui

import com.localagent.app.LocalAgentApplication
import com.localagent.core.command.CommandNormalizer
import com.localagent.core.command.CommandParseResult
import com.localagent.core.command.CommandSource
import com.localagent.core.command.ActionType
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainActivityTest {

    private lateinit var app: LocalAgentApplication
    private lateinit var normalizer: CommandNormalizer

    @Before
    fun setUp() {
        app = LocalAgentApplication()
        app.initializeCoreDomain()
        normalizer = CommandNormalizer()
    }

    @Test
    fun testCoreDomainInitialization() {
        assertNotNull(app.capabilityRegistry)
        assertNotNull(app.policyEngine)
        assertNotNull(app.goalDispatcher)

        assertTrue(app.capabilityRegistry.isCapabilitySupported("UI_CLICK", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("GLOBAL_BACK", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("UI_SCROLL_FORWARD", deviceApi = 27))
    }

    @Test
    fun testUnknownCommandRejectedAtNormalizerLayer() {
        val parseResult = normalizer.parseInput("not real cmd", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.UnknownCommand)

        // Verify unknown command does not enter GoalDispatcher queue
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }

    @Test
    fun testRecognizedGrammarParsedAndDispatched() {
        val scrollUp = normalizer.parseInput("scroll up", CommandSource.CONSOLE)
        assertTrue(scrollUp is CommandParseResult.Success)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, scrollUp.command.actionType)

        app.goalDispatcher.enqueueCommand(scrollUp.command, priority = 1)
        assertEquals(1, app.goalDispatcher.getQueueSize())

        val polled = app.goalDispatcher.pollNextCommandForExecution()
        assertNotNull(polled)
        assertEquals(ActionType.UI_SCROLL_BACKWARD, polled.command.actionType)
        app.goalDispatcher.completeExecution()
    }

    @Test
    fun testAllPhase1CommandsSupportedByRegistry() {
        val commandsToTest = listOf(
            "back" to ActionType.GLOBAL_BACK,
            "home" to ActionType.GLOBAL_HOME,
            "recents" to ActionType.GLOBAL_RECENTS,
            "click 7" to ActionType.UI_CLICK,
            "long click 7" to ActionType.UI_LONG_CLICK,
            "scroll down" to ActionType.UI_SCROLL_FORWARD,
            "scroll up" to ActionType.UI_SCROLL_BACKWARD,
            "launch Settings" to ActionType.APP_LAUNCH
        )

        for ((input, expectedType) in commandsToTest) {
            val parseResult = normalizer.parseInput(input, CommandSource.CONSOLE)
            assertTrue(parseResult is CommandParseResult.Success, "Failed for input: $input")
            assertEquals(expectedType, parseResult.command.actionType)
        }
    }
}
