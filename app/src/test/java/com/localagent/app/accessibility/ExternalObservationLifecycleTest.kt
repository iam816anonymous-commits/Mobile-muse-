package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityEvent
import com.localagent.app.accessibility.ObservationEngineState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ExternalObservationLifecycleTest {

    private lateinit var service: AgentAccessibilityService

    @Before
    fun setUp() {
        service = AgentAccessibilityService()
        service.onServiceConnectedForTest()
    }

    @Test
    fun `P6-OBS-LIFE-001 - Start external observation and process event creates snapshot`() {
        service.externalObservationState = ObservationEngineState.OBSERVING

        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = "com.android.calculator2"
            className = "com.android.calculator2.Calculator"
        }

        service.onAccessibilityEvent(event)

        assertEquals("com.android.calculator2", service.lastExternalPackageName)
        assertEquals(ObservationEngineState.OBSERVING, service.externalObservationState)
    }

    @Test
    fun `P6-OBS-LIFE-002 - Stop observation rejects subsequent events`() {
        service.externalObservationState = ObservationEngineState.OBSERVING
        val initialSnap = service.lastExternalObservationSnapshot

        // Stop Observation
        service.externalObservationState = ObservationEngineState.STOPPED

        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
            packageName = "com.android.calculator2"
        }

        service.onAccessibilityEvent(event)

        // Verify last snapshot remains unchanged and no new snapshot was published
        assertEquals(initialSnap, service.lastExternalObservationSnapshot)
        assertEquals(ObservationEngineState.STOPPED, service.externalObservationState)
    }

    @Test
    fun `P6-OBS-LIFE-003 - Stop observation ignores multiple subsequent events`() {
        service.externalObservationState = ObservationEngineState.STOPPED

        for (i in 1..5) {
            val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED).apply {
                packageName = "com.android.calculator2"
            }
            service.onAccessibilityEvent(event)
        }

        assertEquals(ObservationEngineState.STOPPED, service.externalObservationState)
        assertNull(service.lastExternalObservationSnapshot)
    }

    @Test
    fun `P6-OBS-LIFE-004 - Start after Stop resumes normal event processing`() {
        service.externalObservationState = ObservationEngineState.STOPPED

        // Re-start Observation
        service.externalObservationState = ObservationEngineState.OBSERVING

        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED).apply {
            packageName = "com.android.calculator2"
            className = "com.android.calculator2.Calculator"
        }

        service.onAccessibilityEvent(event)

        assertEquals("com.android.calculator2", service.lastExternalPackageName)
        assertEquals(ObservationEngineState.OBSERVING, service.externalObservationState)
    }

    @Test
    fun `P6-OBS-LIFE-005 - Stopped session cannot publish snapshot from queued event`() {
        service.externalObservationState = ObservationEngineState.OBSERVING

        // Session stops while event is in flight
        service.externalObservationState = ObservationEngineState.STOPPED

        service.evaluateExternalApplicationWindows()

        assertNull(service.lastExternalObservationSnapshot)
    }

    @Test
    fun `P6-OBS-LIFE-006 - AccessibilityService remains bound after Stop Observation`() {
        service.externalObservationState = ObservationEngineState.OBSERVING
        assertTrue(AgentAccessibilityService.isBound)

        // User stops external observation session
        service.externalObservationState = ObservationEngineState.STOPPED

        // AccessibilityService remains active and bound
        assertTrue(AgentAccessibilityService.isBound)
        assertEquals(ObservationEngineState.STOPPED, service.externalObservationState)
    }
}
