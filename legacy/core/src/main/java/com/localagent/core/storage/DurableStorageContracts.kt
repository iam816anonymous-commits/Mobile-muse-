package com.localagent.core.storage

enum class StorageAvailabilityStatus {
    AVAILABLE_APP_PRIVATE_ONLY,
    AVAILABLE_DURABLE_EXTERNAL,
    AVAILABLE_SAF_DOCUMENT_TREE,
    UNAVAILABLE_PERMISSION_DENIED,
    UNAVAILABLE_CORRUPTED,
    UNAVAILABLE_STORAGE_FULL
}

enum class DurableMemoryCategory {
    LEARNED_BEHAVIOR,
    EPISODIC_RECORD,
    LEARNED_PREFERENCE,
    PROCEDURAL_WORKFLOW,
    SEMANTIC_KNOWLEDGE,
    LEARNING_ARTIFACT
}

data class DurableRecord(
    val recordId: String,
    val category: DurableMemoryCategory,
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val payloadJson: String,
    val checksumSha256: String = "",
    val metadataJson: String = "{}"
)

data class DurableStorageIntegrityResult(
    val isValid: Boolean,
    val recordId: String,
    val status: String, // VALID, CORRUPTED, NOT_FOUND, CHECKSUM_MISMATCH, VERSION_INCOMPATIBLE
    val detail: String = ""
)

interface DurableMemoryStorageProvider {
    fun initializeStorage(): StorageAvailabilityStatus
    fun getAvailabilityStatus(): StorageAvailabilityStatus
    fun writeRecord(record: DurableRecord): Boolean
    fun readRecord(recordId: String): DurableRecord?
    fun updateRecord(record: DurableRecord): Boolean
    fun deleteRecord(recordId: String): Boolean
    fun listRecords(category: DurableMemoryCategory? = null): List<DurableRecord>
    fun verifyIntegrity(recordId: String): DurableStorageIntegrityResult
    fun migrateSchema(targetVersion: Int): Boolean
}
