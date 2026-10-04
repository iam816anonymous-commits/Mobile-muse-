package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.command.CommandNormalizer
import com.localagent.core.command.CommandParseResult
import com.localagent.core.command.CommandSource
import com.localagent.core.command.ActionType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class MainActivityTest {

    private lateinit var app: LocalAgentApplication
    private lateinit var normalizer: CommandNormalizer

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext<LocalAgentApplication>()
        app.initializeCoreDomain()
        normalizer = CommandNormalizer()
    }

    @Test
    fun testCoreDomainInitialization() {
        assertNotNull(app.capabilityRegistry)
        assertNotNull(app.policyEngine)
        assertNotNull(app.goalDispatcher)
        assertNotNull(app.eventRepository)
        assertNotNull(app.eventLogger)

        assertTrue(app.capabilityRegistry.isCapabilitySupported("UI_CLICK", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("GLOBAL_BACK", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("UI_SCROLL_FORWARD", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("OBSERVE", deviceApi = 27))
        assertTrue(app.capabilityRegistry.isCapabilitySupported("AGENT_STATUS", deviceApi = 27))
    }

    @Test
    fun testUnknownCommandRejectedAtNormalizerLayer() {
        val parseResult = normalizer.parseInput("not real cmd", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.UnknownCommand)

        // Verify unknown command does not enter GoalDispatcher queue
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }

    @Test
    fun testEmptyInputReturnsInvalidInput() {
        val parseResult = normalizer.parseInput("  ", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.InvalidInput)

        // Verify invalid input does not enter GoalDispatcher queue
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
    fun testAllCanonicalCommandsSupportedByGrammarAndRegistry() {
        val commandsToTest = listOf(
            "back" to ActionType.GLOBAL_BACK,
            "home" to ActionType.GLOBAL_HOME,
            "recents" to ActionType.GLOBAL_RECENTS,
            "click 7" to ActionType.UI_CLICK,
            "long click 7" to ActionType.UI_LONG_CLICK,
            "scroll down" to ActionType.UI_SCROLL_FORWARD,
            "scroll forward" to ActionType.UI_SCROLL_FORWARD,
            "scroll up" to ActionType.UI_SCROLL_BACKWARD,
            "scroll backward" to ActionType.UI_SCROLL_BACKWARD,
            "observe" to ActionType.OBSERVE,
            "observe current" to ActionType.OBSERVE,
            "test observe" to ActionType.OBSERVE,
            "status" to ActionType.AGENT_STATUS,
            "action status" to ActionType.AGENT_STATUS,
            "launch Settings" to ActionType.APP_LAUNCH
        )

        for ((input, expectedType) in commandsToTest) {
            val parseResult = normalizer.parseInput(input, CommandSource.CONSOLE)
            assertTrue(parseResult is CommandParseResult.Success, "Failed for input: $input")
            assertEquals(expectedType, parseResult.command.actionType)
            assertTrue(app.capabilityRegistry.isCapabilitySupported(expectedType.name, deviceApi = 27))
        }
    }
}
