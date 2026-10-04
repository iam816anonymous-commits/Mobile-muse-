# OBSERVATION_MODEL.md — UI Observation Engine & Snapshot Model

## 1. Executive Summary & Core Architectural Rules

The **Observation Subsystem** serves as the agent's primary visual and structural sensing engine. It converts raw Android system `AccessibilityNodeInfo` tree structures into lightweight, immutable, serializable **`ObservationSnapshot`** objects.

### Fundamental Observation Rules
1. **Single-Root Capture Protocol:** Acquire `rootInActiveWindow` **EXACTLY ONCE** per snapshot cycle. Extract package, activity, window ID, and node primitives, then recycle every acquired node reference immediately. **NEVER call `rootInActiveWindow` a second time during snapshot construction.**
2. **Immediate Node Recycling:** Call `.recycle()` on every retrieved `AccessibilityNodeInfo` instance in `finally` blocks to prevent native C++ memory leaks on Android 8.1 (API 27).
3. **Stable Node Identity vs Instance ID:** Distinguish `nodeIdentity` (stable cross-snapshot matching key based on resource ID, class, bounds, text, content description, and structural path) from `observationInstanceId` (ephemeral snapshot-specific counter).
4. **Complete Primitive Bounds Extraction:** Always extract complete bounding boxes via `node.getBoundsInScreen(rect)` into `RectPrimitive(left, top, right, bottom)`.

---

## 2. Snapshot Data Structure & Node Identity Model

```kotlin
data class ObservationSnapshot(
    val snapshotId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val windowId: Int,
    val totalNodeCount: Int,
    val nodes: List<NodePrimitive>
)

data class NodePrimitive(
    val observationInstanceId: Int, // Ephemeral index within this snapshot
    val nodeIdentity: String,       // Stable matching key across snapshots
    val parentInstanceId: Int?,
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
    val isVisibleToUser: Boolean,
    val structuralPath: String      // Path in UI tree, e.g., "0/1/3/2"
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

### Node Identity Matching Algorithm (`nodeIdentity`)
To track UI nodes across screen updates without relying on ephemeral counters:
```kotlin
fun generateNodeIdentity(
    packageName: String,
    viewId: String?,
    className: String,
    text: String?,
    contentDescription: String?,
    bounds: RectPrimitive,
    structuralPath: String
): String {
    // 1. Primary match: Stable resource ID + Package
    if (!viewId.isNull_or_empty()) {
        return "$packageName:$viewId"
    }
    // 2. Secondary match: Class + Content Description + Bounds
    if (!contentDescription.isNull_or_empty()) {
        return "$className:cd='$contentDescription':${bounds.left},${bounds.top}"
    }
    // 3. Tertiary match: Class + Text + Bounds
    if (!text.isNull_or_empty()) {
        return "$className:txt='$text':${bounds.left},${bounds.top}"
    }
    // 4. Structural fallback: Class + Structural Path + Bounds
    return "$className:path=$structuralPath:${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
}
```

---

## 3. Single-Root Capture & Recycling Pipeline Implementation

```kotlin
class ObservationSnapshotGenerator(private val service: AccessibilityService) {

    fun captureSnapshot(): ObservationSnapshot {
        // Step 1: Acquire single root node reference ONCE
        val rootNode = service.rootInActiveWindow
            ?: return ObservationSnapshot.empty()

        val primitives = mutableListOf<NodePrimitive>()
        var instanceCounter = 0
        val extractedPackageName = rootNode.packageName?.toString() ?: "unknown"
        val extractedWindowId = rootNode.windowId

        try {
            // Step 2: Traverse tree, extract primitives, and recycle nodes
            traverseAndExtract(
                node = rootNode,
                parentInstanceId = null,
                path = "0",
                packageName = extractedPackageName,
                primitives = primitives,
                idGenerator = { instanceCounter++ }
            )
        } finally {
            // Step 3: Guaranteed recycling of root node
            rootNode.recycle()
        }

        // Step 4: Construct immutable ObservationSnapshot (no live AccessibilityNodeInfo retained)
        return ObservationSnapshot(
            packageName = extractedPackageName,
            windowId = extractedWindowId,
            totalNodeCount = primitives.size,
            nodes = primitives
        )
    }

