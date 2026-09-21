package com.example.thenobbery.logic

import android.content.Context
import android.net.Uri
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class WatermarkPreferenceManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("watermark_prefs", Context.MODE_PRIVATE)

    fun saveWatermarkFile(uri: Uri): String? {
        val destinationFile = File(context.filesDir, "master_watermark.png")
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save watermark file")
            null
        }
    }

    fun getWatermarkPath(): String? {
        val file = File(context.filesDir, "master_watermark.png")
        return if (file.exists()) file.absolutePath else null
    }

    var lastOpacity: Float
        get() = prefs.getFloat("opacity", 0.5f)
        set(value) = prefs.edit().putFloat("opacity", value).apply()

    var lastScale: Float
        get() = prefs.getFloat("scale", 0.3f)
        set(value) = prefs.edit().putFloat("scale", value).apply()
}
