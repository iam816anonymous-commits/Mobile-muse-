package com.localagent.app.ui

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.app.accessibility.AccessibilityLifecycleState
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.system.PermissionManager
import com.localagent.app.system.PermissionStatus
import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.result.ResultCode
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAccessibilityService
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = LocalAgentApplication::class)
class CommandConsoleAccessibilityStateTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        AgentAccessibilityService.resetForTest(app)
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest(app)
    }

    @Test
    fun `CC-ACCESS-001 - Service bound yields capability AVAILABLE`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        assertTrue(AgentAccessibilityService.isBound)
        assertEquals(
            AccessibilityLifecycleState.BOUND,
            app.accessibilityConnectionMonitor.connectionInfo.state
        )
    }

    @Test
    fun `CC-ACCESS-002 - Service disconnected yields capability UNAVAILABLE`() {
        AgentAccessibilityService.resetForTest(app)

        assertFalse(AgentAccessibilityService.isBound)
        assertNull(AgentAccessibilityService.INSTANCE)
        assertEquals(
            AccessibilityLifecycleState.DEGRADED,
            app.accessibilityConnectionMonitor.connectionInfo.state
        )
    }

    @Test
    fun `CC-ACCESS-003 - Command Console reads AVAILABLE after service connection`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        assertTrue(AgentAccessibilityService.isBound)

        // Execute command and verify it does NOT report ACCESSIBILITY_UNAVAILABLE
        activity.executeCommandFromInput("back")
        val resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()

        assertFalse(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
        assertTrue(resultText.contains("GLOBAL_BACK"))
    }

    @Test
    fun `CC-ACCESS-004 - Command Console does not retain stale UNAVAILABLE after service becomes bound`() {
        // Start unbound
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        activity.executeCommandFromInput("back")
        var resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()
        assertTrue(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))

        // Bind service dynamically
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        // Execute command again and verify state transition
        activity.executeCommandFromInput("back")
        resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()

        assertFalse(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
        assertTrue(resultText.contains("GLOBAL_BACK"))
    }

    @Test
    fun `CC-ACCESS-005 - Activity recreation preserves and re-reads authoritative capability state`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()

        // Recreate activity
        activityController.recreate()
        val recreatedActivity = activityController.get()

        recreatedActivity.executeCommandFromInput("back")
        val resultText = recreatedActivity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()

        assertFalse(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
        assertTrue(resultText.contains("GLOBAL_BACK"))
    }

    @Test
    fun `CC-ACCESS-006 - Permission Center and Command Console resolve the same runtime capability`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val permManager = app.permissionManager
        val permDesc = permManager.checkAccessibilityPermission()

        assertEquals(PermissionStatus.SPECIAL_ACCESS_GRANTED, permDesc.status)
        assertTrue(permDesc.isGranted)
        assertTrue(AgentAccessibilityService.isBound)
    }

    @Test
    fun `CC-ACCESS-007 - Unavailable capability blocks command execution safely`() {
        AgentAccessibilityService.resetForTest(app)

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        activity.executeCommandFromInput("click submit")
        val resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()

        assertTrue(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
        assertTrue(resultText.contains("Accessibility Service unbound"))
    }

    @Test
    fun `CC-ACCESS-008 - Available capability allows command execution to reach Phase 7 executor`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val shadowService = shadowOf(service as AccessibilityService)

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        activity.executeCommandFromInput("back")
        val resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()

        assertFalse(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_BACK, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `CC-ACCESS-009 - No second or stale service instance is used for capability detection`() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        val activeInstance = AgentAccessibilityService.INSTANCE
        assertSame(service, activeInstance)

        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        activity.executeCommandFromInput("home")
        val shadowService = shadowOf(service as AccessibilityService)

        assertEquals(1, shadowService.globalActionsPerformed.size)
        assertEquals(AccessibilityService.GLOBAL_ACTION_HOME, shadowService.globalActionsPerformed[0])
    }

    @Test
    fun `CC-ACCESS-010 - Service rebind transitions UNAVAILABLE to AVAILABLE correctly`() {
        val activityController = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = activityController.get()

        // 1. Unbound initially
        activity.executeCommandFromInput("recents")
        var resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()
        assertTrue(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))

        // 2. Bind service
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()
        service.onServiceConnectedForTest()

        // 3. Command succeeds
        activity.executeCommandFromInput("recents")
        resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()
        assertFalse(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
        assertTrue(resultText.contains("GLOBAL_RECENTS"))

        // 4. Disconnect service
        AgentAccessibilityService.resetForTest(app)

        // 5. Command blocked again
        activity.executeCommandFromInput("recents")
        resultText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvLatestResult).text.toString()
        assertTrue(resultText.contains(ResultCode.ACCESSIBILITY_UNAVAILABLE.name))
    }
}
