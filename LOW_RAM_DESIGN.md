# LOW_RAM_DESIGN.md — Cross-Phase Low-RAM & Low-Storage Architectural Design

## 1. Executive Summary & Primary Mandate

Operating effectively on entry-level Android devices—specifically **Android 8.1 / API level 27 devices with 1 GB to 2 GB total RAM**—is a **primary baseline requirement** enforced **across all implementation phases** of **LocalAgent**.

Low-RAM compatibility is not a late optimization phase; it dictates every architectural choice from day one.

---

## 2. Hard Architectural Anti-Patterns (Explicitly Forbidden)

To guarantee low-RAM stability, the codebase strictly forbids:

1. **NO Permanently Resident LLMs or ML Frameworks:** Large models must never be kept loaded in memory.
2. **NO Retained Accessibility Trees:** Holding `AccessibilityNodeInfo` objects across execution bounds causes severe native C++ memory leaks on API 27.
3. **NO Continuous Polling Timers:** No busy loops, `Handler` polling, or high-frequency background observers.
4. **NO Continuous Screen Capture or Background OCR:** Screen capture and OCR run strictly on-demand.
5. **NO Unbounded Logs in RAM:** All logging streams directly to disk via buffered SQLite transactions in unified `agent.db`.
6. **NO Unnecessary Background Services:** Services run in passive mode or unbind immediately when idle.
7. **NO Routine `System.gc()` Calls in Core Architecture:** Do not rely on `System.gc()` calls for memory management; manage object lifecycles, clear references, and recycle native nodes deterministically.

---

## 3. Engineering Memory Targets & Memory Threshold States

Rather than guaranteeing arbitrary memory numbers, LocalAgent defines explicit engineering target thresholds. Memory behavior is evaluated against a reference **Android 8.1 / API 27 Low-RAM Test Profile (1.5 GB RAM hardware/emulator)**.

```kotlin
enum class SystemMemoryThreshold {
    TARGET,   // Normal execution within budget bounds
    WARNING,  // Moderate memory pressure (TRIM_MEMORY_RUNNING_MODERATE)
    CRITICAL  // Severe memory pressure (TRIM_MEMORY_RUNNING_CRITICAL / Low RAM)
}
```

### Engineering Memory Footprint Targets (API 27 Reference Profile)

| Agent State | System Activity | TARGET Heap | WARNING Threshold | CRITICAL Threshold |
|---|---|---|---|---|
| **IDLE** | Passive listening for Accessibility events or user input | **< 15 MB** | **15 MB – 25 MB** | **> 25 MB** (Flush caches, trim buffers) |
| **EXECUTING** | Target resolution, action dispatch, snapshot diff | **< 35 MB** | **35 MB – 50 MB** | **> 50 MB** (Drop diff history, recycle snapshot) |
| **VOICE_ACTIVE** | STT recording or TTS speech synthesis | **< 45 MB** | **45 MB – 60 MB** | **> 60 MB** (Destroy STT recognizer immediately) |
| **LOW_MEMORY_DEGRADED**| System low-memory callback (`onTrimMemory()`) | **< 10 MB** | **10 MB – 15 MB** | Clear all in-memory caches, commit `agent.db` WAL |

---

## 4. `ResourceManager` Memory Trimming Implementation

```kotlin
class ResourceManager(private val context: Context) : ComponentCallbacks2 {

    private var currentThreshold = SystemMemoryThreshold.TARGET

    override fun onTrimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                currentThreshold = SystemMemoryThreshold.WARNING
                evictCaches(aggressive = false)
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                currentThreshold = SystemMemoryThreshold.CRITICAL
                evictCaches(aggressive = true)
                releaseNonEssentialResources()
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                currentThreshold = SystemMemoryThreshold.CRITICAL
                releaseNonEssentialResources()
            }
        }
    }

    private fun evictCaches(aggressive: Boolean) {
        // Clear snapshot diff caches, command history caches, and temporary UI primitives
    }

    private fun releaseNonEssentialResources() {
        // Shutdown TTS engine, destroy STT recognizer, flush log buffers to SQLite
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}
    override fun onLowMemory() {
        onTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
```

---

## 5. Storage Budget, WAL Accounting & Durable Storage Partitioning

To protect low-storage Android devices (e.g., 8 GB or 16 GB internal storage):

### Storage Partitioning Architecture
1. **App-Private Operational Storage (`/data/data/com.localagent.app/files/agent/`):**
   - **Total Storage Cap:** **30 MB Maximum** across `agent.db`, `-wal`, `-shm`, and temporary evidence files.
   - **Unified DB Budget Formula:** `DB Total Size = FileSize("agent.db") + FileSize("agent.db-wal") + FileSize("agent.db-shm")`.
   - **WAL Checkpoint Policy:** Run `PRAGMA wal_checkpoint(TRUNCATE)` when `agent.db-wal` exceeds 5 MB.
2. **Durable Agent Memory Storage (`DurableMemoryStorageProvider`):**
   - **Location:** External public storage (`/sdcard/LocalAgent/memory`) or user-granted Storage Access Framework document trees.
   - **Total Storage Cap:** **10 MB Maximum** across JSON memory records and learned workflows.
   - **Uninstall Survival:** Independent of app-private directory; survives app uninstalls and reinstalls.

---

## 6. Process Recovery after Low Memory Killer (LMK) Termination

On 1 GB RAM Android 8.1 devices, Android's Low Memory Killer (LMK) may terminate the `LocalAgent` app process while in the background.

### LMK Survival & Recovery Protocol
1. **Zero State in RAM:** All task states, command queues, and execution logs are committed synchronously to SQLite `agent.db` before waiting for UI transitions.
2. **Service Binding Recovery:** When Android restarts `AgentAccessibilityService` after process kill, `onServiceConnected()` queries `agent.db` for incomplete tasks.
3. **Graceful Resume:** If an active workflow was interrupted by LMK death:
   - Mark interrupted task as `TASK_INTERRUPTED_BY_LMK`.
   - Capture fresh live `ObservationSnapshot`.
   - Verify current foreground package and prompt user or safely resume workflow execution.
