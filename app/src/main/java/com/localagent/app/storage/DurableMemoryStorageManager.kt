package com.localagent.app.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import com.localagent.core.storage.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import org.json.JSONObject

class DurableMemoryStorageManager(
    private val context: Context,
    private val customExternalDir: File? = null
) : DurableMemoryStorageProvider {

    private val appPrivateMemoryDir: File by lazy {
        File(context.filesDir, "agent/memory").apply {
            if (!exists()) mkdirs()
        }
    }

    private val durableExternalMemoryDir: File? by lazy {
        customExternalDir ?: resolveExternalDurableDirectory()
    }

    override fun initializeStorage(): StorageAvailabilityStatus {
        val status = getAvailabilityStatus()
        ensureDirectoryStructure()
        return status
    }

    override fun getAvailabilityStatus(): StorageAvailabilityStatus {
        val extDir = durableExternalMemoryDir
        if (extDir != null && extDir.canWrite()) {
            return StorageAvailabilityStatus.AVAILABLE_DURABLE_EXTERNAL
        }

        // On API 29+, if SAF tree is configured or if public external storage requires SAF
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val safConfigured = isSafDocumentTreeConfigured()
            if (safConfigured) {
                return StorageAvailabilityStatus.AVAILABLE_SAF_DOCUMENT_TREE
            }
        }

        if (appPrivateMemoryDir.canWrite()) {
            return StorageAvailabilityStatus.AVAILABLE_APP_PRIVATE_ONLY
        }

        return StorageAvailabilityStatus.UNAVAILABLE_PERMISSION_DENIED
    }

    override fun writeRecord(record: DurableRecord): Boolean {
        val recordWithChecksum = computeChecksum(record)
        val jsonString = serializeRecord(recordWithChecksum)

        val targetDirs = getWritableDirectories()
        if (targetDirs.isEmpty()) return false

        var success = false
        for (dir in targetDirs) {
            val file = File(dir, "${record.recordId}.json")
            try {
                FileOutputStream(file).use { out ->
                    out.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                success = true
            } catch (e: Exception) {
                System.err.println("DurableMemoryStorageManager: Failed writing to ${file.absolutePath}: ${e.message}")
            }
        }
        return success
    }

    override fun readRecord(recordId: String): DurableRecord? {
        val record = readRecordRaw(recordId) ?: return null
        val integrity = verifyIntegrityOfRecord(record)
        return if (integrity.isValid) record else null
    }

    fun readRecordRaw(recordId: String): DurableRecord? {
        val targetDirs = getReadableDirectories()
        for (dir in targetDirs) {
            val file = File(dir, "$recordId.json")
            if (file.exists() && file.canRead()) {
                try {
                    val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
                    return deserializeRecord(jsonString)
                } catch (e: Exception) {
                    System.err.println("DurableMemoryStorageManager: Error reading $recordId: ${e.message}")
                }
            }
        }
        return null
    }

    override fun updateRecord(record: DurableRecord): Boolean {
        return writeRecord(record)
    }

    override fun deleteRecord(recordId: String): Boolean {
        val targetDirs = getWritableDirectories()
        var deletedAny = false
        for (dir in targetDirs) {
            val file = File(dir, "$recordId.json")
            if (file.exists()) {
                if (file.delete()) deletedAny = true
            }
        }
        return deletedAny
    }

    override fun listRecords(category: DurableMemoryCategory?): List<DurableRecord> {
        val recordsMap = mutableMapOf<String, DurableRecord>()
        val targetDirs = getReadableDirectories()

        for (dir in targetDirs) {
            val files = dir.listFiles { _, name -> name.endsWith(".json") } ?: continue
            for (file in files) {
                try {
                    val jsonString = FileInputStream(file).bufferedReader().use { it.readText() }
                    val record = deserializeRecord(jsonString)
                    if (category == null || record.category == category) {
                        if (!recordsMap.containsKey(record.recordId)) {
                            val integrity = verifyIntegrityOfRecord(record)
                            if (integrity.isValid) {
                                recordsMap[record.recordId] = record
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        return recordsMap.values.toList()
    }

    override fun verifyIntegrity(recordId: String): DurableStorageIntegrityResult {
        val record = readRecordRaw(recordId)
            ?: return DurableStorageIntegrityResult(
                isValid = false,
                recordId = recordId,
                status = "NOT_FOUND",
                detail = "Record file not found in any storage location"
            )
        return verifyIntegrityOfRecord(record)
    }

    override fun migrateSchema(targetVersion: Int): Boolean {
        val allRecords = listRecords(category = null)
        var migrated = true

        for (record in allRecords) {
            if (record.version < targetVersion) {
                val updatedRecord = record.copy(version = targetVersion)
                if (!writeRecord(updatedRecord)) {
                    migrated = false
                }
            }
        }
        return migrated
    }

    private fun verifyIntegrityOfRecord(record: DurableRecord): DurableStorageIntegrityResult {
        val expectedSha = calculateSha256(record.payloadJson)
        if (record.checksumSha256.isNotBlank() && record.checksumSha256 != expectedSha) {
            return DurableStorageIntegrityResult(
                isValid = false,
                recordId = record.recordId,
                status = "CHECKSUM_MISMATCH",
                detail = "Expected $expectedSha but found ${record.checksumSha256}"
            )
        }
        return DurableStorageIntegrityResult(
            isValid = true,
            recordId = record.recordId,
            status = "VALID",
            detail = "Checksum matches payload"
        )
    }

    private fun computeChecksum(record: DurableRecord): DurableRecord {
        val sha = calculateSha256(record.payloadJson)
        return record.copy(checksumSha256 = sha)
    }

    private fun calculateSha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun getWritableDirectories(): List<File> {
        val list = mutableListOf<File>()
        durableExternalMemoryDir?.let {
            if (it.exists() || it.mkdirs()) {
                if (it.canWrite()) list.add(it)
            }
        }
        if (appPrivateMemoryDir.exists() || appPrivateMemoryDir.mkdirs()) {
            if (appPrivateMemoryDir.canWrite()) list.add(appPrivateMemoryDir)
        }
        return list
    }

    private fun getReadableDirectories(): List<File> {
        val list = mutableListOf<File>()
        durableExternalMemoryDir?.let {
            if (it.exists() && it.canRead()) list.add(it)
        }
        if (appPrivateMemoryDir.exists() && appPrivateMemoryDir.canRead()) {
            list.add(appPrivateMemoryDir)
        }
        return list
    }

    private fun resolveExternalDurableDirectory(): File? {
        val state = Environment.getExternalStorageState()
        if (Environment.MEDIA_MOUNTED != state) return null

        val publicDir = Environment.getExternalStorageDirectory()
        val durableDir = File(publicDir, "LocalAgent/memory")
        return try {
            if (!durableDir.exists()) {
                durableDir.mkdirs()
            }
            if (durableDir.canWrite()) durableDir else null
        } catch (_: Exception) {
            null
        }
    }

    private fun isSafDocumentTreeConfigured(): Boolean {
        val prefs = context.getSharedPreferences("agent_storage_prefs", Context.MODE_PRIVATE)
        return prefs.getString("saf_memory_uri", null) != null
    }

    private fun ensureDirectoryStructure() {
        if (!appPrivateMemoryDir.exists()) appPrivateMemoryDir.mkdirs()
        durableExternalMemoryDir?.let {
            if (!it.exists()) try { it.mkdirs() } catch (_: Exception) {}
        }
    }

    private fun serializeRecord(record: DurableRecord): String {
        return JSONObject().apply {
            put("recordId", record.recordId)
            put("category", record.category.name)
            put("version", record.version)
            put("timestamp", record.timestamp)
            put("payloadJson", record.payloadJson)
            put("checksumSha256", record.checksumSha256)
            put("metadataJson", record.metadataJson)
        }.toString(2)
    }

    private fun deserializeRecord(jsonStr: String): DurableRecord {
        val json = JSONObject(jsonStr)
        return DurableRecord(
            recordId = json.getString("recordId"),
            category = DurableMemoryCategory.valueOf(json.getString("category")),
            version = json.optInt("version", 1),
            timestamp = json.optLong("timestamp", System.currentTimeMillis()),
            payloadJson = json.getString("payloadJson"),
            checksumSha256 = json.optString("checksumSha256", ""),
            metadataJson = json.optString("metadataJson", "{}")
        )
    }
}
