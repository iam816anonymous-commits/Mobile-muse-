package com.localagent.app.ui

import android.content.Intent
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.core.command.CommandNormalizer
import com.localagent.core.command.CommandParseResult
import com.localagent.core.command.CommandSource
import com.localagent.core.command.TargetSelector
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class FloatingConsoleTest {

    private val normalizer = CommandNormalizer()

    @Before
    fun setUp() {
        AgentAccessibilityService.resetForTest()
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest()
    }

    @Test
    fun `FLOATING-CONSOLE-001 - click 7 parses into TargetSelector ByText 7`() {
        val parseResult = normalizer.parseInput("click 7", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.Success)
        val cmd = (parseResult as CommandParseResult.Success).command
        assertEquals(com.localagent.core.command.ActionType.UI_CLICK, cmd.actionType)
        assertTrue(cmd.targetSelector is TargetSelector.ByText)
        assertEquals("7", (cmd.targetSelector as TargetSelector.ByText).text)
    }

    @Test
    fun `FLOATING-CONSOLE-002 - invalid click command returns UnknownCommand`() {
        val parseResult = normalizer.parseInput("click", CommandSource.CONSOLE)
        assertTrue(parseResult is CommandParseResult.UnknownCommand)
    }

    @Test
    fun `FLOATING-CONSOLE-003 - FloatingConsoleService lifecycle start and stop`() {
        val controller = Robolectric.buildService(FloatingConsoleService::class.java)
        val service = controller.create().startCommand(0, 1).get()
        assertNotNull(service)
        controller.destroy()
    }
}
