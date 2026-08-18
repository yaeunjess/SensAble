package com.finclue.sdk.prediction

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

internal class BundledModelInstaller(context: Context) {
    private val appContext = context.applicationContext

    suspend fun installIfNeeded(): File = withContext(Dispatchers.IO) {
        val modelDirectory = File(appContext.filesDir, "finclue/prediction/model")
        val modelFile = File(modelDirectory, MODEL_FILE_NAME)
        val markerFile = File(modelDirectory, "$MODEL_FILE_NAME.sha256")
        if (modelFile.length() == MODEL_SIZE && markerFile.readTextOrNull() == MODEL_SHA256) {
            return@withContext modelFile
        }

        modelDirectory.mkdirs()
        val temporaryFile = File(modelDirectory, "$MODEL_FILE_NAME.tmp")
        temporaryFile.delete()
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            appContext.assets.open(MODEL_ASSET_PATH).use { input ->
                FileOutputStream(temporaryFile).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            check(temporaryFile.length() == MODEL_SIZE && actualHash == MODEL_SHA256) {
                "Bundled prediction model failed integrity validation."
            }
            if (modelFile.exists()) check(modelFile.delete()) { "Could not replace prediction model." }
            check(temporaryFile.renameTo(modelFile)) { "Could not install prediction model." }
            markerFile.writeText(MODEL_SHA256)
            modelFile
        } finally {
            temporaryFile.delete()
        }
    }

    private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

    private companion object {
        const val MODEL_FILE_NAME = "exaone-4.0-1.2b-q4_k_m.gguf"
        const val MODEL_ASSET_PATH = "finclue/prediction/model/$MODEL_FILE_NAME"
        const val MODEL_SIZE = 812_437_792L
        const val MODEL_SHA256 = "7b5e753540183ae4d56e6febd9b48cdd944de53386e6faa8f51c8f98cb2b47df"
    }
}
