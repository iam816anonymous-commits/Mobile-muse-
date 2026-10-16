package com.localagent.app.accessibility

import com.localagent.core.observation.ObservationSnapshot
import org.junit.Assert.*
import org.junit.Test

class ObservationSnapshotExtractorTest {

    @Test
    fun testNullRootNodeReturnsEmptySnapshot() {
        val extractor = ObservationSnapshotExtractor(maxNodes = 500, maxDepth = 30)
        val snapshot = extractor.extractSnapshot(
            rootNodeInfo = null,
            packageName = "com.test.app",
            activityName = "TestActivity"
        )

        assertEquals("com.test.app", snapshot.packageName)
        assertEquals("TestActivity", snapshot.activityName)
        assertEquals(0, snapshot.nodeCount)
        assertNull(snapshot.rootNode)
        assertFalse(snapshot.truncationInfo.isTruncated)
        assertEquals("NONE", snapshot.truncationInfo.reason)
    }
}
