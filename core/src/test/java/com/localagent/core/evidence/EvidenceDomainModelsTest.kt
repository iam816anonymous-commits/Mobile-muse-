package com.localagent.core.evidence

import com.localagent.core.observation.ObservationBounds
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.observation.ObservationTruncationInfo
import org.junit.Assert.*
import org.junit.Test

class EvidenceDomainModelsTest {

    @Test
    fun testObservationEvidenceFromSnapshotAndProvenanceHash() {
        val childNode = ObservationNode(
            nodeId = "node_1",
            className = "android.widget.Button",
            packageName = "com.android.chrome",
            text = "Search",
            bounds = ObservationBounds(10, 20, 100, 50),
            clickable = true
        )

        val rootNode = ObservationNode(
            nodeId = "node_0",
            className = "android.widget.FrameLayout",
            packageName = "com.android.chrome",
            bounds = ObservationBounds(0, 0, 1080, 1920),
            children = listOf(childNode)
        )

        val snapshot = ObservationSnapshot(
            snapshotId = "snap-test-100",
            timestamp = 1600000000000L,
            packageName = "com.android.chrome",
            windowId = 42,
            nodeCount = 2,
            rootNode = rootNode,
            truncationInfo = ObservationTruncationInfo(isTruncated = false, reason = "NONE", maxDepthReached = 1)
        )

        val evidence = ObservationEvidence.fromSnapshot(
            snapshot = snapshot,
            source = EvidenceSource.EXTERNAL_APP,
            windowType = 1,
            evidenceId = "ev-test-100"
        )

        assertEquals("ev-test-100", evidence.evidenceId)
        assertEquals("snap-test-100", evidence.snapshotId)
        assertEquals("com.android.chrome", evidence.packageName)
        assertEquals(2, evidence.nodeCount)
        assertEquals(1, evidence.maxDepth)
        assertEquals(EvidenceSource.EXTERNAL_APP, evidence.sourceChannel)
        assertEquals(2, evidence.primitives.size)

        // Verify SHA-256 provenance hash is non-empty and deterministic
        val provHash1 = evidence.provenance.provenanceHash
        val provHash2 = ObservationEvidence.computeProvenanceHash("com.android.chrome", "snap-test-100", evidence.primitives)

        assertNotNull(provHash1)
        assertTrue(provHash1.length >= 32)
        assertEquals(provHash1, provHash2)

        // Test JSON serialization
        val json = evidence.toJsonString()
        assertTrue(json.contains("\"evidenceId\":\"ev-test-100\""))
        assertTrue(json.contains("\"provenanceHash\":\"$provHash1\""))
        assertTrue(json.contains("\"packageName\":\"com.android.chrome\""))
    }
}
