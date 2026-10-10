package com.localagent.app.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.localagent.app.LocalAgentApplication
import com.localagent.app.R
import com.localagent.app.accessibility.AgentAccessibilityService
import com.localagent.app.accessibility.GlobalActionExecutor
import com.localagent.app.accessibility.UiActionExecutor
import com.localagent.core.command.CommandNormalizer
import com.localagent.core.command.CommandParseResult
import com.localagent.core.command.CommandSource
import com.localagent.core.command.NormalizedCommand
import com.localagent.core.command.TargetSelector
import com.localagent.core.result.ResultCode

class FloatingConsoleService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val commandNormalizer = CommandNormalizer()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        showOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) {
            showOverlay()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val layoutInflater = LayoutInflater.from(this)
        val view = layoutInflater.inflate(R.layout.overlay_floating_console, null)
        overlayView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        val tvTitle = view.findViewById<TextView>(R.id.tvConsoleTitle)
        val tvTargetPkg = view.findViewById<TextView>(R.id.tvTargetPackage)
        val etInput = view.findViewById<EditText>(R.id.etConsoleInput)
        val btnRun = view.findViewById<Button>(R.id.btnConsoleRun)
        val btnClose = view.findViewById<Button>(R.id.btnConsoleClose)
        val tvStatus = view.findViewById<TextView>(R.id.tvConsoleStatus)

        // Make title bar draggable
        tvTitle.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        // Update target package
        val service = AgentAccessibilityService.INSTANCE
        val targetPkg = service?.lastExternalPackageName ?: "com.android.calculator2"
        tvTargetPkg.text = "Target: $targetPkg"

        val runAction = {
            val input = etInput.text.toString().trim()
            val result = executeCommand(input)
            tvStatus.text = result
            // Refresh target package label
            val currentPkg = AgentAccessibilityService.INSTANCE?.lastExternalPackageName ?: targetPkg
            tvTargetPkg.text = "Target: $currentPkg"
        }

        btnRun.setOnClickListener { runAction() }

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                runAction()
                true
            } else {
                false
            }
        }

        btnClose.setOnClickListener {
            stopSelf()
        }

        try {
            wm.addView(view, params)
        } catch (e: Exception) {
            System.err.println("FloatingConsoleService: Unable to add overlay view: ${e.message}")
        }
    }

    private fun executeCommand(input: String): String {
        if (input.isBlank()) return "Result: ${ResultCode.INVALID_INPUT} | Reason: Input is blank"

        val service = AgentAccessibilityService.INSTANCE
        if (service == null || !AgentAccessibilityService.isBound) {
            return "Result: ${ResultCode.ACCESSIBILITY_UNAVAILABLE} | Reason: Accessibility Service unbound"
        }

        return when (val parseResult = commandNormalizer.parseInput(input, CommandSource.CONSOLE)) {
            is CommandParseResult.InvalidInput -> "Result: ${ResultCode.INVALID_INPUT} | Reason: ${parseResult.reason}"
            is CommandParseResult.UnknownCommand -> "Result: ${ResultCode.UNKNOWN_COMMAND} | Reason: Unrecognized syntax '$input'"
            is CommandParseResult.Success -> {
                val command = parseResult.command
                executeNormalizedCommand(command, service)
            }
        }
    }

    private fun executeNormalizedCommand(command: NormalizedCommand, service: AgentAccessibilityService): String {
        return when (command.actionType) {
            com.localagent.core.command.ActionType.UI_CLICK,
            com.localagent.core.command.ActionType.UI_LONG_CLICK,
            com.localagent.core.command.ActionType.UI_TEXT_INPUT,
            com.localagent.core.command.ActionType.UI_SCROLL_FORWARD,
            com.localagent.core.command.ActionType.UI_SCROLL_BACKWARD -> {
                val uiExecutor = UiActionExecutor(
                    accessibilityService = service,
                    snapshotProvider = { service.getSnapshotForContext(isExternal = true) ?: service.captureLiveSnapshot() },
                    requireExternalContext = true
                )
                val coreActionType = when (command.actionType) {
                    com.localagent.core.command.ActionType.UI_CLICK -> com.localagent.core.action.ActionType.UI_CLICK
                    com.localagent.core.command.ActionType.UI_LONG_CLICK -> com.localagent.core.action.ActionType.UI_LONG_CLICK
                    com.localagent.core.command.ActionType.UI_TEXT_INPUT -> com.localagent.core.action.ActionType.UI_TEXT_INPUT
                    com.localagent.core.command.ActionType.UI_SCROLL_FORWARD -> com.localagent.core.action.ActionType.UI_SCROLL_FORWARD
                    com.localagent.core.command.ActionType.UI_SCROLL_BACKWARD -> com.localagent.core.action.ActionType.UI_SCROLL_BACKWARD
                    else -> com.localagent.core.action.ActionType.UI_CLICK
                }
                val targetIdStr = when (val selector = command.targetSelector) {
                    is TargetSelector.ByViewId -> selector.viewIdResourceName
                    is TargetSelector.ByText -> selector.text
                    is TargetSelector.ByContentDescription -> selector.contentDescription
                    is TargetSelector.ByNodeIdentityKey -> selector.nodeIdentityKey
                    is TargetSelector.ByCoordinates -> "coords:${selector.x},${selector.y}"
                    TargetSelector.None -> null
                }
                val request = com.localagent.core.action.ActionRequest(
                    actionType = coreActionType,
                    targetNodeId = targetIdStr,
                    targetNodeIdentity = command.parameters["targetIdentity"],
                    textInputPayload = command.parameters["text"],
                    sourceChannel = command.source.name
                )
                val result = uiExecutor.execute(request)
                "Command: ${command.actionType} | Target: ${targetIdStr ?: "None"} | Status: ${result.verificationResult.resultCode} | Reason: ${result.verificationResult.reason ?: "Executed"}"
            }
            com.localagent.core.command.ActionType.GLOBAL_BACK,
            com.localagent.core.command.ActionType.GLOBAL_HOME,
            com.localagent.core.command.ActionType.GLOBAL_RECENTS -> {
                val globalExecutor = GlobalActionExecutor(accessibilityService = service)
                val coreActionType = when (command.actionType) {
                    com.localagent.core.command.ActionType.GLOBAL_BACK -> com.localagent.core.action.ActionType.GLOBAL_BACK
                    com.localagent.core.command.ActionType.GLOBAL_HOME -> com.localagent.core.action.ActionType.GLOBAL_HOME
                    com.localagent.core.command.ActionType.GLOBAL_RECENTS -> com.localagent.core.action.ActionType.GLOBAL_RECENTS
                    else -> com.localagent.core.action.ActionType.GLOBAL_BACK
                }
                val request = com.localagent.core.action.ActionRequest(
                    actionType = coreActionType,
                    sourceChannel = command.source.name
                )
                val result = globalExecutor.execute(request)
                "Command: ${command.actionType} | Status: ${result.verificationResult.resultCode} | Reason: ${result.verificationResult.reason ?: "Executed"}"
            }
            else -> "Command: ${command.actionType} | Status: ${ResultCode.CAPABILITY_UNAVAILABLE}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { view ->
            windowManager?.removeView(view)
        }
        overlayView = null
        windowManager = null
    }
}
