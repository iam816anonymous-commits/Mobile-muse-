# OBSERVATION_MODEL.md — UI Observation Engine & Snapshot Model

## 1. Executive Summary & Core Architectural Rules

The **Observation Subsystem** serves as the agent's primary visual and structural sensing engine. It converts raw Android system `AccessibilityNodeInfo` tree structures into lightweight, immutable, serializable **`ObservationSnapshot`** objects.

### Fundamental Observation Rules
1. **Single-Root Capture Protocol:** Acquire `rootInActiveWindow` **EXACTLY ONCE** per snapshot cycle. Extract package, activity, window ID, and node primitives, then recycle every acquired node reference immediately. **NEVER call `rootInActiveWindow` a second time during snapshot construction.**
2. **Immediate Node Recycling:** Call `.recycle()` on every retrieved `AccessibilityNodeInfo` instance in `finally` blocks to prevent native C++ memory leaks on Android 8.1 (API 27).
3. **Snapshot-Local Metadata Disclaimer:** `observationInstanceId` and `parentInstanceId` are **ephemeral, snapshot-local metadata ONLY**. They MUST NEVER be used as cross-snapshot node identities or persistent targets.
4. **Identity Confidence Model:** Every node primitive is assigned a composite identity and a `NodeIdentityConfidence` rating (`EXACT`, `HIGH`, `MEDIUM`, `LOW`, `EPHEMERAL`).
5. **Complete Primitive Bounds Extraction:** Always extract complete bounding boxes via `node.getBoundsInScreen(rect)` into `RectPrimitive(left, top, right, bottom)`.

---

## 2. Node Identity Confidence Model & Composite Matching

Target matching across UI state updates cannot rely solely on a single attribute like Resource ID or text. LocalAgent uses a composite identity strategy and classifies identity confidence explicitly.

```kotlin
enum class NodeIdentityConfidence {
    EXACT,     // Unique resource ID in package OR unique content description
    HIGH,      // Resource ID + Class + Bounds OR Text + Class + Structural Path
    MEDIUM,    // Text + Class + Approximate Bounds OR Class + Parent Context + Index
    LOW,       // Class + Structural Path fallback in dynamic container
    EPHEMERAL  // Unidentifiable dynamic node (e.g., transient loading indicator)
}

data class NodeIdentity(
    val identityKey: String,
    val confidence: NodeIdentityConfidence,
    val matchStrategy: IdentityMatchStrategy
)

enum class IdentityMatchStrategy {
    RESOURCE_ID_PACKAGE,
    RESOURCE_ID_CLASS_BOUNDS,
    CONTENT_DESC_CLASS_BOUNDS,
    TEXT_CLASS_PATH,
    PARENT_INDEX_PATH_FALLBACK,
    EPHEMERAL_UNMATCHED
}
```

### Composite Node Identity Generation Protocol
```kotlin
fun generateNodeIdentity(
    packageName: String,
    viewId: String?,
    className: String,
    text: String?,
    contentDescription: String?,
    bounds: RectPrimitive,
    structuralPath: String,
    parentClass: String?,
    siblingIndex: Int
): NodeIdentity {
    // 1. EXACT Match: Unique Resource ID within Package
    if (!viewId.isNullOrEmpty()) {
        return NodeIdentity(
            identityKey = "$packageName:$viewId",
            confidence = NodeIdentityConfidence.EXACT,
            matchStrategy = IdentityMatchStrategy.RESOURCE_ID_PACKAGE
        )
    }

    // 2. HIGH Match: Content Description + Class + Bounds
    if (!contentDescription.isNullOrEmpty()) {
        return NodeIdentity(
            identityKey = "$className:cd='$contentDescription':${bounds.left},${bounds.top}",
            confidence = NodeIdentityConfidence.HIGH,
            matchStrategy = IdentityMatchStrategy.CONTENT_DESC_CLASS_BOUNDS
        )
    }

    // 3. HIGH Match: Text + Class + Structural Path
    if (!text.isNullOrEmpty()) {
        return NodeIdentity(
            identityKey = "$className:txt='$text':path=$structuralPath",
            confidence = NodeIdentityConfidence.HIGH,
            matchStrategy = IdentityMatchStrategy.TEXT_CLASS_PATH
        )
    }

    // 4. MEDIUM Match: Parent Context + Sibling Index + Class
    if (!parentClass.isNullOrEmpty()) {
        return NodeIdentity(
            identityKey = "$parentClass/[$siblingIndex]::$className:${bounds.width}x${bounds.height}",
            confidence = NodeIdentityConfidence.MEDIUM,
            matchStrategy = IdentityMatchStrategy.PARENT_INDEX_PATH_FALLBACK
        )
    }

    // 5. LOW / EPHEMERAL Fallback
    return NodeIdentity(
        identityKey = "$className:path=$structuralPath:${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
        confidence = NodeIdentityConfidence.LOW,
        matchStrategy = IdentityMatchStrategy.EPHEMERAL_UNMATCHED
    )
}
```

