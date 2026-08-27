package com.example.thenobbery.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.MainActivity
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.BitmapUtils
import com.example.thenobbery.logic.FileExportUtils
import com.example.thenobbery.ui.screens.menus.DeleteWarningDialog
import com.example.thenobbery.ui.screens.menus.VaultActionMenu
import com.example.thenobbery.ui.screens.profiledialog.AssetProfileDialog
import com.example.thenobbery.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryDialogDispatcher(
    activity: MainActivity,
    activeActionAsset: Asset?,
    showProfileIndex: Int?,
    showDeleteConfirm: Asset?,
    mergingInitiator: Asset?,
    linkingInitiator: Asset?,
    exportingAsset: Asset?,
    studioAsset: Asset?,
    assets: List<Asset>?,
    onDismissActionMenu: () -> Unit,
    onDismissProfile: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissMerge: () -> Unit,
    onDismissLink: () -> Unit,
    onDismissExport: () -> Unit,
    onDismissStudio: () -> Unit,
    onUpdateAsset: (Asset) -> Unit,
    onDeleteAsset: (Asset) -> Unit,
    onUnlinkAsset: (Long) -> Unit,
    onLinkRequested: (Asset) -> Unit,
    onMergeRequested: (Asset) -> Unit,
    onExportRequested: (Asset) -> Unit,
    onLaunchStudio: (Asset) -> Unit,
    onLinkAssets: (Long, Long) -> Unit,
    onMergeAssets: (Asset, Asset) -> Unit,
    onDeleteFile: (Asset, com.example.thenobbery.ui.screens.profiledialog.FileType) -> Unit,
    onDismissSvg: () -> Unit = {},
    onExportSvg: (Asset, String, Boolean) -> Unit = { _, _, _ -> },
    bulkExportAssets: List<Asset> = emptyList()
) {
    val context = LocalContext.current
    val assetList = assets ?: emptyList()

    // Internal states for lineage warnings
    var pendingUnlinkAsset by remember { mutableStateOf<Asset?>(null) }
    var pendingLinkConfirmation by remember { mutableStateOf<Pair<Asset, Asset>?>(null) }
    // Track if we need to open profile from action menu
    var pendingProfileAsset by remember { mutableStateOf<Asset?>(null) }
    var pendingSvgAsset by remember { mutableStateOf<Asset?>(null) }
    
    // Watermark studio state: tracks pending export after watermark is applied
    var studioLaunchedAsset by remember { mutableStateOf<Asset?>(null) }
    var pendingExportAction by remember { mutableStateOf<String?>(null) } // "device" or "share"
    var pendingExportTypes by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingSelectedFileType by remember { mutableStateOf("background") }
    var pendingBulkAssetsForWatermark by remember { mutableStateOf<List<Asset>>(emptyList()) }
    val scope = rememberCoroutineScope()
    
    // Bulk export state
    val isBulkExport = bulkExportAssets.isNotEmpty()
    val exportAssetCount = if (isBulkExport) bulkExportAssets.size else 1
    
    // Progress state for bulk export
    var showExportProgress by remember { mutableStateOf(false) }
    var exportProgressText by remember { mutableStateOf("") }

    // 1. Vault Action Menu
    activeActionAsset?.let { asset ->
        VaultActionMenu(
            asset = asset,
            onDismiss = onDismissActionMenu,
            onProfile = { 
                pendingProfileAsset = it
                onDismissActionMenu()
            },
            onLink = { 
                if (asset.isChild()) pendingUnlinkAsset = asset 
                else onLinkRequested(asset)
            },
            onMerge = { onMergeRequested(asset) },
            onExport = { onExportRequested(asset) },
            onConvertToSvg = { pendingSvgAsset = it },
            onDelete = { onDeleteAsset(asset) }
        )
    }

    // 2. Link Picker Dialog
    linkingInitiator?.let { initiator ->
        LinkPickerDialog(
            initiator = initiator,
            allAssets = assetList,
            onDismiss = onDismissLink,
            onLinkSelected = { parent -> pendingLinkConfirmation = Pair(initiator, parent) },
            onToggleLink = { target, shouldLink ->
                if (shouldLink) onLinkAssets(target.uid, initiator.uid)
                else pendingUnlinkAsset = target
            }
        )
    }

    // 3. Merge Picker Dialog
    mergingInitiator?.let { initiator ->
        MergePickerSheet(
            targetAsset = initiator,
            potentialParents = assetList,
            onAssetSelected = { target -> 
                onMergeAssets(initiator, target)
                onDismissMerge()
            },
            onDismiss = onDismissMerge
        )
    }

    // 4. Unlink Warning
    pendingUnlinkAsset?.let { child ->
        AlertDialog(
            onDismissRequest = { pendingUnlinkAsset = null },
            title = { Text("ARE YOU SURE?", color = VaultWhite, fontWeight = FontWeight.Black) },
            text = { Text("Unlink ${child.title} from its parent? It will become standalone.", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    onUnlinkAsset(child.uid)
                    pendingUnlinkAsset = null
                    onDismissActionMenu()
                }) { Text("UNLINK", color = Color(0xFFE57373), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnlinkAsset = null }) { Text("CANCEL", color = VaultSilver) }
            },
            containerColor = VaultSurface
        )
    }

    // 5. Link Confirmation
    pendingLinkConfirmation?.let { (child, parent) ->
        AlertDialog(
            onDismissRequest = { pendingLinkConfirmation = null },
            title = { Text("CONFIRM LINK", color = VaultWhite, fontWeight = FontWeight.Black) },
            text = { Text("${child.title} will be linked to ${parent.title}.", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    onLinkAssets(child.uid, parent.uid)
                    pendingLinkConfirmation = null
                    onDismissLink()
                }) { Text("OK", color = VaultWhite, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingLinkConfirmation = null }) { Text("CANCEL", color = VaultSilver) }
            },
            containerColor = VaultSurface
        )
    }

    // 6. Delete Confirmation
    showDeleteConfirm?.let { asset ->
        DeleteWarningDialog(
            asset = asset,
            onConfirm = { 
                onDeleteAsset(asset)
                onDismissDelete()
            },
            onDismiss = onDismissDelete
        )
    }

    // 8. Export Dialog
    exportingAsset?.let { asset ->
        ExportDispatcher(
            asset = asset,
            isBulkExport = isBulkExport,
            bulkAssetCount = exportAssetCount,
            bulkAssets = bulkExportAssets,
            onExportToDevice = { _, selectedTypes, useWatermark ->
                scope.launch {
                    val assetsToExport = if (isBulkExport) bulkExportAssets else listOf(asset)
                    val fileCount = assetsToExport.size * selectedTypes.size
                    showExportProgress = true
                    exportProgressText = "Preparing $fileCount files..."
                    withContext(Dispatchers.IO) {
                        FileExportUtils.exportOrShare(context, activity, assetsToExport, selectedTypes, useWatermark, false)
                    }
                    showExportProgress = false
                    onDismissExport()
                }
            },
            onShareSheet = { _, selectedTypes, useWatermark ->
                scope.launch {
                    val assetsToExport = if (isBulkExport) bulkExportAssets else listOf(asset)
                    val fileCount = assetsToExport.size * selectedTypes.size
                    showExportProgress = true
                    exportProgressText = "Preparing $fileCount files..."
                    withContext(Dispatchers.IO) {
                        FileExportUtils.exportOrShare(context, activity, assetsToExport, selectedTypes, useWatermark, true)
                    }
                    showExportProgress = false
                    onDismissExport()
                }
            },
            onLaunchWatermarkStudio = { assetForStudio, bulkAssetsForStudio, selectedTypes, actionType ->
                studioLaunchedAsset = assetForStudio
                pendingExportTypes = selectedTypes
                pendingExportAction = actionType
                pendingSelectedFileType = selectedTypes.firstOrNull() ?: "background"
                // Store bulk assets for watermark processing
                pendingBulkAssetsForWatermark = bulkAssetsForStudio.ifEmpty { listOf(assetForStudio) }
                onDismissExport()
            },
            onDismiss = onDismissExport
        )
    }

    // 9. Watermark Studio Dialog
    studioLaunchedAsset?.let { asset ->
        WatermarkStudioDialog(
            asset = asset,
            selectedFileType = pendingSelectedFileType,
            onDismiss = {
                studioLaunchedAsset = null
                pendingExportAction = null
                pendingExportTypes = emptyList()
                pendingSelectedFileType = "background"
            },
            onFinalize = { opacity, scale, offset, containerSize, imageSize ->
                scope.launch {
                    val wmManager = com.example.thenobbery.logic.WatermarkPreferenceManager(context)
                    val watermarkPath = wmManager.getWatermarkPath()

                    if (watermarkPath != null) {
                        val tempDir = File(context.cacheDir, "watermark_temp")
                        if (tempDir.exists()) tempDir.deleteRecursively()
                        tempDir.mkdirs()

                        val filesToExport = mutableListOf<Pair<String, String>>()

                        val assetsToProcess = pendingBulkAssetsForWatermark.ifEmpty { listOf(asset) }

                        showExportProgress = true
                        exportProgressText = "Applying watermarks..."

                        withContext(Dispatchers.IO) {
                            assetsToProcess.forEachIndexed { assetIndex, processAsset ->
                                pendingExportTypes.forEachIndexed { typeIndex, type ->
                                    exportProgressText = "Processing ${assetIndex + 1}/${assetsToProcess.size}..."
                                    val exportPath = if (type == "pntr") {
                                        val pntrSource = processAsset.primaryPntrPath
                                        if (pntrSource != null) {
                                            val safeTitle = processAsset.title.replace(Regex("[^a-zA-Z0-9\\-]"), "_")
                                            val destFile = File(tempDir, "${safeTitle}-Pntr.pntr")
                                            File(pntrSource).copyTo(destFile, overwrite = true)
                                            destFile.absolutePath
                                        } else null
                                    } else {
                                        BitmapUtils.applyWatermark(
                                            context = context,
                                            asset = processAsset,
                                            fileType = type,
                                            watermarkPath = watermarkPath,
                                            opacity = opacity,
                                            scaleFactor = scale,
                                            uiOffset = offset,
                                            containerSize = containerSize,
                                            imageSize = imageSize,
                                            isWatermarked = true
                                        )
                                    }

                                    if (exportPath != null) {
                                        val fileName = File(exportPath).name
                                        filesToExport.add(exportPath to fileName)
                                    }
                                }
                            }
                        }

                        if (filesToExport.isNotEmpty()) {
                            exportProgressText = "Creating ZIP..."
                            if (filesToExport.size == 1) {
                                val (path, name) = filesToExport.first()
                                withContext(Dispatchers.IO) {
                                    if (pendingExportAction == "device") {
                                        activity.enqueueExport(listOf(path to name))
                                    } else {
                                        FileExportUtils.shareFile(context, File(path))
                                    }
                                }
                                showExportProgress = false
                            } else {
                                val dateSuffix = java.text.SimpleDateFormat("MM.dd.yyyy", java.util.Locale.US).format(java.util.Date())
                                val zipName = if (assetsToProcess.size > 1) {
                                    "${assetsToProcess.first().title.replace(Regex("[^a-zA-Z0-9]"), "_")}_and_${assetsToProcess.size - 1}_more-$dateSuffix"
                                } else {
                                    "${asset.title.replace(Regex("[^a-zA-Z0-9]"), "_")}-$dateSuffix"
                                }
                                withContext(Dispatchers.IO) {
                                    if (pendingExportAction == "device") {
                                        FileExportUtils.createZipAndSave(context, activity, filesToExport, zipName)
                                    } else {
                                        FileExportUtils.createZipAndShare(context, filesToExport, zipName)
                                    }
                                }
                                showExportProgress = false
                            }
                        } else {
                            showExportProgress = false
                        }
                        tempDir.deleteRecursively()
                    }
                    studioLaunchedAsset = null
                    pendingExportAction = null
                    pendingExportTypes = emptyList()
                    pendingSelectedFileType = "background"
                    pendingBulkAssetsForWatermark = emptyList()
                }
            }
        )
    }
    
    // 10. Export Progress Dialog
    if (showExportProgress) {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = { },
            title = { 
                Text("PREPARING EXPORT", color = VaultWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = VaultWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (exportProgressText.isNotEmpty()) exportProgressText else "Processing files...",
                        color = VaultSilver,
                        fontSize = 14.sp
                    )
                }
            },
            containerColor = VaultSurface
        )
    }

    // 7. Profile Quick View
    showProfileIndex?.let { index ->
        if (index in assetList.indices) {
            AssetProfileDialog(
                pagerState = rememberPagerState(initialPage = index, pageCount = { assetList.size }),
                assets = assetList,
                onDismiss = onDismissProfile,
                onUpdateAsset = onUpdateAsset,
                onUnlink = { onUnlinkAsset(it.uid) },
                onLinkToParent = { child, parent -> onLinkAssets(child.uid, parent.uid) },
                onDeleteFile = onDeleteFile
            )
        }
    }

    // Handle pending profile asset from action menu
    pendingProfileAsset?.let { asset ->
        val index = assetList.indexOf(asset)
        if (index >= 0) {
            LaunchedEffect(pendingProfileAsset) {
                onDismissProfile()
            }
            AssetProfileDialog(
                pagerState = rememberPagerState(initialPage = index, pageCount = { assetList.size }),
                assets = assetList,
                onDismiss = {
                    pendingProfileAsset = null
                    onDismissProfile()
                },
                onUpdateAsset = onUpdateAsset,
                onUnlink = { onUnlinkAsset(it.uid) },
                onLinkToParent = { child, parent -> onLinkAssets(child.uid, parent.uid) },
                onDeleteFile = onDeleteFile
            )
        } else {
            pendingProfileAsset = null
        }
    }

    // SVG Converter Dialog
    pendingSvgAsset?.let { asset ->
        SvgConverterDialog(
            asset = asset,
            onDismiss = {
                pendingSvgAsset = null
                onDismissSvg()
            },
            onSaveToAsset = { asset, svg ->
                // Save SVG to asset's files
                val svgFileName = "${asset.title.replace(" ", "_")}_${System.currentTimeMillis()}.svg"
                val svgFile = File(context.filesDir, svgFileName)
                FileOutputStream(svgFile).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        writer.write(svg)
                    }
                }
                // Update asset with SVG path (append to list)
                val updatedAsset = asset.copy(svgPaths = asset.svgPaths + svgFile.absolutePath)
                onUpdateAsset(updatedAsset)
            },
            onExportSvg = { title, svg ->
                // Export to Downloads
                try {
                    val svgFileName = "${title.replace(" ", "_")}_${System.currentTimeMillis()}.svg"
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, svgFileName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, "image/svg+xml")
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                        }
                    }
                    val uri = context.contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        context.contentResolver.openOutputStream(it)?.use { output ->
                            output.write(svg.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save SVG file")
                }
            }
        )
    }
}
