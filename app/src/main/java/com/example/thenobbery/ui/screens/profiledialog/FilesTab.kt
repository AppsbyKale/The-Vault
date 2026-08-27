package com.example.thenobbery.ui.screens.profiledialog

import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

data class AssetFile(
    val type: FileType,
    val paths: List<String>,
    val label: String
)

enum class FileType {
    BACKGROUND, TRANSPARENT, PNTR, SVG, OTHER
}

@Composable
fun FilesTabContent(
    asset: Asset,
    onUpdateAsset: (Asset) -> Unit,
    onDeleteFile: (Asset, FileType) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var pendingFileType by remember { mutableStateOf<FileType?>(null) }
    val context = LocalContext.current

    val files = remember(asset) {
        listOf(
            AssetFile(FileType.BACKGROUND, asset.backgroundPngPaths, "Background PNG"),
            AssetFile(FileType.TRANSPARENT, asset.transparentPngPaths, "Transparent PNG"),
            AssetFile(FileType.PNTR, asset.pntrPaths, "Infinite Painter (.pntr)"),
            AssetFile(FileType.SVG, asset.svgPaths, "Vector (.svg)"),
            AssetFile(FileType.OTHER, if (asset.mainPreviewPath != null) listOf(asset.mainPreviewPath!!) else emptyList(), "Other")
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingFileType?.let { type ->
            if (uris.isEmpty()) return@let
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val newPaths = uris.mapNotNull { uri ->
                    copyToAppStorage(context, uri)?.absolutePath
                }
                if (newPaths.isNotEmpty()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val updatedAsset = when (type) {
                            FileType.BACKGROUND -> asset.copy(backgroundPngPaths = asset.backgroundPngPaths + newPaths)
                            FileType.TRANSPARENT -> asset.copy(transparentPngPaths = asset.transparentPngPaths + newPaths)
                            FileType.PNTR -> asset.copy(pntrPaths = asset.pntrPaths + newPaths)
                            FileType.SVG -> asset.copy(svgPaths = asset.svgPaths + newPaths)
                            FileType.OTHER -> asset.copy(mainPreviewPath = newPaths.first())
                        }
                        onUpdateAsset(updatedAsset)
                    }
                }
            }
        }
    }

    fun launchPicker(type: FileType) {
        pendingFileType = type
        filePickerLauncher.launch(arrayOf("image/*", "application/octet-stream"))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "ASSET FILES",
            color = VaultSilver,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files) { file ->
                FileItem(
                    file = file,
                    onAdd = { launchPicker(file.type) },
                    onRemove = { path ->
                        if (path.isNotEmpty()) {
                            showDeleteConfirm = path
                        }
                    }
                )
            }
        }
    }

    if (showDeleteConfirm != null) {
        val pathToDelete = showDeleteConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor = VaultBlack,
            modifier = Modifier.border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
            title = { Text("Remove File?", color = VaultWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove the file from this asset. The file will not be deleted from storage.", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    val updatedAsset = when {
                        pathToDelete in asset.backgroundPngPaths -> asset.copy(backgroundPngPaths = asset.backgroundPngPaths - pathToDelete)
                        pathToDelete in asset.transparentPngPaths -> asset.copy(transparentPngPaths = asset.transparentPngPaths - pathToDelete)
                        pathToDelete in asset.pntrPaths -> asset.copy(pntrPaths = asset.pntrPaths - pathToDelete)
                        pathToDelete in asset.svgPaths -> asset.copy(svgPaths = asset.svgPaths - pathToDelete)
                        else -> asset
                    }
                    onUpdateAsset(updatedAsset)
                    showDeleteConfirm = null
                }) {
                    Text("REMOVE", color = AccentBack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }
}

private fun copyToAppStorage(context: Context, uri: android.net.Uri): File? {
    return try {
        val originalName = getFileName(context, uri) ?: "file_${System.currentTimeMillis()}"
        val uniqueName = makeUniqueFileName(context.filesDir, originalName)
        val destFile = File(context.filesDir, uniqueName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile
    } catch (e: Exception) {
        Timber.e(e, "Failed to copy file to internal storage")
        null
    }
}

private fun getFileName(context: Context, uri: android.net.Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) name = cursor.getString(nameIndex)
    }
    return name
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

@Composable
private fun FileItem(
    file: AssetFile,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    val existingFiles = file.paths.filter { File(it).exists() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
        color = VaultSurface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (existingFiles.isNotEmpty()) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (existingFiles.isNotEmpty()) VaultWhite else VaultSilver,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            file.label,
                            color = VaultWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${existingFiles.size} file(s)",
                            color = VaultSilver,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(onClick = { onAdd("") }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = VaultWhite
                    )
                }
            }

            if (existingFiles.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                existingFiles.forEachIndexed { index, path ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            File(path).name,
                            color = VaultSilver,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onRemove(path) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = AccentBack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
