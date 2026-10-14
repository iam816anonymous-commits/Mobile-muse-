package com.localagent.core.observation

import org.junit.Assert.*
import org.junit.Test

class SnapshotDiffEngineTest {

    private val diffEngine = SnapshotDiffEngine()

    @Test
    fun `testIdenticalTreesUnderDifferentGenerationsProduceZeroDiff`() {
        val (idNode, conf) = ObservationNode.computeIdentity(
            packageName = "com.app",
            className = "android.widget.TextView",
            resourceId = "com.app:id/title",
            text = "Welcome",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )

        val rootNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.TextView",
            resourceId = "com.app:id/title",
            text = "Welcome",
            packageName = "com.app",
            nodeIdentity = idNode,
            identityConfidence = conf
        )

        val beforeSnap = ObservationSnapshot(
            snapshotId = "snap_100",
            generation = 100L,
            timestamp = 1000L,
            packageName = "com.app",
            nodeCount = 1,
            rootNode = rootNode
        )

        val afterSnap = ObservationSnapshot(
            snapshotId = "snap_101",
            generation = 101L,
            timestamp = 1500L,
            packageName = "com.app",
            nodeCount = 1,
            rootNode = rootNode
        )

        val diff = diffEngine.computeDiff(beforeSnap, afterSnap)

