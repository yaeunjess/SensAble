package com.sensable.app.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Region
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent

/**
 * Narrow accessibility service used only to pass the active braille surface's raw multitouch
 * stream through Android's touch explorer. It does not inspect windows or accessibility events.
 */
class BrailleTouchAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        BraillePassthroughController.attach(this)
    }

    internal fun activatePassthrough(region: Region): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || region.isEmpty) return false

        val info = serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE == 0) {
            info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            serviceInfo = info
        }
        setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, Region(region))
        return true
    }

    internal fun deactivatePassthrough() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        setTouchExplorationPassthroughRegion(Display.DEFAULT_DISPLAY, Region())
        val info = serviceInfo
        if (info.flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE != 0) {
            info.flags = info.flags and
                AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE.inv()
            serviceInfo = info
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        deactivatePassthrough()
        BraillePassthroughController.detach(this)
        super.onDestroy()
    }
}
