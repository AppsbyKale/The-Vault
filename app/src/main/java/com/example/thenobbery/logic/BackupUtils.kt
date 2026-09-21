package com.example.thenobbery.logic

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.thenobbery.data.Asset
import com.example.thenobbery.data.VaultDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupUtils {
    private const val BUFFER_SIZE = 8192
    private const val BACKUP_VERSION = 2

    private fun sanitizeFileName(name: String): String {
        return name
            .replace(Regex("[\\\\/:*?\"<>|]"), "")
            .trim()
    }

    // Returns a deterministic, unique backup file name for a given vault file path.
    // The original on-disk file name is included. Vault file names are globally unique
    // (BulkImportEngine.makeUniqueFileName) and sanitizeFileName is injective for real
    // filesystem names, so two different files never collapse to the same zip entry even
    // when assets share a title. Manifest + restore both use this, so they stay in sync.
    private fun getBackupFileNameFromPath(path: String, asset: Asset, defaultType: String): String {
        val lowerPath = path.lowercase()
        val type = when {
            lowerPath.contains("transparent") -> "transparent"
            lowerPath.contains("background") || lowerPath.contains("_b.") -> "background"
            lowerPath.endsWith(".pntr") -> "pntr"
            lowerPath.endsWith(".svg") -> "svg"
            lowerPath.contains("preview") -> "preview"
            else -> defaultType
        }
        val originalBase = File(path).name
        val cleanTitle = sanitizeFileName(asset.title)
        val cleanOriginalBase = sanitizeFileName(originalBase)
        return "$cleanTitle-$type-$cleanOriginalBase"
    }

    suspend fun exportCollection(
        context: Context,
        onProgress: (String, Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress("Loading assets...", 0f)
            val dao = VaultDatabase.getDatabase(context).assetDao()
            val assets = dao.getAllAssets().first()

            if (assets.isEmpty()) {
                return@withContext Result.failure(Exception("No assets to export"))
            }

            val appFilesDir = context.filesDir

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
            val dateSuffix = dateFormat.format(Date())
            val zipName = "Nobbery_Backup_$dateSuffix.zip"
            val tempZipFile = File(context.cacheDir, zipName)

            onProgress("Creating backup...", 0.1f)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile), BUFFER_SIZE)).use { zos ->
                val json = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("exportDate", System.currentTimeMillis())
                    put("count", assets.size)
                    put("assets", JSONArray().apply {
                        assets.forEach { asset ->
                            put(JSONObject().apply {
                                put("uid", asset.uid)
                                put("title", asset.title)
                                put("timestamp", asset.timestamp)
                                put("sourceLinkId", asset.sourceLinkId)
                                put("pntrFileNames", JSONArray().apply { asset.pntrPaths.forEach { put(getBackupFileNameFromPath(it, asset, "pntr")) } })
                                put("svgFileNames", JSONArray().apply { asset.svgPaths.forEach { put(getBackupFileNameFromPath(it, asset, "svg")) } })
                                put("transparentFileNames", JSONArray().apply { asset.transparentPngPaths.forEach { put(getBackupFileNameFromPath(it, asset, "transparent")) } })
                                put("backgroundFileNames", JSONArray().apply { asset.backgroundPngPaths.forEach { put(getBackupFileNameFromPath(it, asset, "background")) } })
                                put("previewFileName", asset.mainPreviewPath?.let { getBackupFileNameFromPath(it, asset, "preview") } ?: "")
                                put("tags", asset.tags)
                                put("collections", asset.collections)
                                put("lore", asset.lore)
                                put("notes", asset.notes)
                                put("classification", asset.classification)
                            })
                        }
                    })
                }

                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(json.toString(2).toByteArray())
                zos.closeEntry()

                val processedFiles = mutableSetOf<String>()
                var fileCount = 0
                val totalFiles = assets.sumOf { asset ->
                    asset.pntrPaths.size + asset.svgPaths.size + asset.transparentPngPaths.size + asset.backgroundPngPaths.size +
                    if (asset.mainPreviewPath != null) 1 else 0
                }

                assets.forEach { asset ->
                    asset.pntrPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "pntr")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.svgPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "svg")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.transparentPngPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "transparent")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.backgroundPngPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "background")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.mainPreviewPath?.let { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "preview")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                }
            }

            onProgress("Saving to Downloads...", 0.95f)

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, zipName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    // Keep the file hidden until it is fully written to avoid a broken/empty zip.
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri == null) {
                return@withContext Result.failure(Exception("Could not create the backup file in Downloads"))
            }

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    tempZipFile.inputStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
            } catch (e: Exception) {
                // Clean up the partially-created file so Downloads isn't left with a broken zip.
                runCatching { resolver.delete(uri, null, null) }
                tempZipFile.delete()
                return@withContext Result.failure(Exception("Failed to write backup to Downloads: ${e.message}"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val doneValues = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, doneValues, null, null)
            }

            tempZipFile.delete()
            onProgress("Complete!", 1f)
            Result.success(uri.toString())
        } catch (e: Exception) {
            Timber.e(e, "Failed to export backup to Downloads")
            Result.failure(e)
        }
    }

    suspend fun exportCollectionToUri(
        context: Context,
        outputUri: Uri,
        onProgress: (String, Float) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var tempZipFile: File? = null
        try {
            onProgress("Loading assets...", 0f)
            val dao = VaultDatabase.getDatabase(context).assetDao()
            val assets = dao.getAllAssets().first()

            if (assets.isEmpty()) {
                return@withContext Result.failure(Exception("No assets to export"))
            }

            tempZipFile = File(context.cacheDir, "temp_backup.zip")

            onProgress("Creating backup...", 0.1f)

            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempZipFile), BUFFER_SIZE)).use { zos ->
                val json = JSONObject().apply {
                    put("version", BACKUP_VERSION)
                    put("exportDate", System.currentTimeMillis())
                    put("count", assets.size)
                    put("assets", JSONArray().apply {
                        assets.forEach { asset ->
                            put(JSONObject().apply {
                                put("uid", asset.uid)
                                put("title", asset.title)
                                put("timestamp", asset.timestamp)
                                put("sourceLinkId", asset.sourceLinkId)
                                put("pntrFileNames", JSONArray().apply { asset.pntrPaths.forEach { put(getBackupFileNameFromPath(it, asset, "pntr")) } })
                                put("svgFileNames", JSONArray().apply { asset.svgPaths.forEach { put(getBackupFileNameFromPath(it, asset, "svg")) } })
                                put("transparentFileNames", JSONArray().apply { asset.transparentPngPaths.forEach { put(getBackupFileNameFromPath(it, asset, "transparent")) } })
                                put("backgroundFileNames", JSONArray().apply { asset.backgroundPngPaths.forEach { put(getBackupFileNameFromPath(it, asset, "background")) } })
                                put("previewFileName", asset.mainPreviewPath?.let { getBackupFileNameFromPath(it, asset, "preview") } ?: "")
                                put("tags", asset.tags)
                                put("collections", asset.collections)
                                put("lore", asset.lore)
                                put("notes", asset.notes)
                                put("classification", asset.classification)
                            })
                        }
                    })
                }

                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(json.toString(2).toByteArray())
                zos.closeEntry()

                val processedFiles = mutableSetOf<String>()
                var fileCount = 0
                val totalFiles = assets.sumOf { asset ->
                    asset.pntrPaths.size + asset.svgPaths.size + asset.transparentPngPaths.size + asset.backgroundPngPaths.size +
                    if (asset.mainPreviewPath != null) 1 else 0
                }

                assets.forEach { asset ->
                    asset.pntrPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "pntr")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.svgPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "svg")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.transparentPngPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "transparent")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.backgroundPngPaths.forEach { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "background")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                    asset.mainPreviewPath?.let { path ->
                        val fileName = File(path).name
                        if (fileName !in processedFiles && File(path).exists()) {
                            processedFiles.add(fileName)
                            val backupName = getBackupFileNameFromPath(path, asset, "preview")
                            zos.putNextEntry(ZipEntry("files/$backupName"))
                            File(path).inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                            fileCount++
                            onProgress("Adding files... $fileCount/$totalFiles", 0.1f + 0.8f * fileCount / totalFiles)
                        }
                    }
                }
            }

            onProgress("Saving to selected location...", 0.95f)

            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                tempZipFile.inputStream().use { input ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            
            tempZipFile.delete()
            onProgress("Complete!", 1f)
            Result.success(outputUri.toString())
        } catch (e: Exception) {
            Timber.e(e, "Failed to export backup to selected location")
            tempZipFile?.delete()
            Result.failure(e)
        }
    }

    suspend fun importCollection(
        context: Context,
        zipUri: Uri,
        onProgress: (String, Float) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        var tempZipFile: File? = null
        try {
            val appFilesDir = context.filesDir
            if (!appFilesDir.exists()) appFilesDir.mkdirs()

            onProgress("Reading backup...", 0f)

            tempZipFile = File(context.cacheDir, "temp_restore.zip")
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }

            onProgress("Extracting files...", 0.1f)

            var manifestJson: JSONObject? = null
            val extractedFiles = mutableMapOf<String, File>()

            ZipInputStream(BufferedInputStream(FileInputStream(tempZipFile), BUFFER_SIZE)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "manifest.json" -> {
                            manifestJson = JSONObject(zis.bufferedReader().readText())
                        }
                        entry.name.startsWith("files/") && !entry.isDirectory -> {
                            val fileName = entry.name.removePrefix("files/")
                            val destFile = File(appFilesDir, fileName)
                            destFile.parentFile?.mkdirs()
                            FileOutputStream(destFile).use { fos ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var bytesRead: Int
                                while (zis.read(buffer).also { bytesRead = it } != -1) {
                                    fos.write(buffer, 0, bytesRead)
                                }
                            }
                            extractedFiles[fileName] = destFile
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            tempZipFile.delete()

            val json = manifestJson ?: return@withContext Result.failure(Exception("Missing manifest in backup"))
            val assetCount = json.optInt("count", 0)

            if (assetCount == 0) {
                return@withContext Result.failure(Exception("No assets found in backup"))
            }

            onProgress("Preparing $assetCount assets...", 0.3f)

            val assetsArray = json.getJSONArray("assets")
            val assets = mutableListOf<Asset>()

            for (i in 0 until assetsArray.length()) {
                val a = assetsArray.getJSONObject(i)

                val originalUid = a.optLong("uid", 0)
                val sourceLinkId = a.optLong("sourceLinkId", 0)

                // Handle both old format (single file) and new format (list of files)
                val pntrPaths = mutableListOf<String>()
                val svgPaths = mutableListOf<String>()
                val transparentPngPaths = mutableListOf<String>()
                val backgroundPngPaths = mutableListOf<String>()

                // Try new format first (list)
                val pntrFiles = a.optJSONArray("pntrFileNames")
                val svgFiles = a.optJSONArray("svgFileNames")
                val transparentFiles = a.optJSONArray("transparentFileNames")
                val backgroundFiles = a.optJSONArray("backgroundFileNames")

                if (pntrFiles != null) {
                    for (i in 0 until pntrFiles.length()) {
                        pntrFiles.getString(i).ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> pntrPaths.add(path) } }
                    }
                } else {
                    // Fallback to old single file format
                    a.optString("pntrFileName").ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> pntrPaths.add(path) } }
                }
                if (svgFiles != null) {
                    for (i in 0 until svgFiles.length()) {
                        svgFiles.getString(i).ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> svgPaths.add(path) } }
                    }
                } else {
                    a.optString("svgFileName").ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> svgPaths.add(path) } }
                }
                if (transparentFiles != null) {
                    for (i in 0 until transparentFiles.length()) {
                        transparentFiles.getString(i).ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> transparentPngPaths.add(path) } }
                    }
                } else {
                    a.optString("transparentFileName").ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> transparentPngPaths.add(path) } }
                }
                if (backgroundFiles != null) {
                    for (i in 0 until backgroundFiles.length()) {
                        backgroundFiles.getString(i).ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> backgroundPngPaths.add(path) } }
                    }
                } else {
                    a.optString("backgroundFileName").ifEmpty { null }?.let { extractedFiles[it]?.absolutePath?.let { path -> backgroundPngPaths.add(path) } }
                }

                val asset = Asset(
                    uid = originalUid,
                    title = a.optString("title"),
                    timestamp = a.optLong("timestamp", System.currentTimeMillis()),
                    sourceLinkId = sourceLinkId,
                    pntrPaths = pntrPaths,
                    svgPaths = svgPaths,
                    transparentPngPaths = transparentPngPaths,
                    backgroundPngPaths = backgroundPngPaths,
                    mainPreviewPath = a.optString("previewFileName").ifEmpty { null }?.let { extractedFiles[it]?.absolutePath },
                    tags = a.optString("tags"),
                    collections = a.optString("collections"),
                    lore = a.optString("lore"),
                    notes = a.optString("notes"),
                    classification = a.optString("classification")
                )

                assets.add(asset)
            }

            onProgress("Saving to database...", 0.6f)

            val dao = VaultDatabase.getDatabase(context).assetDao()
            dao.insertAll(assets)

            onProgress("Complete!", 1f)
            Result.success(assetCount)
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore backup")
            tempZipFile?.delete()
            Result.failure(e)
        }
    }
}
