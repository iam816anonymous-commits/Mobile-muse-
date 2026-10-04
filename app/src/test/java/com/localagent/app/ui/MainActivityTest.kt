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
    }

    @Test
    fun testUnknownCommandRejectedAtNormalizerLayer() {
        val parseResult = normalizer.parseInput("not real cmd", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.UnknownCommand)

        // Verify unknown command does not enter GoalDispatcher queue
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }

    @Test
    fun testKnownCommandParsedAndDispatched() {
        val parseResult = normalizer.parseInput("back", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.Success)
        assertEquals(ActionType.GLOBAL_BACK, parseResult.command.actionType)

        app.goalDispatcher.enqueueCommand(parseResult.command, priority = 1)
        assertEquals(1, app.goalDispatcher.getQueueSize())

        val polled = app.goalDispatcher.pollNextCommandForExecution()
        assertNotNull(polled)
        assertEquals(ActionType.GLOBAL_BACK, polled.command.actionType)
        assertEquals(0, app.goalDispatcher.getQueueSize())
        app.goalDispatcher.completeExecution()
    }
}
