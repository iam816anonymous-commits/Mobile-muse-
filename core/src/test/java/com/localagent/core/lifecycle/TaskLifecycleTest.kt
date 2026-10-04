package com.localagent.core.lifecycle

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TaskLifecycleTest {

    @Test
    fun testLegalTaskStateTransitions() {
        val task = TaskLifecycle(taskId = "task-101")
        assertEquals(TaskState.CREATED, task.currentState)

        assertTrue(task.transitionTo(TaskState.QUEUED))
        assertEquals(TaskState.QUEUED, task.currentState)

        assertTrue(task.transitionTo(TaskState.RUNNING))
        assertEquals(TaskState.RUNNING, task.currentState)

        assertTrue(task.transitionTo(TaskState.COMPLETED))
        assertEquals(TaskState.COMPLETED, task.currentState)
    }

    @Test
    fun testIllegalTaskStateTransitionsBlocked() {
        val task = TaskLifecycle(taskId = "task-102")

        // Direct CREATED -> COMPLETED is illegal
        assertFalse(task.transitionTo(TaskState.COMPLETED))
        assertEquals(TaskState.CREATED, task.currentState)
    }

    @Test
    fun testLmkInterruptionAndRecoveryTransitions() {
        val task = TaskLifecycle(taskId = "task-103")

        task.transitionTo(TaskState.QUEUED)
        task.transitionTo(TaskState.RUNNING)

        // Process killed by LMK while running
        assertTrue(task.transitionTo(TaskState.INTERRUPTED))
        assertEquals(TaskState.INTERRUPTED, task.currentState)

        // App restarts and recovers
        assertTrue(task.transitionTo(TaskState.RECOVERING))
        assertEquals(TaskState.RECOVERING, task.currentState)

        // Re-queued to resume
        assertTrue(task.transitionTo(TaskState.QUEUED))
        assertEquals(TaskState.QUEUED, task.currentState)
    }
}
