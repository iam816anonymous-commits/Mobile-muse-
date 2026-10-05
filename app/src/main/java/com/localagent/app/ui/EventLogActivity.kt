package com.localagent.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.localagent.app.LocalAgentApplication
import com.localagent.app.databinding.ActivityEventLogBinding
import com.localagent.core.logging.EventFilter

class EventLogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventLogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        refreshEventLog()
    }

    override fun onResume() {
        super.onResume()
        refreshEventLog()
    }

    private fun refreshEventLog() {
        val app = application as? LocalAgentApplication ?: return

        Thread {
            try {
                val totalCount = app.eventRepository.getEventCount()
                val dbBytes = app.eventRepository.getStorageFootprintBytes()
                val events = app.eventRepository.queryEvents(EventFilter(limit = 100))

                val sb = StringBuilder()
                events.reversed().forEach { event ->
                    val actionStr = if (event.actionType != null) " ${event.actionType}" else ""
                    val codeStr = if (event.resultCode != null) " [${event.resultCode}]" else ""
                    sb.append("${event.timestamp % 1000000}: [${event.subsystem}] ${event.eventType}$actionStr$codeStr\n")
                }

                runOnUiThread {
                    binding.tvEventLogStats.text = "Total Persisted Events: $totalCount | DB Size: ${dbBytes / 1024} KB"
                    binding.tvEventLogStream.text = if (sb.isNotEmpty()) sb.toString() else "No events recorded in database."
                    binding.scrollEventLogContainer.post {
                        binding.scrollEventLogContainer.fullScroll(android.view.View.FOCUS_DOWN)
                    }
                }
            } catch (e: Exception) {
                System.err.println("EventLogActivity error: ${e.message}")
            }
        }.start()
    }
}
