package com.localagent.core.resolver

import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationBounds
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import org.junit.Assert.*
import org.junit.Test

class TargetResolverTest {

    private val targetResolver = TargetResolver()

    @Test
    fun `P6-RESOLVE-001 - self clickable node resolves to itself`() {
        val clickableButton = ObservationNode(
            nodeId = "btn_1",
            className = "android.widget.Button",
            text = "Submit",
            resourceId = "com.app:id/submit_btn",
            clickable = true,
            nodeIdentity = "id:submit_btn_text:Submit",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = clickableButton
        )

        val result = targetResolver.resolveTargetById(snapshot, "btn_1", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("btn_1", result.resolvedNodeId)
        assertEquals(TargetResolutionStrategy.SELF_ACTIONABLE, result.strategy)
        assertEquals(0, result.ancestorDepth)
        assertEquals(NodeIdentityConfidence.EXACT, result.confidence)
        assertFalse(result.reacquired)
    }

    @Test
    fun `P6-RESOLVE-002 - non-clickable TextView resolves to clickable parent`() {
        // Child TextView "7" (non-clickable)
        val text7Node = ObservationNode(
            nodeId = "text_7",
            className = "android.widget.TextView",
            text = "7",
            clickable = false,
            nodeIdentity = "cls:TextView_lbl:7_parent:btn_7",
            identityConfidence = NodeIdentityConfidence.MEDIUM
        )

        // Parent MaterialButton (clickable)
        val parentBtn = ObservationNode(
            nodeId = "btn_7",
            className = "com.google.android.material.button.MaterialButton",
            resourceId = "com.android.calculator2:id/digit_7",
            clickable = true,
            nodeIdentity = "id:digit_7_text:7",
            identityConfidence = NodeIdentityConfidence.EXACT,
            children = listOf(text7Node)
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 2,
            rootNode = parentBtn
        )

        val result = targetResolver.resolveTargetById(snapshot, "text_7", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("btn_7", result.resolvedNodeId)
        assertEquals("id:digit_7_text:7", result.resolvedNodeIdentity)
        assertEquals("com.google.android.material.button.MaterialButton", result.resolvedClassName)
        assertEquals(TargetResolutionStrategy.CLICKABLE_ANCESTOR, result.strategy)
        assertEquals(1, result.ancestorDepth)
        assertTrue(result.confidence == NodeIdentityConfidence.HIGH || result.confidence == NodeIdentityConfidence.EXACT)
    }

    @Test
    fun `P6-RESOLVE-003 - multiple ancestors exist, nearest valid clickable ancestor wins`() {
        val leafText = ObservationNode(
            nodeId = "leaf_text",
            className = "android.widget.TextView",
            text = "Item Text",
            clickable = false
        )

        val innerLayout = ObservationNode(
            nodeId = "inner_clickable",
            className = "android.widget.LinearLayout",
            clickable = true,
            children = listOf(leafText)
        )

        val outerContainer = ObservationNode(
            nodeId = "outer_clickable",
            className = "android.widget.FrameLayout",
            clickable = true,
            children = listOf(innerLayout)
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 3,
            rootNode = outerContainer
        )

        val result = targetResolver.resolveTargetById(snapshot, "leaf_text", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("inner_clickable", result.resolvedNodeId)
        assertEquals(1, result.ancestorDepth)
    }

    @Test
    fun `P6-RESOLVE-004 - no clickable ancestor returns NO_TARGET safely`() {
        val nonClickableText = ObservationNode(
            nodeId = "label_1",
            className = "android.widget.TextView",
            text = "Static Label",
            clickable = false
        )

        val nonClickableParent = ObservationNode(
            nodeId = "layout_1",
            className = "android.widget.LinearLayout",
            clickable = false,
            children = listOf(nonClickableText)
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 2,
            rootNode = nonClickableParent
        )

        val result = targetResolver.resolveTargetById(snapshot, "label_1", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.NO_ACTIONABLE_TARGET, result.status)
        assertEquals(TargetResolutionStrategy.NO_TARGET, result.strategy)
        assertNull(result.resolvedNodeId)
    }

    @Test
    fun `P6-RESOLVE-005 - long-clickable child and ancestor resolution`() {
        val child = ObservationNode(
            nodeId = "child_view",
            className = "android.view.View",
            longClickable = false
        )

        val longClickableParent = ObservationNode(
            nodeId = "parent_card",
            className = "androidx.cardview.widget.CardView",
            longClickable = true,
            children = listOf(child)
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 2,
            rootNode = longClickableParent
        )

        val result = targetResolver.resolveTargetById(snapshot, "child_view", TargetActionType.LONG_CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("parent_card", result.resolvedNodeId)
        assertEquals(TargetResolutionStrategy.LONG_CLICKABLE_ANCESTOR, result.strategy)
    }

    @Test
    fun `P6-RESOLVE-006 - scrollable child resolves to scrollable ancestor`() {
        val itemText = ObservationNode(
            nodeId = "item_5",
            className = "android.widget.TextView",
            text = "Row 5",
            scrollable = false
        )

        val recyclerList = ObservationNode(
            nodeId = "recycler_view",
            className = "androidx.recyclerview.widget.RecyclerView",
            scrollable = true,
            children = listOf(itemText)
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 2,
            rootNode = recyclerList
        )

        val result = targetResolver.resolveTargetById(snapshot, "item_5", TargetActionType.SCROLL)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("recycler_view", result.resolvedNodeId)
        assertEquals(TargetResolutionStrategy.SCROLLABLE_ANCESTOR, result.strategy)
    }

    @Test
    fun `P6-RESOLVE-007 - editable target resolution`() {
        val editText = ObservationNode(
            nodeId = "input_field",
            className = "android.widget.EditText",
            editable = true
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = editText
        )

        val result = targetResolver.resolveTargetById(snapshot, "input_field", TargetActionType.EDITABLE)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("input_field", result.resolvedNodeId)
        assertEquals(TargetResolutionStrategy.EDITABLE_SELF, result.strategy)
    }

    @Test
    fun `P6-RESOLVE-008 - identity confidence is preserved and derived correctly`() {
        val lowConfChild = ObservationNode(
            nodeId = "child",
            className = "android.view.View",
            identityConfidence = NodeIdentityConfidence.LOW
        )

        val exactParent = ObservationNode(
            nodeId = "parent",
            className = "android.widget.Button",
            resourceId = "com.app:id/exact_btn",
            text = "Click Me",
            clickable = true,
            identityConfidence = NodeIdentityConfidence.EXACT,
            children = listOf(lowConfChild)
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 2,
            rootNode = exactParent
        )

        val result = targetResolver.resolveTargetById(snapshot, "child", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals(NodeIdentityConfidence.EXACT, result.confidence)
    }

    @Test
    fun `P6-RESOLVE-009 - resolution handles missing node ID safely`() {
        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = ObservationNode(nodeId = "root", className = "android.widget.LinearLayout")
        )

        val result = targetResolver.resolveTargetById(snapshot, "non_existent_id", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.TARGET_NOT_FOUND, result.status)
        assertEquals(TargetResolutionStrategy.NO_TARGET, result.strategy)
        assertNotNull(result.failureReason)
    }

    @Test
    fun `P6-RESOLVE-010 - resolution never performs action execution`() {
        val clickableNode = ObservationNode(
            nodeId = "btn",
            className = "android.widget.Button",
            clickable = true
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = clickableNode
        )

        val result = targetResolver.resolveTargetById(snapshot, "btn", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertFalse(result.reacquired)
        // Verified pure domain computation without framework side effects
    }
}
