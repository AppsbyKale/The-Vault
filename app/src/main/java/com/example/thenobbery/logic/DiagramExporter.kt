package com.example.thenobbery.logic

import android.content.Context
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object DiagramExporter {
    /**
     * Copies the architecture diagram from raw resources into the cache directory
     * so it can be shared or downloaded.
     */
    fun exportArchitectureDiagram(context: Context): File? {
        return try {
            val inputStream = context.resources.openRawResource(
                context.resources.getIdentifier(
                    "architecture_diagram",
                    "raw",
                    context.packageName
                )
            )

            val cacheDir = context.cacheDir
            val diagramFile = File(cacheDir, "TheNobberyVault_Architecture_Diagram.txt")

            inputStream.use { input ->
                FileOutputStream(diagramFile).use { output ->
                    input.copyTo(output)
                }
            }

            diagramFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to export architecture diagram")
            null
        }
    }

    /**
     * Returns a shareable URI for the exported diagram file.
     */
    fun getDiagramFileUri(context: Context): android.net.Uri? {
        val file = exportArchitectureDiagram(context) ?: return null

        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create diagram share URI")
            null
        }
    }
}
