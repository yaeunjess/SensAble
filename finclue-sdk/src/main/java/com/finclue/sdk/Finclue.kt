package com.finclue.sdk

import android.content.Context
import android.view.accessibility.AccessibilityManager

/** Stable public entry point for host applications. */
object Finclue {
    /**
     * Returns whether an enabled accessibility service is currently touch-exploring.
     * Hosts may ignore this hint and expose an explicit FIN:CLUE entry button.
     */
    @JvmStatic
    fun shouldAssist(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager
        return manager?.isEnabled == true && manager.isTouchExplorationEnabled
    }
}
