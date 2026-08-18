package com.finclue.sdk

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import com.finclue.sdk.storage.LocalDataStore
import com.finclue.sdk.storage.LocalDataSummary
import com.finclue.sdk.api.FlowResult
import com.finclue.sdk.api.FlowSpec
import com.finclue.sdk.api.PredictionCandidate
import com.finclue.sdk.api.PredictionRequest
import com.finclue.sdk.api.PredictionSelection
import com.finclue.sdk.internal.FinclueFlowActivity
import com.finclue.sdk.internal.PendingFlow
import com.finclue.sdk.internal.PendingFlowStore
import com.finclue.sdk.prediction.PredictionEngine

/** Stable public entry point for host applications. */
object Finclue {
    @Volatile
    private var predictionEngine: PredictionEngine? = null

    /** Starts a FIN:CLUE-owned input UI and returns its values to the host app. */
    @JvmStatic
    fun runFlow(activity: Activity, spec: FlowSpec, callback: (FlowResult) -> Unit) {
        val id = PendingFlowStore.put(PendingFlow(spec, callback))
        activity.startActivity(
            Intent(activity, FinclueFlowActivity::class.java)
                .putExtra(FinclueFlowActivity.EXTRA_FLOW_ID, id)
        )
    }

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
        predictionEngine?.clearCache()
    }

    /** Runs prediction only when explicitly requested by the host, such as on an up-swipe. */
    @JvmStatic
    suspend fun requestPredictions(
        context: Context,
        request: PredictionRequest,
    ): List<PredictionCandidate> = engine(context).suggest(request)

    /** Returns matching personal history without loading or running the language model. */
    @JvmStatic
    suspend fun requestPersonalPredictions(
        context: Context,
        request: PredictionRequest,
    ): List<PredictionCandidate> = engine(context).suggestPersonal(request)

    /** Records only a candidate that the user explicitly selected and the host allowed to persist. */
    @JvmStatic
    suspend fun recordPredictionSelection(
        context: Context,
        selection: PredictionSelection,
    ) {
        engine(context).recordSelection(selection)
    }

    private fun engine(context: Context): PredictionEngine =
        predictionEngine ?: synchronized(this) {
            predictionEngine ?: PredictionEngine(context.applicationContext).also {
                predictionEngine = it
            }
        }
}
