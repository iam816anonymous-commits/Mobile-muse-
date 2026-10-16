package com.localagent.app.accessibility

import android.view.accessibility.AccessibilityNodeInfo

object AccessibilityTestFixtures {

    @Suppress("DEPRECATION")
    fun createClickableNode(
        packageName: String = "com.app",
        className: String = "android.widget.Button",
        viewIdResourceName: String? = null,
        text: CharSequence? = null
    ): AccessibilityNodeInfo {
        return AccessibilityNodeInfo.obtain().apply {
            this.packageName = packageName
            this.className = className
            this.viewIdResourceName = viewIdResourceName
            this.text = text
            this.isClickable = true
            this.addAction(AccessibilityNodeInfo.ACTION_CLICK)
            this.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
        }
    }

    @Suppress("DEPRECATION")
    fun createLongClickableNode(
        packageName: String = "com.app",
        className: String = "androidx.cardview.widget.CardView",
        viewIdResourceName: String? = null,
        text: CharSequence? = null
    ): AccessibilityNodeInfo {
        return AccessibilityNodeInfo.obtain().apply {
            this.packageName = packageName
            this.className = className
            this.viewIdResourceName = viewIdResourceName
            this.text = text
            this.isLongClickable = true
            this.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            this.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK)
        }
    }

    @Suppress("DEPRECATION")
    fun createEditableNode(
        packageName: String = "com.app",
        className: String = "android.widget.EditText",
        viewIdResourceName: String? = null,
        text: CharSequence? = null
    ): AccessibilityNodeInfo {
        return AccessibilityNodeInfo.obtain().apply {
            this.packageName = packageName
            this.className = className
            this.viewIdResourceName = viewIdResourceName
            this.text = text
            this.isEditable = true
            this.addAction(AccessibilityNodeInfo.ACTION_SET_TEXT)
            this.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)
        }
    }

    @Suppress("DEPRECATION")
    fun createScrollableNode(
        packageName: String = "com.app",
        className: String = "androidx.recyclerview.widget.RecyclerView",
        viewIdResourceName: String? = null,
        text: CharSequence? = null
    ): AccessibilityNodeInfo {
        return AccessibilityNodeInfo.obtain().apply {
            this.packageName = packageName
            this.className = className
            this.viewIdResourceName = viewIdResourceName
            this.text = text
            this.isScrollable = true
            this.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            this.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            this.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD)
            this.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD)
        }
    }
}
