package com.finclue.sdk.api

/** Describes one SDK-owned accessible input flow. */
data class FlowSpec(
    val fields: List<FieldSpec>,
) {
    init {
        require(fields.isNotEmpty()) { "FlowSpec requires at least one field." }
        require(fields.map(FieldSpec::key).distinct().size == fields.size) {
            "FieldSpec keys must be unique within a flow."
        }
    }
}

data class FieldSpec(
    val key: String,
    val type: FieldType,
    val label: String,
    val prompt: String,
) {
    init {
        require(key.isNotBlank()) { "FieldSpec.key must not be blank." }
        require(label.isNotBlank()) { "FieldSpec.label must not be blank." }
        require(prompt.isNotBlank()) { "FieldSpec.prompt must not be blank." }
    }
}

enum class FieldType {
    TEXT,
    ACCOUNT,
    AMOUNT,
    PIN,
}
