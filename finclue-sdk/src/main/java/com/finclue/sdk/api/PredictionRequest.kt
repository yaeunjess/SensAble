package com.finclue.sdk.api

/** Host-owned policy and input for one explicit prediction request. */
data class PredictionRequest(
    val currentText: String,
    val mode: PredictionMode = PredictionMode.GENERAL,
    val context: PredictionContext = PredictionContext.GENERAL,
    val historyPolicy: HistoryPolicy = HistoryPolicy.NONE,
    val limit: Int = 3,
) {
    init {
        require(currentText.isNotBlank()) { "PredictionRequest.currentText must not be blank." }
        require(mode != PredictionMode.DISABLED) { "A disabled prediction request cannot run." }
        require(limit in 1..10) { "PredictionRequest.limit must be between 1 and 10." }
        require(mode == PredictionMode.PERSONALIZED || historyPolicy == HistoryPolicy.NONE) {
            "HistoryPolicy requires PredictionMode.PERSONALIZED."
        }
    }
}

/** Host-owned policy for recording an explicitly selected prediction. */
data class PredictionSelection(
    val text: String,
    val context: PredictionContext = PredictionContext.GENERAL,
    val historyPolicy: HistoryPolicy = HistoryPolicy.NONE,
) {
    init {
        require(text.isNotBlank()) { "PredictionSelection.text must not be blank." }
    }
}
