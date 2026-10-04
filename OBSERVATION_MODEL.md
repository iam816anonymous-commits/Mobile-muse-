# OBSERVATION_MODEL.md — UI Observation Engine & Snapshot Model

## 1. Executive Summary & Low-RAM Constraints

The **Observation Subsystem** serves as the agent's primary visual and structural sensing engine. It converts raw Android system `AccessibilityNodeInfo` tree structures into lightweight, immutable, serializable **`ObservationSnapshot`** objects.

### Critical Low-RAM Rules
1. **Never Retain Live Node Trees:** Holding `AccessibilityNodeInfo` trees causes severe memory leaks and `StaleStateException` crashes.
2. **Immediate Node Recycling:** Every `AccessibilityNodeInfo` fetched from `rootInActiveWindow` must have `.recycle()` called immediately after primitive extraction on API levels 27–29.
3. **Bounded Primitive Storage:** Extract only essential primitive fields (`text`, `contentDescription`, `viewId`, `className`, `bounds`, `flags`).
4. **On-Demand Capture:** Capture UI snapshots only when an action requires target resolution or post-action verification—no continuous background tree polling.

---

## 2. Snapshot Data Structure

```kotlin
data class ObservationSnapshot(
    val snapshotId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val activityName: String?,
    val windowId: Int,
    val totalNodeCount: Int,
    val nodes: List<NodePrimitive>,
    val isSystemWindow: Boolean = false
)

data class NodePrimitive(
    val nodeId: Int,
    val parentNodeId: Int?,
    val viewIdResourceName: String?,
    val className: String,
    val text: String?,
    val contentDescription: String?,
    val boundsInScreen: RectPrimitive,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val isFocused: Boolean,
    val isSelected: Boolean,
    val isVisibleToUser: Boolean
)

data class RectPrimitive(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
}
```

---

## 3. Observation Capture & Node Recycling Pipeline

```kotlin
class ObservationSnapshotGenerator(private val service: AccessibilityService) {

    fun captureSnapshot(): ObservationSnapshot {
        val rootNode = service.rootInActiveWindow
            ?: return ObservationSnapshot.empty()

        val primitives = mutableListOf<NodePrimitive>()
        var counter = 0

        try {
            traverseAndExtract(rootNode, parentId = null, primitives = primitives, idGenerator = { counter++ })
        } finally {
            // Explicitly recycle root node to free native C++ AccessibilityNodeInfo allocations on API 27
            rootNode.recycle()
        }

        val foregroundPackage = primitives.firstOrNull()?.className ?: service.packageName.toString()

        return ObservationSnapshot(
            packageName = service.rootInActiveWindow?.packageName?.toString() ?: "unknown",
            activityName = null, // Extracted via UsageStats or AccessibilityEvent
            windowId = service.rootInActiveWindow?.windowId ?: -1,
            totalNodeCount = primitives.size,
            nodes = primitives
        )
    }

    private fun traverseAndExtract(
        node: AccessibilityNodeInfo,
        parentId: Int?,
        primitives: MutableList<NodePrimitive>,
        idGenerator: () -> Int
    ) {
        val currentId = idGenerator()

        val primitive = NodePrimitive(
            nodeId = currentId,
            parentNodeId = parentId,
            viewIdResourceName = node.viewIdResourceName,
            className = node.className?.toString() ?: "",
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            boundsInScreen = RectPrimitive(
                left = 0, top = 0, right = 0, bottom = 0
            ).also { rect ->
                val androidRect = android.graphics.Rect()
                node.getBoundsInScreen(androidRect)
                rect.apply {
                    // Populate primitive rect
                }
            },
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            isCheckable = node.isCheckable,
            isChecked = node.isChecked,
            isEnabled = node.isEnabled,
            isFocused = node.isFocused,
            isSelected = node.isSelected,
            isVisibleToUser = node.isVisibleToUser
        )

        primitives.add(primitive)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                traverseAndExtract(child, currentId, primitives, idGenerator)
            } finally {
                child.recycle() // Recycle child node immediately after processing
            }
        }
    }
}
```

---

## 4. Snapshot Diffing & Verification Engine

The **`SnapshotDiffEngine`** compares two `ObservationSnapshot` instances (`PreState` vs `PostState`) captured before and after an action dispatch to determine whether an observable UI state change occurred.

```kotlin
data class SnapshotDiffResult(
    val hasObservableChange: Boolean,
    val packageChanged: Boolean,
    val activeWindowChanged: Boolean,
    val textChangedNodes: List<TextDiff>,
    val addedNodesCount: Int,
    val removedNodesCount: Int,
    val checkedStateToggled: Boolean
)

data class TextDiff(
    val nodeId: Int,
    val viewId: String?,
    val oldText: String?,
    val newText: String?
)
```

### State-Diff Rules
1. **Package / Window Diff:** If `PreState.packageName != PostState.packageName` or `PreState.windowId != PostState.windowId`, `packageChanged = true` (Verifies `APP_LAUNCH`, `GLOBAL_HOME`, `GLOBAL_BACK`).
2. **Text Diff:** Iterate nodes by View ID or bounds; if target text modified, `textChangedNodes.add(...)` (Verifies `UI_TEXT_INPUT`, calculator display updates).
3. **Checked State Diff:** If target node `isChecked` toggled from false to true, `checkedStateToggled = true` (Verifies switch/checkbox toggle).
4. **Node Count / Layout Diff:** If `addedNodesCount > 0` or `removedNodesCount > 0`, UI structure changed (Verifies list scrolling, popups, menu dialogs).
