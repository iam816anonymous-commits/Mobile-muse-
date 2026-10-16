package com.localagent.core.execution

import com.localagent.core.command.NormalizedCommand
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

class ExecutionLock {
    private val isLocked = AtomicBoolean(false)

    fun tryAcquire(): Boolean {
        return isLocked.compareAndSet(false, true)
    }

    fun release() {
        isLocked.set(false)
    }

    fun isCurrentlyLocked(): Boolean {
        return isLocked.get()
    }
}

data class QueuedCommand(
    val command: NormalizedCommand,
    val priority: Int = 1 // Lower number = higher priority
) : Comparable<QueuedCommand> {
    override fun compareTo(other: QueuedCommand): Int {
        val priorityComparison = this.priority.compareTo(other.priority)
        if (priorityComparison != 0) return priorityComparison
        return this.command.timestamp.compareTo(other.command.timestamp)
    }
}

class GoalDispatcher {
    private val commandQueue = PriorityBlockingQueue<QueuedCommand>()
    private val executionLock = ExecutionLock()

    fun enqueueCommand(command: NormalizedCommand, priority: Int = 1): Boolean {
        return commandQueue.offer(QueuedCommand(command, priority))
    }

    fun peekNextCommand(): QueuedCommand? {
        return commandQueue.peek()
    }

    fun pollNextCommandForExecution(): QueuedCommand? {
        if (!executionLock.tryAcquire()) {
            return null // Execution channel currently locked
        }
        val next = commandQueue.poll()
        if (next == null) {
            executionLock.release()
            return null
        }
        return next
    }

    fun completeExecution() {
        executionLock.release()
    }

    fun getQueueSize(): Int {
        return commandQueue.size
    }

    fun clearQueue() {
        commandQueue.clear()
    }

    fun isExecutionChannelLocked(): Boolean {
        return executionLock.isCurrentlyLocked()
    }
}
