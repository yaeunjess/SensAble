package com.finclue.sdk.prediction

import android.content.Context
/** A single context-independent candidate source packaged with the SDK. */
internal class AssetGeneralCandidateProvider(context: Context) : GeneralCandidateProvider {
    private val appContext = context.applicationContext

    private val delegate: GeneralCandidateProvider by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        appContext.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).useLines { lines ->
            ListGeneralCandidateProvider(lines.toList().asSequence())
        }
    }

    override fun findByPrefix(prefix: String, limit: Int): List<String> =
        delegate.findByPrefix(prefix, limit)

    private companion object {
        const val ASSET_PATH = "finclue/prediction/general_candidates.txt"

    }
}
