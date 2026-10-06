package com.localagent.core.action

import com.localagent.core.observation.NodeIdentityConfidence
import com.localagent.core.observation.ObservationNode
import com.localagent.core.observation.ObservationSnapshot
import com.localagent.core.result.ResultCode
import org.junit.Assert.*
import org.junit.Test

class VerificationStrategyTest {

    private val targetVerifier = TargetAwareVerificationStrategy()
    private val navVerifier = NavigationAwareVerificationStrategy()

    @Test
    fun `P7-VERIFY-001 - identical pre-post snapshot yields verification failure or unverified`() {
        val node = ObservationNode(nodeId = "btn", text = "Click")
        val snap1 = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", nodeCount = 1, rootNode = node)
        val snap2 = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", nodeCount = 1, rootNode = node)

        val result = targetVerifier.verify(ActionType.UI_CLICK, "btn", snap1, snap2)

        assertEquals(VerificationStatus.EXECUTED_BUT_NOT_VERIFIED, result.status)
        assertEquals(ResultCode.DISPATCHED_BUT_NOT_VERIFIED, result.resultCode)
        assertNotNull(result.diffResult)
        assertFalse(result.diffResult?.hasChanges == true)
    }

    @Test
    fun `P7-VERIFY-002 - expected target state change yields verification success`() {
        val preNode = ObservationNode(nodeId = "btn", text = "Off", checked = false)
        val postNode = ObservationNode(nodeId = "btn", text = "On", checked = true)

        val preSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", nodeCount = 1, rootNode = preNode)
        val postSnap = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", nodeCount = 1, rootNode = postNode)

        val result = targetVerifier.verify(ActionType.UI_CLICK, "btn", preSnap, postSnap)

        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.resultCode)
    }

    @Test
    fun `P7-VERIFY-003 - unrelated UI change yields UI change verification`() {
        val targetNode = ObservationNode(nodeId = "target", text = "Unchanged")
        val unrelatedPre = ObservationNode(nodeId = "unrelated", text = "Before")
        val unrelatedPost = ObservationNode(nodeId = "unrelated", text = "After")

        val preSnap = ObservationSnapshot(
            snapshotId = "s1",
            packageName = "com.app",
            nodeCount = 2,
            rootNode = ObservationNode(nodeId = "root", children = listOf(targetNode, unrelatedPre))
        )
        val postSnap = ObservationSnapshot(
            snapshotId = "s2",
            packageName = "com.app",
            nodeCount = 2,
            rootNode = ObservationNode(nodeId = "root", children = listOf(targetNode, unrelatedPost))
        )

        val result = targetVerifier.verify(ActionType.UI_CLICK, "target", preSnap, postSnap)

        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.resultCode)
    }

    @Test
    fun `P7-VERIFY-004 - expected navigation change yields navigation verification success`() {
        val preSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", activityName = "MainActivity", windowId = 1)
        val postSnap = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", activityName = "DetailActivity", windowId = 2)

        val result = navVerifier.verify(ActionType.GLOBAL_BACK, null, preSnap, postSnap)

        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.resultCode)
        assertTrue(result.reason?.contains("DetailActivity") == true)
    }

    @Test
    fun `P7-VERIFY-005 - no navigation change yields unverified navigation result`() {
        val preSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", activityName = "MainActivity", windowId = 1)
        val postSnap = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", activityName = "MainActivity", windowId = 1)

        val result = navVerifier.verify(ActionType.GLOBAL_BACK, null, preSnap, postSnap)

        assertEquals(VerificationStatus.EXECUTED_BUT_NOT_VERIFIED, result.status)
        assertEquals(ResultCode.DISPATCHED_BUT_NOT_VERIFIED, result.resultCode)
    }

    @Test
    fun `P7-VERIFY-006 - target disappeared after action yields verification success`() {
        val preTarget = ObservationNode(nodeId = "dialog_btn", text = "Dismiss")
        val preSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", nodeCount = 1, rootNode = preTarget)
        val postSnap = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", nodeCount = 0, rootNode = null)

        val result = targetVerifier.verify(ActionType.UI_CLICK, "dialog_btn", preSnap, postSnap)

        assertEquals(VerificationStatus.EXECUTED_AND_VERIFIED, result.status)
        assertEquals(ResultCode.SUCCESS_VERIFIED, result.resultCode)
        assertTrue(result.reason?.contains("removed") == true)
    }

    @Test
    fun `P7-VERIFY-007 - target identity changed unexpectedly yields verification failure`() {
        val preNode = ObservationNode(nodeId = "btn", nodeIdentity = "id:submit_conf:EXACT")
        val postNode = ObservationNode(nodeId = "btn", nodeIdentity = "id:cancel_conf:EXACT")

        val preSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", nodeCount = 1, rootNode = preNode)
        val postSnap = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", nodeCount = 1, rootNode = postNode)

        val result = targetVerifier.verify(ActionType.UI_CLICK, "btn", preSnap, postSnap)

        assertEquals(VerificationStatus.VERIFICATION_FAILED, result.status)
        assertEquals(ResultCode.TARGET_STALE, result.resultCode)
    }

    @Test
    fun `P7-VERIFY-008 - empty or invalid snapshot yields verification failure`() {
        val validSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app")

        val resultNullPre = targetVerifier.verify(ActionType.UI_CLICK, "btn", null, validSnap)
        val resultNullPost = targetVerifier.verify(ActionType.UI_CLICK, "btn", validSnap, null)

        assertEquals(VerificationStatus.VERIFICATION_FAILED, resultNullPre.status)
        assertEquals(VerificationStatus.VERIFICATION_FAILED, resultNullPost.status)
    }

    @Test
    fun `P7-VERIFY-009 - successful action with unchanged UI yields EXECUTED_BUT_NOT_VERIFIED`() {
        val node = ObservationNode(nodeId = "btn", text = "Static Button")
        val snap1 = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", nodeCount = 1, rootNode = node)
        val snap2 = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", nodeCount = 1, rootNode = node)

        val result = targetVerifier.verify(ActionType.UI_CLICK, "btn", snap1, snap2)

        assertEquals(VerificationStatus.EXECUTED_BUT_NOT_VERIFIED, result.status)
        assertEquals(ResultCode.DISPATCHED_BUT_NOT_VERIFIED, result.resultCode)
    }

    @Test
    fun `P7-VERIFY-010 - verification result contains pre-post diff information`() {
        val preNode = ObservationNode(nodeId = "btn", text = "Start")
        val postNode = ObservationNode(nodeId = "btn", text = "Stop")

        val preSnap = ObservationSnapshot(snapshotId = "s1", packageName = "com.app", nodeCount = 1, rootNode = preNode)
        val postSnap = ObservationSnapshot(snapshotId = "s2", packageName = "com.app", nodeCount = 1, rootNode = postNode)

        val result = targetVerifier.verify(ActionType.UI_CLICK, "btn", preSnap, postSnap)

        assertNotNull(result.diffResult)
        assertEquals(1, result.diffResult?.totalChanged)
    }
}
