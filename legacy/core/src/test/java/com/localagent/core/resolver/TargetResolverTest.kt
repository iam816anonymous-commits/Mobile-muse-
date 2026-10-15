package com.localagent.core.resolver

import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import org.junit.Assert.*
import org.junit.Test

class TargetResolverTest {

    private val targetResolver = TargetResolver()

    @Test
    fun `P6-RESOLVE-001 - direct target resolution`() {
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
    fun `P6-RESOLVE-002 - exact target identity resolution`() {
        val targetIdentity = "id:digit_7_text:7"
        val btnNode = ObservationNode(
            nodeId = "btn_7",
            className = "com.google.android.material.button.MaterialButton",
            text = "7",
            resourceId = "com.android.calculator2:id/digit_7",
            clickable = true,
            nodeIdentity = targetIdentity,
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.android.calculator2",
            nodeCount = 1,
            rootNode = btnNode
        )

        val result = targetResolver.resolveTargetByIdentity(snapshot, targetIdentity, TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.RESOLVED, result.status)
        assertEquals("btn_7", result.resolvedNodeId)
        assertEquals(targetIdentity, result.resolvedNodeIdentity)
        assertEquals(TargetResolutionStrategy.SELF_ACTIONABLE, result.strategy)
    }

    @Test
    fun `P6-RESOLVE-003 - clickable child to clickable ancestor`() {
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
    fun `P6-RESOLVE-004 - long-clickable child to long-clickable ancestor`() {
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
    fun `P6-RESOLVE-005 - scrollable child to scrollable ancestor`() {
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
    fun `P6-RESOLVE-006 - editable child to editable ancestor`() {
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
    fun `P6-RESOLVE-007 - ancestor traversal respects maximum depth`() {
        val resolverWithMaxDepth1 = TargetResolver(
            ancestorResolver = ActionableAncestorResolver(maxDepth = 1)
        )

        val leaf = ObservationNode(nodeId = "leaf", clickable = false)
        val mid = ObservationNode(nodeId = "mid", clickable = false, children = listOf(leaf))
        val top = ObservationNode(nodeId = "top", clickable = true, children = listOf(mid))

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 3,
            rootNode = top
        )

        // Traversal from leaf -> mid is depth 1, top is depth 2 (exceeding maxDepth 1)
        val result = resolverWithMaxDepth1.resolveTargetById(snapshot, "leaf", TargetActionType.CLICK)

        assertEquals(TargetResolutionStatus.NO_ACTIONABLE_TARGET, result.status)
        assertEquals(TargetResolutionStrategy.NO_TARGET, result.strategy)
    }

    @Test
    fun `P6-RESOLVE-008 - no matching actionable ancestor returns failure`() {
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
    fun `P6-RESOLVE-009 - capability mismatch target rejected`() {
        val clickableOnlyNode = ObservationNode(
            nodeId = "btn_1",
            className = "android.widget.Button",
            clickable = true,
            scrollable = false,
            editable = false
        )

        val snapshot = ObservationSnapshot(
            packageName = "com.app",
            nodeCount = 1,
            rootNode = clickableOnlyNode
        )

        val scrollResult = targetResolver.resolveTargetById(snapshot, "btn_1", TargetActionType.SCROLL)
        val editResult = targetResolver.resolveTargetById(snapshot, "btn_1", TargetActionType.EDITABLE)

        assertEquals(TargetResolutionStatus.NO_ACTIONABLE_TARGET, scrollResult.status)
        assertEquals(TargetResolutionStatus.NO_ACTIONABLE_TARGET, editResult.status)
    }

    @Test
    fun `P6-RESOLVE-010 - correct resolution strategy and result metadata is returned`() {
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
        assertNotNull(result.toJsonObject())
    }
}
