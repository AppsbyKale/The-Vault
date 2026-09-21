package com.example.thenobbery.logic

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.thenobbery.MainActivity
import com.example.thenobbery.data.Asset
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileExportUtils {

    fun exportOrShare(
        context: Context,
        activity: Activity,
        assets: List<Asset>,
        selectedTypes: List<String>,
        useWatermark: Boolean,
        isShare: Boolean
    ) {
        val tempDir = File(context.cacheDir, "temp_export")
        if (tempDir.exists()) tempDir.deleteRecursively()
        tempDir.mkdirs()

        val filesToExport = mutableListOf<Pair<String, String>>()

        // Collect all files from all assets
        assets.forEach { asset ->
            selectedTypes.forEach { type ->
                val sourcePaths = when (type) {
                    "transparent" -> asset.transparentPngPaths
                    "pntr" -> asset.pntrPaths
                    else -> asset.backgroundPngPaths
                }
                if (sourcePaths.isEmpty()) return@forEach

                val baseName = asset.title.replace(Regex("[^a-zA-Z0-9\\-]"), "_")
                val typeSuffix = when (type) {
                    "transparent" -> "Transparent"
                    "pntr" -> "Pntr"
                    else -> ""
                }
                val extension = if (type == "pntr") ".pntr" else ".png"
                val suffix = if (useWatermark) "-Watermarked" else ""

                sourcePaths.forEachIndexed { index, sourcePath ->
                    val fileNameSuffix = if (sourcePaths.size > 1) "-${index + 1}" else ""
                    val fileName = if (assets.size > 1) {
                        "${baseName}-${typeSuffix}${fileNameSuffix}${suffix}$extension"
                    } else {
                        "${baseName}${if (typeSuffix.isNotEmpty()) "-$typeSuffix" else ""}${fileNameSuffix}${suffix}$extension"
                    }

                    val destFile = File(tempDir, fileName)
                    File(sourcePath).copyTo(destFile, overwrite = true)
                    filesToExport.add(destFile.absolutePath to fileName)
                }
            }
        }

        if (filesToExport.isEmpty()) return

        // Single file = deliver directly, multiple = ZIP
        if (filesToExport.size == 1 && !isShare) {
            val (path, name) = filesToExport.first()
            if (activity is MainActivity) {
                activity.enqueueExport(listOf(path to name))
            }
        } else {
            val dateSuffix = java.text.SimpleDateFormat("MM.dd.yyyy", java.util.Locale.US).format(java.util.Date())
            val zipName = if (assets.size > 1) {
                "${assets.first().title.replace(Regex("[^a-zA-Z0-9]"), "_")}_and_${assets.size - 1}_more-$dateSuffix"
            } else {
                "${assets.first().title.replace(Regex("[^a-zA-Z0-9]"), "_")}-$dateSuffix"
            }
            if (isShare) {
                createZipAndShare(context, filesToExport, zipName)
            } else {
                createZipAndSave(context, activity, filesToExport, zipName)
            }
        }

        // Cleanup temp files
        tempDir.deleteRecursively()
    }

    fun createZipAndShare(
        context: Context,
        files: List<Pair<String, String>>,
        zipName: String
    ) {
        try {
            val zipFile = File(context.cacheDir, "$zipName.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                files.forEach { (filePath, fileName) ->
                    val file = File(filePath)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(fileName))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            shareFile(context, zipFile)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create and share zip")
        }
    }

    fun createZipAndSave(
        context: Context,
        activity: Activity,
        files: List<Pair<String, String>>,
        zipName: String
    ) {
        try {
            val zipFile = File(context.cacheDir, "$zipName.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                files.forEach { (filePath, fileName) ->
                    val file = File(filePath)
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(fileName))
                        FileInputStream(file).use { fis -> fis.copyTo(zos) }
                        zos.closeEntry()
                    }
                }
            }
            if (activity is MainActivity) {
                activity.enqueueExport(listOf(zipFile.absolutePath to "$zipName.zip"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create and save zip")
        }
    }

    fun shareFile(context: Context, file: File) {
        if (!file.exists()) return
        val contentUri = FileProvider.getUriForFile(
            context, "com.thevault.app.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }

    fun writeFileToUri(context: Context, sourcePath: String, targetUri: Uri) {
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                FileInputStream(File(sourcePath)).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to write file to URI")
        }
    }
}
