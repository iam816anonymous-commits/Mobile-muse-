package com.localagent.app.storage

import androidx.test.core.app.ApplicationProvider
import com.localagent.core.storage.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DurableMemoryStorageManagerTest {

    private lateinit var context: android.content.Context
    private lateinit var externalTestDir: File
    private lateinit var storageManager: DurableMemoryStorageManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        externalTestDir = File(context.cacheDir, "external_durable_memory").apply {
            if (!exists()) mkdirs()
        }
        storageManager = DurableMemoryStorageManager(context, customExternalDir = externalTestDir)
        storageManager.initializeStorage()
    }

    @After
    fun tearDown() {
        externalTestDir.deleteRecursively()
    }

    @Test
    fun testWriteAndReadDurableRecord() {
        val record = DurableRecord(
            recordId = "behavior-101",
            category = DurableMemoryCategory.LEARNED_BEHAVIOR,
            version = 1,
            payloadJson = "{\"action\":\"calculator_add\",\"target\":\"button_equals\"}",
            metadataJson = "{\"author\":\"localagent_learner\"}"
        )

        val writeSuccess = storageManager.writeRecord(record)
        assertTrue(writeSuccess)

        val read = storageManager.readRecord("behavior-101")
        assertNotNull(read)
        assertEquals("behavior-101", read?.recordId)
        assertEquals(DurableMemoryCategory.LEARNED_BEHAVIOR, read?.category)
        assertTrue(read?.payloadJson?.contains("calculator_add") == true)
        assertTrue(read?.checksumSha256?.isNotBlank() == true)
    }

    @Test
    fun testDurableMemorySurvivesAppPrivateDirectoryDeletion() {
        val record = DurableRecord(
            recordId = "workflow-202",
            category = DurableMemoryCategory.PROCEDURAL_WORKFLOW,
            version = 1,
            payloadJson = "{\"steps\":[\"launch_settings\",\"click_network\"]}"
        )

        assertTrue(storageManager.writeRecord(record))

        // Simulate app uninstall / "Clear Data" by wiping app-private memory directory
        val appPrivateDir = File(context.filesDir, "agent/memory")
        if (appPrivateDir.exists()) {
            appPrivateDir.deleteRecursively()
        }

        // Read again; record MUST survive because it exists in external durable storage!
        val readAfterUninstall = storageManager.readRecord("workflow-202")
        assertNotNull(readAfterUninstall)
        assertEquals("workflow-202", readAfterUninstall?.recordId)
    }

    @Test
    fun testIntegrityCheckDetectsTampering() {
        val record = DurableRecord(
            recordId = "preference-303",
            category = DurableMemoryCategory.LEARNED_PREFERENCE,
            version = 1,
            payloadJson = "{\"theme\":\"dark\"}"
        )
        assertTrue(storageManager.writeRecord(record))

        // Tamper with file directly on disk
        val file = File(externalTestDir, "preference-303.json")
        assertTrue(file.exists())
        val tamperedJson = file.readText().replace("dark", "light_tampered")
        file.writeText(tamperedJson)

        val integrity = storageManager.verifyIntegrity("preference-303")
        assertFalse(integrity.isValid)
        assertEquals("CHECKSUM_MISMATCH", integrity.status)
    }

    @Test
    fun testListRecordsFilterByCategory() {
        val rec1 = DurableRecord(
            recordId = "rec-1",
            category = DurableMemoryCategory.LEARNED_BEHAVIOR,
            payloadJson = "{\"data\":\"1\"}"
        )
        val rec2 = DurableRecord(
            recordId = "rec-2",
            category = DurableMemoryCategory.PROCEDURAL_WORKFLOW,
            payloadJson = "{\"data\":\"2\"}"
        )

        storageManager.writeRecord(rec1)
        storageManager.writeRecord(rec2)

        val behaviors = storageManager.listRecords(DurableMemoryCategory.LEARNED_BEHAVIOR)
        assertEquals(1, behaviors.size)
        assertEquals("rec-1", behaviors[0].recordId)

        val all = storageManager.listRecords(category = null)
        assertEquals(2, all.size)
    }

    @Test
    fun testSchemaMigration() {
        val record = DurableRecord(
            recordId = "legacy-v1",
            category = DurableMemoryCategory.SEMANTIC_KNOWLEDGE,
            version = 1,
            payloadJson = "{\"key\":\"val\"}"
        )
        storageManager.writeRecord(record)

        val migrated = storageManager.migrateSchema(targetVersion = 2)
        assertTrue(migrated)

        val read = storageManager.readRecord("legacy-v1")
        assertNotNull(read)
        assertEquals(2, read?.version)
    }

    @Test
    fun testDeleteRecord() {
        val record = DurableRecord(
            recordId = "to-delete",
            category = DurableMemoryCategory.LEARNING_ARTIFACT,
            payloadJson = "{\"temp\":true}"
        )
        storageManager.writeRecord(record)
        assertNotNull(storageManager.readRecord("to-delete"))

        val deleted = storageManager.deleteRecord("to-delete")
        assertTrue(deleted)
        assertNull(storageManager.readRecord("to-delete"))
    }
}
