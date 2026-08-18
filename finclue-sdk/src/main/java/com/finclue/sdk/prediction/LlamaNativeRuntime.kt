package com.finclue.sdk.prediction

/**
 * Thin JNI boundary for the on-device llama.cpp runtime.
 *
 * Model ownership and candidate scoring will be added behind this boundary so
 * callers never depend directly on llama.cpp types.
 */
internal object LlamaNativeRuntime {
    init {
        System.loadLibrary("finclue_prediction")
    }

    external fun nativeRuntimeVersion(): String

    external fun loadModel(modelPath: String, contextSize: Int, threadCount: Int)

    external fun unloadModel()

    external fun scoreCandidates(conditioningText: String, candidates: Array<String>): FloatArray
}
