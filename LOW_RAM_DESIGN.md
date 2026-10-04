# LOW_RAM_DESIGN.md — Low-RAM & Low-Storage Architectural Design

## 1. Executive Summary & Primary Mandate

Operating effectively on entry-level Android devices—specifically **Android 8.1 / API level 27 devices with 1 GB to 2 GB total RAM**—is a **primary baseline requirement** of **LocalAgent**.

Low-RAM compatibility is not an optimization phase added at the end; it dictates every architectural choice from day one.

---

## 2. Hard Anti-Patterns (Explicitly Forbidden)

To guarantee low-RAM stability, the codebase strictly forbids:

1. **NO Permanently Resident LLMs or ML Frameworks:** Large models must never be kept loaded in memory.
2. **NO Retained Accessibility Trees:** Holding `AccessibilityNodeInfo` objects across execution bounds causes catastrophic native C++ memory leaks on API 27.
3. **NO Continuous Polling Timers:** No busy loops, `Handler` polling, or high-frequency background observers.
4. **NO Continuous Screen Capture or Background OCR:** Screen capture and OCR run strictly on-demand.
5. **NO Unbounded Logs in RAM:** All logging streams directly to disk via buffered SQLite transactions.
6. **NO Unnecessary Background Services:** Services run in foreground mode only when active tasks are executing and unbind immediately when idle.
7. **NO Duplicate Execution / UI Frameworks:** No Jetpack Compose overhead; uses light Android View system for overlay and console UI.

---

## 3. Low-RAM Architecture Principles

```text
                     ┌───────────────────────────────────────────────┐
                     │               ResourceManager                 │
                     │  Monitors RAM, Battery, Storage, Thermal State│
                     └───────────────────────┬───────────────────────┘
                                             │
                                             ▼
                     ┌───────────────────────────────────────────────┐
                     │            Agent Execution Lifecycle          │
                     │   IDLE ◄──► ACTIVE ◄──► LOW_MEMORY_DEGRADED   │
                     └───────────────────────┬───────────────────────┘
                                             │
                                             ▼
                     ┌───────────────────────────────────────────────┐
                     │            Low-RAM Operating Rules            │
                     │  1. Event-Driven Execution Only               │
                     │  2. Immediate Node Recycling (.recycle())     │
                     │  3. Bounded Primitive Snapshot Objects        │
                     │  4. Lazy Initialization of Speech & Storage   │
                     │  5. Bounded SQLite DB & Automated Log Purge   │
                     └───────────────────────────────────────────────┘
```

### 3.1 Immediate `AccessibilityNodeInfo` Recycling Protocol
On Android 8.1 (API 27), every call to `rootInActiveWindow` or `node.getChild(i)` allocates native C++ memory backing structures. To prevent native memory exhaustion:
- Every retrieved `AccessibilityNodeInfo` must be processed into a primitive `NodePrimitive` data object.
- `.recycle()` must be called explicitly in `finally` blocks on every node reference.

### 3.2 Operating State & Memory Footprint Targets

| Agent State | System Activity | Target Heap Footprint | Target CPU Usage | Background Services |
|---|---|---|---|---|
| **IDLE** | Listening for Accessibility events or user input | **< 15 MB** | **~0%** | Accessibility Service bound (passive) |
| **EXECUTING** | Target resolution, action dispatch, snapshot diff | **< 35 MB** | **5% – 15%** | Active execution thread running |
| **VOICE_ACTIVE** | STT recording or TTS speech synthesis | **< 45 MB** | **10% – 25%** | Speech recognizer active; destroyed immediately when speech ends |
| **LOW_MEMORY_DEGRADED**| System low-memory warning (`onTrimMemory()`) | **< 10 MB** | **< 2%** | Clear all in-memory caches, flush log buffers to disk |

---

## 4. `ResourceManager` Subsystem

The **`ResourceManager`** monitors device system health and enforces memory trimming:

```kotlin
class ResourceManager(private val context: Context) : ComponentCallbacks2 {

    enum class MemoryState {
        NORMAL,
        MODERATE,
        LOW,
        CRITICAL
    }

    private var currentMemoryState = MemoryState.NORMAL

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                currentMemoryState = MemoryState.MODERATE
                evictCaches(aggressive = false)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                currentMemoryState = MemoryState.CRITICAL
                evictCaches(aggressive = true)
                System.gc() // Hint GC during critical memory pressure
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                currentMemoryState = MemoryState.CRITICAL
                releaseNonEssentialResources()
            }
        }
    }

    private fun evictCaches(aggressive: Boolean) {
        // Clear snapshot diff caches, command history caches, and temporary UI primitives
    }

    private fun releaseNonEssentialResources() {
        // Shutdown TTS engine, release STT recognizer, flush log buffers to SQLite
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    override fun onLowMemory() {
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
```

---

## 5. Low-Storage Management & Storage Budgets

To protect low-storage Android devices (e.g., 8 GB or 16 GB internal storage):

### Storage Budget Allocations
- **App Storage Directory:** `/data/data/com.localagent.app/`
- **Total Storage Cap:** **30 MB Maximum** across all logs, databases, and evidence files.
- **Event DB Cap:** 20 MB max (Auto-purged via `LogRetentionManager`).
- **Memory & Workflow DB Cap:** 5 MB max.
- **Evidence / Diagnostic Snapshots:** 5 MB max (Max 10 snapshot JSON dumps; oldest deleted when threshold met).

---

## 6. Process Recovery after Low Memory Killer (LMK) Termination

On 1 GB RAM Android 8.1 devices, Android's Low Memory Killer (LMK) may terminate the `LocalAgent` app process while in the background.

### LMK Survival & Recovery Protocol
1. **Zero State in RAM:** All task states, command queues, and execution logs are committed synchronously to SQLite before waiting for UI transitions.
2. **Service Binding Recovery:** When Android restarts `AgentAccessibilityService` after process kill, `onServiceConnected()` queries `logs/agent-events.db` for incomplete tasks.
3. **Graceful Resume:** If an active workflow was interrupted by LMK death:
   - Mark interrupted task as `TASK_INTERRUPTED_BY_LMK`.
   - Capture fresh live `ObservationSnapshot`.
   - Verify current foreground package and prompt user or safely resume workflow execution.