    private fun traverseAndExtract(
        node: AccessibilityNodeInfo,
        parentInstanceId: Int?,
        path: String,
        packageName: String,
        primitives: MutableList<NodePrimitive>,
        idGenerator: () -> Int
    ) {
        val currentInstanceId = idGenerator()

        // Extract bounding box via Android Rect API
        val androidRect = android.graphics.Rect()
        node.getBoundsInScreen(androidRect)
        val boundsPrimitive = RectPrimitive(
            left = androidRect.left,
            top = androidRect.top,
            right = androidRect.right,
            bottom = androidRect.bottom
        )

        val viewId = node.viewIdResourceName
        val className = node.className?.toString() ?: ""
        val text = node.text?.toString()
        val cd = node.contentDescription?.toString()

        val stableIdentity = generateNodeIdentity(
            packageName = packageName,
            viewId = viewId,
            className = className,
            text = text,
            contentDescription = cd,
            bounds = boundsPrimitive,
            structuralPath = path
        )

        val primitive = NodePrimitive(
            observationInstanceId = currentInstanceId,
            nodeIdentity = stableIdentity,
            parentInstanceId = parentInstanceId,
            viewIdResourceName = viewId,
            className = className,
            text = text,
            contentDescription = cd,
            boundsInScreen = boundsPrimitive,
            isClickable = node.isClickable,
            isLongClickable = node.isLongClickable,
            isScrollable = node.isScrollable,
            isEditable = node.isEditable,
            isCheckable = node.isCheckable,
            isChecked = node.isChecked,
            isEnabled = node.isEnabled,
            isFocused = node.isFocused,
            isSelected = node.isSelected,
            isVisibleToUser = node.isVisibleToUser,
            structuralPath = path
        )

        primitives.add(primitive)

        // Traverse children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                traverseAndExtract(
                    node = child,
                    parentInstanceId = currentInstanceId,
                    path = "$path/$i",
                    packageName = packageName,
                    primitives = primitives,
                    idGenerator = idGenerator
                )
            } finally {
                child.recycle() // Recycle child node immediately after processing
            }
        }
    }
}
```

---

## 4. Snapshot Diffing & State-Diff Model

The **`SnapshotDiffEngine`** compares two `ObservationSnapshot` instances (`PreState` vs `PostState`) to compute a comprehensive state-diff result.

```kotlin
data class SnapshotDiffResult(
    val hasObservableChange: Boolean,
    val packageChanged: Boolean,
    val windowChanged: Boolean,
    val addedNodeIdentities: List<String>,
    val removedNodeIdentities: List<String>,
    val textChanges: List<TextDiff>,
    val boundsChanges: List<BoundsDiff>,
    val visibilityChanges: List<VisibilityDiff>,
    val scrollContainerChanges: List<ScrollContainerDiff>,
    val checkedStateChanges: List<CheckedStateDiff>,
    val focusChanges: List<FocusDiff>
)

data class TextDiff(val nodeIdentity: String, val oldText: String?, val newText: String?)
data class BoundsDiff(val nodeIdentity: String, val oldBounds: RectPrimitive, val newBounds: RectPrimitive)
data class VisibilityDiff(val nodeIdentity: String, val wasVisible: Boolean, val isVisible: Boolean)
data class ScrollContainerDiff(val containerIdentity: String, val shiftedNodeCount: Int)
data class CheckedStateDiff(val nodeIdentity: String, val wasChecked: Boolean, val isChecked: Boolean)
data class FocusDiff(val nodeIdentity: String, val gainedFocus: Boolean)
```
