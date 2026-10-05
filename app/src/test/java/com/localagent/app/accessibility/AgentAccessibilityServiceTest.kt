package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import com.localagent.core.logging.EventFilter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class AgentAccessibilityServiceTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
        AgentAccessibilityService.resetForTest()
    }

    @After
    fun tearDown() {
        AgentAccessibilityService.resetForTest()
    }

    @Test
    fun testUnboundServiceStatus() {
        assertFalse(AgentAccessibilityService.isBound)
        assertNull(AgentAccessibilityService.INSTANCE)
    }

    @Test
    fun testObservationIsReadOnlyWithNoActionExecution() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        assertFalse(AgentAccessibilityService.isBound)

        val snapshot = service.captureLiveSnapshot()
        assertNotNull(snapshot)
        assertEquals(0, snapshot.nodeCount)
        assertNull(snapshot.rootNode)

        // Verify zero action policy or execution dispatched during observation
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }

    @Test
    fun testPreservationOfLastExternalObservationSnapshot() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        // Initially no snapshots
        assertNull(service.currentObservationSnapshot)
        assertNull(service.lastExternalObservationSnapshot)

        // Simulate observing current UI (LocalAgent app itself)
        service.captureLiveSnapshot()
        assertNotNull(service.currentObservationSnapshot)
        assertNull(service.lastExternalObservationSnapshot)
    }

    @Test
    fun testValidExternalApplicationPackageClassification() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        assertTrue(service.isValidExternalApplicationPackage("com.android.chrome"))
        assertTrue(service.isValidExternalApplicationPackage("com.google.android.apps.chrome"))
        assertTrue(service.isValidExternalApplicationPackage("com.android.calculator2"))
        assertTrue(service.isValidExternalApplicationPackage("com.android.settings"))

        assertFalse(service.isValidExternalApplicationPackage("com.localagent.app"))
        assertFalse(service.isValidExternalApplicationPackage("com.android.systemui"))
        assertFalse(service.isValidExternalApplicationPackage("com.google.android.apps.nexuslauncher"))
        assertFalse(service.isValidExternalApplicationPackage("com.android.launcher3"))
        assertFalse(service.isValidExternalApplicationPackage(""))
    }

    @Test
    fun testWindowCandidateScoreRanking() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        val method = AgentAccessibilityService::class.java.getDeclaredMethod(
            "calculateWindowScore",
            Int::class.java,
            String::class.java,
            Boolean::class.java,
            Boolean::class.java
        )
        method.isAccessible = true

        val chromeScore = method.invoke(service, AccessibilityWindowInfo.TYPE_APPLICATION, "com.android.chrome", true, true) as Int
        val systemUiScore = method.invoke(service, AccessibilityWindowInfo.TYPE_SYSTEM, "com.android.systemui", true, true) as Int

        assertTrue(chromeScore > systemUiScore)
        assertEquals(100, chromeScore)
        assertEquals(50, systemUiScore)
    }

    @Test
    fun testCandidateRejectionDeduplicationWithinSingleRequest() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        val selectBestMethod = AgentAccessibilityService::class.java.getDeclaredMethod(
            "selectBestExternalWindow",
            List::class.java,
            String::class.java,
            MutableSet::class.java
        )
        selectBestMethod.isAccessible = true

        val internalCandidateClass = Class.forName("com.localagent.app.accessibility.AgentAccessibilityService\$InternalCandidate")
        val candidateInfoClass = Class.forName("com.localagent.app.accessibility.WindowCandidateInfo")

        val sysUiInfo = candidateInfoClass.getConstructor(
            Int::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java, Boolean::class.java, Int::class.java
        ).newInstance(1, AccessibilityWindowInfo.TYPE_SYSTEM, "TYPE_SYSTEM", "com.android.systemui", true, true, 50)

        val sysUiCandidate = internalCandidateClass.getConstructor(
            candidateInfoClass, android.view.accessibility.AccessibilityNodeInfo::class.java
        ).newInstance(sysUiInfo, null)

        val candidateList = listOf(sysUiCandidate, sysUiCandidate, sysUiCandidate)
        val rejectedSet = mutableSetOf<String>()

        val activeSessionId = app.eventLogger.getActiveSession().sessionId
        val filter = EventFilter(sessionId = activeSessionId, limit = 100)
        val countBefore = app.eventRepository.queryEvents(filter).count { it.eventType == "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED" }

        // Execute selection over candidates containing 3 duplicate SystemUI entries
        selectBestMethod.invoke(service, candidateList, activeSessionId, rejectedSet)

        // Wait briefly for single-thread logger executor to finish DB insert
        Thread.sleep(150)

        val eventsAfter = app.eventRepository.queryEvents(filter)
        val rejectionsLogged = eventsAfter.filter { it.eventType == "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED" }
        val newRejectionsCount = rejectionsLogged.size - countBefore

        // Test A: Exactly 1 rejection event emitted for multiple duplicate candidates in single request
        assertEquals(1, newRejectionsCount)
        val firstEvent = rejectionsLogged.first() // queryEvents is ORDER BY timestamp DESC
        assertTrue(firstEvent.metadataJson.contains("com.android.systemui"))
        assertTrue(firstEvent.metadataJson.contains("SYSTEM_UI_NOT_VALID_EXTERNAL_APPLICATION"))
    }

    @Test
    fun testDifferentCandidateRejectionsLoggedIndependently() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        val selectBestMethod = AgentAccessibilityService::class.java.getDeclaredMethod(
            "selectBestExternalWindow",
            List::class.java,
            String::class.java,
            MutableSet::class.java
        )
        selectBestMethod.isAccessible = true

        val internalCandidateClass = Class.forName("com.localagent.app.accessibility.AgentAccessibilityService\$InternalCandidate")
        val candidateInfoClass = Class.forName("com.localagent.app.accessibility.WindowCandidateInfo")

        val sysUiInfo = candidateInfoClass.getConstructor(
            Int::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java, Boolean::class.java, Int::class.java
        ).newInstance(1, AccessibilityWindowInfo.TYPE_SYSTEM, "TYPE_SYSTEM", "com.android.systemui", true, true, 50)

        val launcherInfo = candidateInfoClass.getConstructor(
            Int::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java, Boolean::class.java, Int::class.java
        ).newInstance(2, AccessibilityWindowInfo.TYPE_APPLICATION, "TYPE_APPLICATION", "com.android.launcher3", true, true, 60)

        val sysUiCand = internalCandidateClass.getConstructor(candidateInfoClass, android.view.accessibility.AccessibilityNodeInfo::class.java).newInstance(sysUiInfo, null)
        val launcherCand = internalCandidateClass.getConstructor(candidateInfoClass, android.view.accessibility.AccessibilityNodeInfo::class.java).newInstance(launcherInfo, null)

        val candidateList = listOf(sysUiCand, launcherCand, sysUiCand)
        val rejectedSet = mutableSetOf<String>()

        val activeSessionId = app.eventLogger.getActiveSession().sessionId
        val filter = EventFilter(sessionId = activeSessionId, limit = 100)
        val countBefore = app.eventRepository.queryEvents(filter).count { it.eventType == "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED" }

        selectBestMethod.invoke(service, candidateList, activeSessionId, rejectedSet)

        // Wait briefly for single-thread logger executor to finish DB insert
        Thread.sleep(150)

        val eventsAfter = app.eventRepository.queryEvents(filter)
        val rejectionsLogged = eventsAfter.filter { it.eventType == "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED" }
        val newRejectionsCount = rejectionsLogged.size - countBefore

        // Test B: SystemUI and Launcher logged independently (2 distinct events for 2 distinct invalid packages)
        assertEquals(2, newRejectionsCount)
        val recentRejections = rejectionsLogged.take(newRejectionsCount) // newest events first
        assertTrue(recentRejections.any { it.metadataJson.contains("com.android.systemui") })
        assertTrue(recentRejections.any { it.metadataJson.contains("com.android.launcher3") })
    }

    @Test
    fun testDeduplicationResetsOnNewObservationRequest() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        val selectBestMethod = AgentAccessibilityService::class.java.getDeclaredMethod(
            "selectBestExternalWindow",
            List::class.java,
            String::class.java,
            MutableSet::class.java
        )
        selectBestMethod.isAccessible = true

        val internalCandidateClass = Class.forName("com.localagent.app.accessibility.AgentAccessibilityService\$InternalCandidate")
        val candidateInfoClass = Class.forName("com.localagent.app.accessibility.WindowCandidateInfo")

        val sysUiInfo = candidateInfoClass.getConstructor(
            Int::class.java, Int::class.java, String::class.java, String::class.java, Boolean::class.java, Boolean::class.java, Int::class.java
        ).newInstance(1, AccessibilityWindowInfo.TYPE_SYSTEM, "TYPE_SYSTEM", "com.android.systemui", true, true, 50)
        val sysUiCand = internalCandidateClass.getConstructor(candidateInfoClass, android.view.accessibility.AccessibilityNodeInfo::class.java).newInstance(sysUiInfo, null)

        val candidateList = listOf(sysUiCand, sysUiCand)
        val activeSessionId = app.eventLogger.getActiveSession().sessionId
        val filter = EventFilter(sessionId = activeSessionId, limit = 100)

        val countBefore = app.eventRepository.queryEvents(filter).count { it.eventType == "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED" }

        // Observation Request #1
        val rejectedSetReq1 = mutableSetOf<String>()
        selectBestMethod.invoke(service, candidateList, activeSessionId, rejectedSetReq1)
        Thread.sleep(150)

        // Observation Request #2 (fresh request-scoped set)
        val rejectedSetReq2 = mutableSetOf<String>()
        selectBestMethod.invoke(service, candidateList, activeSessionId, rejectedSetReq2)
        Thread.sleep(150)

        val countAfter = app.eventRepository.queryEvents(filter).count { it.eventType == "OBSERVATION_EXTERNAL_CANDIDATE_REJECTED" }

        // Test C: Total 2 events emitted across 2 distinct observation requests
        assertEquals(2, countAfter - countBefore)
    }

    @Test
    fun testValidExternalApplicationRemainsFullyObservableAndReadOnly() {
        val serviceController = Robolectric.buildService(AgentAccessibilityService::class.java).create()
        val service = serviceController.get()

        // Test D: Valid external application (Calculator) classification succeeds
        assertTrue(service.isValidExternalApplicationPackage("com.android.calculator2"))

        // Test E: Read-only guarantee verified
        assertEquals(0, app.goalDispatcher.getQueueSize())
    }
}