        assertFalse(diff.hasChanges)
        assertEquals(0, diff.totalAdded)
        assertEquals(0, diff.totalRemoved)
        assertEquals(0, diff.totalChanged)
        assertEquals(1, diff.totalUnchanged)
    }

    @Test
    fun `testCalculatorFormulaTextChangeReportsExactOneChangedNode`() {
        val (idFormula, conf1) = ObservationNode.computeIdentity(
            packageName = "com.transsion.calculator",
            className = "android.widget.TextView",
            resourceId = "com.transsion.calculator:id/formula",
            text = "No formula",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = "root"
        )

        val beforeNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.TextView",
            resourceId = "com.transsion.calculator:id/formula",
            text = "No formula",
            packageName = "com.transsion.calculator",
            nodeIdentity = idFormula,
            identityConfidence = conf1
        )

        val afterNode = beforeNode.copy(text = "6")

        val beforeSnap = ObservationSnapshot(
            snapshotId = "snap_before",
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = beforeNode
        )

        val afterSnap = ObservationSnapshot(
            snapshotId = "snap_after",
            packageName = "com.transsion.calculator",
            nodeCount = 1,
            rootNode = afterNode
        )

        val diff = diffEngine.computeDiff(beforeSnap, afterSnap)

        assertTrue(diff.hasChanges)
        assertEquals(0, diff.totalAdded)
        assertEquals(0, diff.totalRemoved)
        assertEquals(1, diff.totalChanged)

        val changedEntry = diff.diffEntries.first { it.diffType == NodeDiffType.CHANGED }
        assertEquals("id:formula", changedEntry.nodeIdentity)
        assertTrue(changedEntry.changedAttributes.contains("text"))
        assertEquals("No formula", changedEntry.beforeNode?.text)
        assertEquals("6", changedEntry.afterNode?.text)
    }

    @Test
    fun `testSettingsScrollingBoundsShiftDetected`() {
        val (idList, conf) = ObservationNode.computeIdentity(
            packageName = "com.android.settings",
            className = "androidx.recyclerview.widget.RecyclerView",
            resourceId = "com.android.settings:id/list",
            text = null,
            contentDescription = null,
            childIndex = 0,
            parentIdentity = null
        )

        val beforeNode = ObservationNode(
            nodeId = "list_node",
            className = "androidx.recyclerview.widget.RecyclerView",
            resourceId = "com.android.settings:id/list",
            bounds = ObservationBounds(left = 0, top = 200, right = 1080, bottom = 2000),
            nodeIdentity = idList,
            identityConfidence = conf
        )

        // Significant 100px scroll shift in bounds
        val afterNode = beforeNode.copy(
            bounds = ObservationBounds(left = 0, top = 100, right = 1080, bottom = 1900)
        )

        val beforeSnap = ObservationSnapshot(snapshotId = "snap_scroll_1", rootNode = beforeNode)
        val afterSnap = ObservationSnapshot(snapshotId = "snap_scroll_2", rootNode = afterNode)

        val diff = diffEngine.computeDiff(beforeSnap, afterSnap)

        assertTrue(diff.hasChanges)
        assertEquals(0, diff.totalAdded)
        assertEquals(0, diff.totalRemoved)
        assertEquals(1, diff.totalChanged)

        val entry = diff.diffEntries.first()
        assertTrue(entry.changedAttributes.contains("bounds"))
    }

    @Test
    fun `testVolatileBoundsOrMetadataJitterDoesNotProduceFakeDiffs`() {
        val (idBtn, conf) = ObservationNode.computeIdentity(
            packageName = "com.transsion.calculator",
            className = "android.widget.Button",
            resourceId = "com.transsion.calculator:id/digit_6",
            text = "6",
            contentDescription = null,
            childIndex = 1,
            parentIdentity = "root"
        )

        val beforeNode = ObservationNode(
            nodeId = "node_btn",
            className = "android.widget.Button",
            resourceId = "com.transsion.calculator:id/digit_6",
            text = "6",
            bounds = ObservationBounds(left = 100, top = 200, right = 300, bottom = 400),
            focused = false,
            nodeIdentity = idBtn,
            identityConfidence = conf
        )

        // Minor 2px jitter in bounds and focused toggle
        val afterNode = beforeNode.copy(
            bounds = ObservationBounds(left = 101, top = 200, right = 300, bottom = 400),
            focused = true
        )

        val beforeSnap = ObservationSnapshot(snapshotId = "snap_1", rootNode = beforeNode)
        val afterSnap = ObservationSnapshot(snapshotId = "snap_2", rootNode = afterNode)

        val diff = diffEngine.computeDiff(beforeSnap, afterSnap)

        assertFalse(diff.hasChanges)
        assertEquals(0, diff.totalAdded)
        assertEquals(0, diff.totalRemoved)
        assertEquals(0, diff.totalChanged)
        assertEquals(1, diff.totalUnchanged)
    }

    @Test
    fun `testVirtualizedListNodesEnteringAndLeavingReportedAsAddedAndRemoved`() {
        val (idItem1, conf1) = ObservationNode.computeIdentity(
            packageName = "com.android.settings",
            className = "android.widget.TextView",
            resourceId = null,
            text = "Network & internet",
            contentDescription = null,
            childIndex = 0,
            parentIdentity = "root"
        )

        val (idItem2, conf2) = ObservationNode.computeIdentity(
            packageName = "com.android.settings",
            className = "android.widget.TextView",
            resourceId = null,
            text = "Connected devices",
            contentDescription = null,
            childIndex = 1,
            parentIdentity = "root"
        )

        val item1Node = ObservationNode(
            nodeId = "item_1",
            className = "android.widget.TextView",
            text = "Network & internet",
            nodeIdentity = idItem1,
            identityConfidence = conf1
        )

        val item2Node = ObservationNode(
            nodeId = "item_2",
            className = "android.widget.TextView",
            text = "Connected devices",
            nodeIdentity = idItem2,
            identityConfidence = conf2
        )

        val beforeSnap = ObservationSnapshot(snapshotId = "snap_v1", rootNode = item1Node)
        val afterSnap = ObservationSnapshot(snapshotId = "snap_v2", rootNode = item2Node)

        val diff = diffEngine.computeDiff(beforeSnap, afterSnap)

        assertTrue(diff.hasChanges)
        assertEquals(1, diff.totalAdded)
        assertEquals(1, diff.totalRemoved)
        assertEquals(0, diff.totalChanged)
    }
}
