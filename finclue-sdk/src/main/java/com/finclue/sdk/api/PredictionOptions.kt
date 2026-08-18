package com.finclue.sdk.api

/** Controls whether a field requests on-device predictive text. */
enum class PredictionMode {
    DISABLED,
    GENERAL,
    PERSONALIZED,
}

/** Semantic hint supplied to the language-model prompt and personal-history lookup. */
enum class PredictionContext(val storageKey: String) {
    GENERAL("general"),
    BANK_NAME("bank_name"),
    PERSON_NAME("person_name"),
    ORGANIZATION_NAME("organization_name"),
    ADDRESS("address"),
    PRODUCT_NAME("product_name"),
}

/**
 * Controls access to locally stored candidate selections.
 *
 * This policy is selected by the host. FIN:CLUE does not infer sensitivity from the
 * entered value. [NONE] is the safe default.
 */
enum class HistoryPolicy {
    NONE,
    READ_ONLY,
    READ_WRITE,
}

