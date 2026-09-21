package com.example.thenobbery.logic

import android.content.Context
import android.net.Uri
import java.io.File

object UriListPersistence {
    private const val PREFIX = "import_uris_"
    private const val EXTENSION = ".txt"

    fun saveUris(context: Context, uris: List<Uri>): String {
        val fileName = "$PREFIX${System.currentTimeMillis()}$EXTENSION"
        val file = File(context.cacheDir, fileName)
        
        val uriStrings = uris.joinToString("\n") { it.toString() }
        file.writeText(uriStrings)
        
        return fileName
    }

    fun loadUris(context: Context, fileName: String): List<Uri> {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) {
            return emptyList()
        }
        
        return try {
            file.readText()
                .split("\n")
                .filter { it.isNotBlank() }
                .map { Uri.parse(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteFile(context: Context, fileName: String) {
        val file = File(context.cacheDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }
}