### Handling Complex UI Containers
1. **RecyclerView / ListView / GridView Items:**
   - Items in scrollable containers lack unique View IDs for individual cells.
   - Matching uses `ContainerIdentity` + `RelativeChildPath` + `ChildText/ContentDescription`.
   - When scrolled, off-screen nodes are destroyed by Android framework; newly visible nodes are matched as new instances with `HIGH` or `MEDIUM` confidence based on text/content description.
2. **Repeated Nodes (e.g., Calculator Digit Buttons or Lists):**
   - Disambiguated using `text` or `contentDescription` combined with `bounds` center coordinates.
3. **Dynamic / Animated UIs:**
   - During active layout animations, node bounds mutate continuously.
   - Nodes are evaluated using `text` and `viewId` while ignoring exact bounds during animation settle windows.
4. **Behavior on LOW Confidence or Unsafe Matches:**
   - If a target node matches with `LOW` or `EPHEMERAL` confidence, `TargetResolver` aborts direct dispatch and attempts **Semantic Re-resolution** (re-scanning active window for text or parent container).
   - If match remains ambiguous, action returns `ResultCode.TARGET_NOT_FOUND` or requests fresh observation snapshot rather than risking mis-clicking an incorrect node.

---

## 3. Snapshot Data Structures

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
    val observationInstanceId: Int, // Ephemeral index within THIS snapshot ONLY (Local Metadata)
    val parentInstanceId: Int?,     // Ephemeral parent index within THIS snapshot ONLY (Local Metadata)
    val identity: NodeIdentity,      // Stable composite matching key & confidence rating
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
    val structuralPath: String
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

## 4. Single-Root Capture & Recycling Pipeline Implementation

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
                parentClass = null,
                siblingIndex = 0,
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
        parentClass: String?,
        siblingIndex: Int,
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

        val compositeIdentity = generateNodeIdentity(
            packageName = packageName,
            viewId = viewId,
            className = className,
            text = text,
            contentDescription = cd,
            bounds = boundsPrimitive,
            structuralPath = path,
            parentClass = parentClass,
            siblingIndex = siblingIndex
        )

        val primitive = NodePrimitive(
            observationInstanceId = currentInstanceId,
            parentInstanceId = parentInstanceId,
            identity = compositeIdentity,
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
                    parentClass = className,
                    siblingIndex = i,
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

## 5. Snapshot Diffing & State-Diff Model

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

data class TextDiff(val nodeIdentityKey: String, val oldText: String?, val newText: String?)
data class BoundsDiff(val nodeIdentityKey: String, val oldBounds: RectPrimitive, val newBounds: RectPrimitive)
data class VisibilityDiff(val nodeIdentityKey: String, val wasVisible: Boolean, val isVisible: Boolean)
data class ScrollContainerDiff(
    val containerIdentityKey: String,
    val newlyVisibleChildIdentities: List<String>,
    val disappearedChildIdentities: List<String>,
    val shiftedChildIdentities: List<String>,
    val verticalScrollDeltaPx: Int,
    val isAtBoundary: Boolean
)
data class CheckedStateDiff(val nodeIdentityKey: String, val wasChecked: Boolean, val isChecked: Boolean)
data class FocusDiff(val nodeIdentityKey: String, val gainedFocus: Boolean)
```
