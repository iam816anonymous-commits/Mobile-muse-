package com.localagent.app.ui

import androidx.test.core.app.ApplicationProvider
import com.localagent.app.LocalAgentApplication
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = LocalAgentApplication::class)
class StorageDiagnosticsActivityTest {

    private lateinit var app: LocalAgentApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.initializeCoreDomain()
    }

    @Test
    fun testStorageDiagnosticsActivityLaunchAndWrite() {
        val controller = Robolectric.buildActivity(StorageDiagnosticsActivity::class.java).create().start().resume()
        val activity = controller.get()

        val operationalText = activity.findViewById<android.widget.TextView>(com.localagent.app.R.id.tvOperationalStorageDetails).text.toString()
        assertTrue(operationalText.contains("agent.db"))

        val testWriteBtn = activity.findViewById<android.widget.Button>(com.localagent.app.R.id.btnTestWriteMemory)
        testWriteBtn.performClick()

        val readBack = app.durableStorageManager.readRecord("MEMORY_TEST_001")
        assertNotNull(readBack)
        assertEquals("MEMORY_TEST_001", readBack?.recordId)
    }
}
