package com.localagent.app.accessibility

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class AccessibilityServiceConnectionMonitorTest {

    private lateinit var app: LocalAgentApplication
    private lateinit var monitor: AccessibilityServiceConnectionMonitor

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        monitor = app.accessibilityConnectionMonitor
    }

    @Test
    fun testInitialConnectionState() {
        assertNotNull(monitor.connectionInfo)
        // Initial state on Robolectric without service instance is UNBOUND or DEGRADED
        val state = monitor.connectionInfo.state
        assertTrue(state == AccessibilityLifecycleState.UNBOUND || state == AccessibilityLifecycleState.DEGRADED)
    }

    @Test
    fun testNotifyServiceConnectedTransition() {
        var notifiedInfo: ServiceConnectionInfo? = null
        val listener = object : AccessibilityServiceLifecycleListener {
            override fun onAccessibilityLifecycleStateChanged(info: ServiceConnectionInfo) {
                notifiedInfo = info
            }
        }

        monitor.addListener(listener)
        assertNotNull(notifiedInfo) // Immediately receives current state

        monitor.notifyServiceConnected()

        assertNotNull(notifiedInfo)
        assertEquals(AccessibilityLifecycleState.BOUND, notifiedInfo?.state)
        assertTrue(notifiedInfo?.connectedTimestampMs ?: 0L > 0L)

        monitor.removeListener(listener)
    }

    @Test
    fun testNotifyServiceDisconnectedTransition() {
        var notifiedInfo: ServiceConnectionInfo? = null
        val listener = object : AccessibilityServiceLifecycleListener {
            override fun onAccessibilityLifecycleStateChanged(info: ServiceConnectionInfo) {
                notifiedInfo = info
            }
        }

        monitor.addListener(listener)
        monitor.notifyServiceConnected()
        assertEquals(AccessibilityLifecycleState.BOUND, monitor.connectionInfo.state)

        monitor.notifyServiceDisconnected(reason = "TEST_UNBOUND")

        assertEquals(AccessibilityLifecycleState.DEGRADED, monitor.connectionInfo.state)
        assertEquals(AccessibilityLifecycleState.DEGRADED, notifiedInfo?.state)
        assertTrue(monitor.connectionInfo.disconnectedTimestampMs > 0L)

        monitor.removeListener(listener)
    }
}
