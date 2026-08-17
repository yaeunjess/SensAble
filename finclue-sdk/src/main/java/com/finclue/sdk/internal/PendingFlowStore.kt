package com.finclue.sdk.internal

import com.finclue.sdk.api.FlowResult
import com.finclue.sdk.api.FlowSpec
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal data class PendingFlow(
    val spec: FlowSpec,
    val callback: (FlowResult) -> Unit,
)

internal object PendingFlowStore {
    private val flows = ConcurrentHashMap<String, PendingFlow>()

    fun put(flow: PendingFlow): String = UUID.randomUUID().toString().also { flows[it] = flow }
    fun get(id: String): PendingFlow? = flows[id]
    fun remove(id: String): PendingFlow? = flows.remove(id)
}
