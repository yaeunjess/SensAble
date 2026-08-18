package com.finclue.sdk.prediction

internal fun interface GeneralCandidateProvider {
    fun findByPrefix(prefix: String, limit: Int): List<String>
}

