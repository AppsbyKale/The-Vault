package com.example.thenobbery.logic

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.thenobbery.data.Asset
import com.example.thenobbery.data.VaultDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class BulkImportEngine(private val context: Context) {

    private val dao = VaultDatabase.getDatabase(context).assetDao()

    suspend fun importFiles(uris: List<Uri>, onProgress: suspend (Int, Int) -> Unit) {
        val total = uris.size
        var current = 0

        val groupedUris = uris.groupBy { uri ->
            val rawName = getRealFileName(uri)
            rawName.replace(Regex("\\(\\d+\\)|copy"), "").substringBeforeLast(".").trim()
        }

        val batchSize = 10
        val groupedEntries = groupedUris.entries.toList()

        for (batchStart in groupedEntries.indices step batchSize) {
            val batchEnd = minOf(batchStart + batchSize, groupedEntries.size)
            val batch = groupedEntries.subList(batchStart, batchEnd)

            batch.forEach { (title, uriList) ->
                withContext(Dispatchers.IO) {
                    val existingAsset = dao.getAssetByTitle(title)
                    var asset = existingAsset ?: Asset(title = title)

                    uriList.forEach { uri ->
                        val tempFile = copyToInternalStorage(uri) ?: return@forEach
                        val slot = FileClassifier.classify(context, tempFile)
                        val fullPath = tempFile.absolutePath

                        asset = when (slot) {
                            FileClassifier.AssetSlot.SOURCE_PNTR -> asset.copy(pntrPaths = asset.pntrPaths + fullPath)
                            FileClassifier.AssetSlot.VECTOR_SVG -> asset.copy(svgPaths = asset.svgPaths + fullPath)
                            FileClassifier.AssetSlot.IMAGE_TRANSPARENT -> asset.copy(transparentPngPaths = asset.transparentPngPaths + fullPath)
                            FileClassifier.AssetSlot.IMAGE_BACKGROUND -> asset.copy(backgroundPngPaths = asset.backgroundPngPaths + fullPath)
                            FileClassifier.AssetSlot.UNKNOWN -> asset
                        }
                    }

                    if (asset.uid == 0L) {
                        dao.insertAsset(asset)
                    } else {
                        dao.updateAsset(asset)
                    }
                }

                current += uriList.size
                onProgress(current, total)
            }
        }
    }

    private fun getRealFileName(uri: Uri): String {
        var name = ""
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) name = cursor.getString(nameIndex)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to resolve file name for $uri")
        }

        return if (name.isEmpty()) uri.path?.substringAfterLast('/') ?: "unknown" else name
    }

    private fun copyToInternalStorage(uri: Uri): File? {
        return try {
            val originalName = getRealFileName(uri)
            val safeFileName = makeUniqueFileName(context.filesDir, originalName)
            val destFile = File(context.filesDir, safeFileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to import file from $uri")
            null
        }
    }

    private fun makeUniqueFileName(filesDir: File, originalName: String): String {
        var fileName = originalName
        var counter = 1
        while (File(filesDir, fileName).exists()) {
            val nameWithoutExt = originalName.substringBeforeLast(".")
            val ext = originalName.substringAfterLast(".", "")
            fileName = if (ext.isNotEmpty()) "${nameWithoutExt}_$counter.$ext" else "${nameWithoutExt}_$counter"
            counter++
        }
        return fileName
    }
}
