package com.finclue.sdk

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.finclue.sdk.storage.LocalDataStore
import com.finclue.sdk.storage.LocalDataSummary

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

    /** Returns aggregate, non-sensitive data recorded only in this app installation. */
    @JvmStatic
    suspend fun getLocalDataSummary(context: Context): LocalDataSummary =
        LocalDataStore(context).summary()

    /** Clears FIN:CLUE-owned local data. Intended for demo reset and user data controls. */
    @JvmStatic
    suspend fun clearLocalData(context: Context) {
        LocalDataStore(context).clear()
    }
}
