package com.localagent.core.command

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NormalizedCommandTest {

    @Test
    fun testNormalizedCommandCreationAndDefaults() {
        val command = NormalizedCommand(
            source = CommandSource.CONSOLE,
            actionType = ActionType.UI_CLICK,
            targetSelector = TargetSelector.ByViewId("com.google.android.calculator:id/digit_7")
        )

        assertNotNull(command.id)
        assertEquals(CommandSource.CONSOLE, command.source)
        assertEquals(ActionType.UI_CLICK, command.actionType)
        assertEquals(TargetSelector.ByViewId("com.google.android.calculator:id/digit_7"), command.targetSelector)
    }
}
