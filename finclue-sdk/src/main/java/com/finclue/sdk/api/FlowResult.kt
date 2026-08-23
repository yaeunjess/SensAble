package com.finclue.sdk.api

sealed interface FlowResult {
    data class Success(val values: Map<String, String>) : FlowResult
    data object Cancelled : FlowResult
    data class Error(val message: String, val cause: Throwable? = null) : FlowResult
}
