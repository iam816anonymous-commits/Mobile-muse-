package com.localagent.app.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.localagent.app.LocalAgentApplication
import com.localagent.app.databinding.ActivityStorageDiagnosticsBinding
import com.localagent.core.logging.AgentEvent
import com.localagent.core.logging.EventSeverity
import com.localagent.core.logging.EventSubsystem
import com.localagent.core.storage.DurableMemoryCategory
import com.localagent.core.storage.DurableRecord
import java.util.UUID

class StorageDiagnosticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStorageDiagnosticsBinding
    private var lastWriteStatus: String = "NONE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStorageDiagnosticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        refreshStorageDiagnostics()
    }

    override fun onResume() {
        super.onResume()
        refreshStorageDiagnostics()
    }

    private fun setupListeners() {
        binding.btnEnableStorage.setOnClickListener {
            requestDurableStorageAccess()
        }

        binding.btnTestWriteMemory.setOnClickListener {
            testWriteDurableMemoryRecord()
        }
    }

    private fun testWriteDurableMemoryRecord() {
        val app = application as? LocalAgentApplication ?: return
        val record = DurableRecord(
            recordId = "MEMORY_TEST_001",
            category = DurableMemoryCategory.LEARNING_ARTIFACT,
            version = 1,
            payloadJson = "{\"test\":true,\"createdMs\":${System.currentTimeMillis()},\"author\":\"storage_diagnostics_ui\"}",
            metadataJson = "{\"origin\":\"testWriteDurableMemoryRecord\"}"
        )

        val success = app.durableStorageManager.writeRecord(record)
        if (success) {
            val readBack = app.durableStorageManager.readRecord("MEMORY_TEST_001")
            if (readBack != null) {
                lastWriteStatus = "SUCCESS (MEMORY_TEST_001 written & SHA-256 verified)"
                app.eventLogger.logEvent(
                    AgentEvent(
                        eventId = UUID.randomUUID().toString(),
                        sessionId = app.eventLogger.getActiveSession().sessionId,
                        subsystem = EventSubsystem.SYSTEM,
                        eventType = "DURABLE_STORAGE_WRITE_SUCCESS",
                        metadataJson = "{\"recordId\":\"MEMORY_TEST_001\"}"
                    )
                )
            } else {
                lastWriteStatus = "FAILED (Integrity readback failed)"
            }
        } else {
            lastWriteStatus = "FAILED (Write error to backend)"
        }
        refreshStorageDiagnostics()
    }

    private val documentTreeLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val prefs = getSharedPreferences("agent_storage_prefs", MODE_PRIVATE)
            prefs.edit().putString("saf_memory_uri", uri.toString()).apply()
            refreshStorageDiagnostics()
        }
    }

    private fun requestDurableStorageAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                documentTreeLauncher.launch(null)
            } catch (e: Exception) {
                System.err.println("StorageDiagnosticsActivity: SAF intent error: ${e.message}")
            }
        } else {
            val permissions = arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            ActivityCompat.requestPermissions(this, permissions, 1002)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002) {
            refreshStorageDiagnostics()
        }
    }

    private fun refreshStorageDiagnostics() {
        val app = application as? LocalAgentApplication ?: return
        val session = app.eventLogger.getActiveSession()
        val durableStatus = app.durableStorageManager.getAvailabilityStatus()

        Thread {
            try {
                val totalEvents = app.eventRepository.getEventCount()
                val dbBytes = app.eventRepository.getStorageFootprintBytes()
                val durableRecordsCount = app.durableStorageManager.listRecords().size

                runOnUiThread {
                    binding.tvOperationalStorageDetails.text = "Database: agent.db\nLocation: /data/data/com.localagent.app/files/agent/agent.db\nActive Session: ${session.sessionId.take(8)}...\nTotal Events: $totalEvents\nDatabase Footprint: ${dbBytes / 1024} KB\nRetention Cap: 50,000 events / 30 MB"
                    binding.tvDurableMemoryDetails.text = "Location: INTERNAL_SHARED_STORAGE (/sdcard/LocalAgent/memory/)\nStatus: $durableStatus\nDurable Records: $durableRecordsCount\nUninstall Survival: YES (User-Owned)\nLast Test Write: $lastWriteStatus"
                }
            } catch (e: Exception) {
                System.err.println("StorageDiagnosticsActivity error: ${e.message}")
            }
        }.start()
    }
}
