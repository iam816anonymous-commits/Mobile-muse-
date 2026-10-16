package com.localagent.core.observation

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SnapshotDiffEngineTest {

    private lateinit var diffEngine: SnapshotDiffEngine

    @Before
    fun setUp() {
        diffEngine = SnapshotDiffEngine()
    }

    @Test
    fun testIdenticalSnapshotsProduceUnchangedDiff() {
        val node1 = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.TextView",
            text = "Hello World",
            resourceId = "com.example:id/tvTitle",
            nodeIdentity = "id:tvTitle_text:Hello World",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val snapA = ObservationSnapshot(
            snapshotId = "snap_1",
            packageName = "com.example.app",
            nodeCount = 1,
            rootNode = node1
        )

        val snapB = ObservationSnapshot(
            snapshotId = "snap_2",
            packageName = "com.example.app",
            nodeCount = 1,
            rootNode = node1
        )

        val result = diffEngine.computeDiff(snapA, snapB)

        assertFalse(result.hasChanges)
        assertEquals(0, result.totalAdded)
        assertEquals(0, result.totalRemoved)
        assertEquals(0, result.totalChanged)
        assertEquals(1, result.totalUnchanged)
        assertEquals(NodeDiffType.UNCHANGED, result.diffEntries[0].diffType)
    }

    @Test
    fun testAddedAndRemovedNodesDiff() {
        val rootA = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.LinearLayout",
            nodeIdentity = "cls:LinearLayout_parent:root",
            identityConfidence = NodeIdentityConfidence.LOW,
            children = listOf(
                ObservationNode(
                    nodeId = "node_1",
                    className = "android.widget.TextView",
                    text = "Item 1",
                    nodeIdentity = "cls:TextView_lbl:Item 1_parent:cls:LinearLayout_parent:root",
                    identityConfidence = NodeIdentityConfidence.MEDIUM
                )
            )
        )

        val rootB = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.LinearLayout",
            nodeIdentity = "cls:LinearLayout_parent:root",
            identityConfidence = NodeIdentityConfidence.LOW,
            children = listOf(
                ObservationNode(
                    nodeId = "node_2",
                    className = "android.widget.TextView",
                    text = "Item 2",
                    nodeIdentity = "cls:TextView_lbl:Item 2_parent:cls:LinearLayout_parent:root",
                    identityConfidence = NodeIdentityConfidence.MEDIUM
                )
            )
        )

        val snapA = ObservationSnapshot(snapshotId = "snap_1", nodeCount = 2, rootNode = rootA)
        val snapB = ObservationSnapshot(snapshotId = "snap_2", nodeCount = 2, rootNode = rootB)

        val result = diffEngine.computeDiff(snapA, snapB)

        assertTrue(result.hasChanges)
        assertEquals(1, result.totalAdded)   // Item 2 added
        assertEquals(1, result.totalRemoved) // Item 1 removed
        assertEquals(0, result.totalChanged)
        assertEquals(1, result.totalUnchanged) // Root LinearLayout unchanged
    }

    @Test
    fun testChangedAttributeNodeDiff() {
        val rootA = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.Button",
            text = "Submit",
            enabled = true,
            resourceId = "com.example:id/btnSubmit",
            nodeIdentity = "id:btnSubmit_text:Submit",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val rootB = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.Button",
            text = "Submit",
            enabled = false, // Changed attribute
            resourceId = "com.example:id/btnSubmit",
            nodeIdentity = "id:btnSubmit_text:Submit",
            identityConfidence = NodeIdentityConfidence.EXACT
        )

        val snapA = ObservationSnapshot(snapshotId = "snap_1", nodeCount = 1, rootNode = rootA)
        val snapB = ObservationSnapshot(snapshotId = "snap_2", nodeCount = 1, rootNode = rootB)

        val result = diffEngine.computeDiff(snapA, snapB)

        assertTrue(result.hasChanges)
        assertEquals(0, result.totalAdded)
        assertEquals(0, result.totalRemoved)
        assertEquals(1, result.totalChanged)
        assertEquals(0, result.totalUnchanged)

        val changedEntry = result.diffEntries[0]
        assertEquals(NodeDiffType.CHANGED, changedEntry.diffType)
        assertTrue(changedEntry.changedAttributes.contains("enabled"))
    }

    @Test
    fun testEmptyAndNullSnapshotsDiff() {
        val result = diffEngine.computeDiff(null, null)

        assertFalse(result.hasChanges)
        assertEquals(0, result.totalAdded)
        assertEquals(0, result.totalRemoved)
        assertEquals(0, result.totalChanged)
        assertEquals(0, result.totalUnchanged)
        assertTrue(result.diffEntries.isEmpty())
    }
}
