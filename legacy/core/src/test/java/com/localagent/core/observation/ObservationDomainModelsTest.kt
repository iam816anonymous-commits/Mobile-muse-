package com.localagent.core.observation

import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ObservationDomainModelsTest {

    @Test
    fun testObservationNodeHierarchyAndSerialization() {
        val childNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.TextView",
            text = "7",
            resourceId = "com.calculator:id/btn_7",
            bounds = ObservationBounds(left = 10, top = 20, right = 100, bottom = 200),
            clickable = true,
            parentInstanceId = "node_0"
        )

        val rootNode = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.LinearLayout",
            packageName = "com.calculator",
            bounds = ObservationBounds(left = 0, top = 0, right = 1080, bottom = 1920),
            children = listOf(childNode)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-100",
            packageName = "com.calculator",
            nodeCount = 2,
            rootNode = rootNode,
            truncationInfo = ObservationTruncationInfo(isTruncated = false, reason = "NONE")
        )

        val jsonStr = snapshot.toJsonString()
        assertTrue(jsonStr.contains("com.calculator"))
        assertTrue(jsonStr.contains("node_1"))
        assertTrue(jsonStr.contains("btn_7"))

        val restored = ObservationSnapshot.fromJsonString(jsonStr)
        assertEquals("snap-100", restored.snapshotId)
        assertEquals("com.calculator", restored.packageName)
        assertEquals(2, restored.nodeCount)
        assertNotNull(restored.rootNode)
        assertEquals(1, restored.rootNode?.children?.size)
        assertEquals("7", restored.rootNode?.children?.get(0)?.text)
        assertEquals("node_0", restored.rootNode?.children?.get(0)?.parentInstanceId)
    }

    @Test
    fun testTruncationInfoSerialization() {
        val info = ObservationTruncationInfo(
            isTruncated = true,
            reason = "MAX_NODES_EXCEEDED",
            totalNodesTraversed = 500,
            maxDepthReached = 12
        )

        val jsonObj = info.toJsonObject()
        assertTrue(jsonObj.getBoolean("isTruncated"))
        assertEquals("MAX_NODES_EXCEEDED", jsonObj.getString("reason"))
        assertEquals(500, jsonObj.getInt("totalNodesTraversed"))

        val restored = ObservationTruncationInfo.fromJsonObject(jsonObj)
        assertTrue(restored.isTruncated)
        assertEquals("MAX_NODES_EXCEEDED", restored.reason)
    }
}
