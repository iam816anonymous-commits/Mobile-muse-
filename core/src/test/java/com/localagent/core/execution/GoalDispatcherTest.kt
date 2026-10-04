package com.localagent.core.execution

import com.localagent.core.command.ActionType
import com.localagent.core.command.CommandSource
import com.localagent.core.command.NormalizedCommand
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoalDispatcherTest {

    private lateinit var dispatcher: GoalDispatcher

    @Before
    fun setUp() {
        dispatcher = GoalDispatcher()
    }

    @Test
    fun testPriorityQueueOrdering() {
        val lowPriorityCmd = NormalizedCommand(
            source = CommandSource.AUTOMATION,
            actionType = ActionType.UI_SCROLL_FORWARD
        )
        val highPriorityCmd = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = ActionType.GLOBAL_BACK
        )

        dispatcher.enqueueCommand(lowPriorityCmd, priority = 3)
        dispatcher.enqueueCommand(highPriorityCmd, priority = 1)

        val next = dispatcher.peekNextCommand()
        assertNotNull(next)
        assertEquals(ActionType.GLOBAL_BACK, next.command.actionType)
    }

    @Test
    fun testExecutionLockExclusivity() {
        val cmd1 = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = ActionType.UI_CLICK
        )
        val cmd2 = NormalizedCommand(
            source = CommandSource.OVERLAY,
            actionType = ActionType.GLOBAL_HOME
        )

        dispatcher.enqueueCommand(cmd1, priority = 1)
        dispatcher.enqueueCommand(cmd2, priority = 1)

        val polled1 = dispatcher.pollNextCommandForExecution()
        assertNotNull(polled1)
        assertEquals(ActionType.UI_CLICK, polled1.command.actionType)
        assertTrue(dispatcher.isExecutionChannelLocked())

        // Concurrent attempt to poll while locked should return null
        val polled2 = dispatcher.pollNextCommandForExecution()
        assertNull(polled2)

        // Complete execution and unlock channel
        dispatcher.completeExecution()
        assertFalse(dispatcher.isExecutionChannelLocked())

        // Second poll should now succeed
        val polled3 = dispatcher.pollNextCommandForExecution()
        assertNotNull(polled3)
        assertEquals(ActionType.GLOBAL_HOME, polled3.command.actionType)
    }
}
