package com.localagent.core.observation

import org.junit.Assert.*
import org.junit.Test

class SnapshotDiffEngineTest {

    private val diffEngine = SnapshotDiffEngine()

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
}
