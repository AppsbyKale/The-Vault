package com.example.thenobbery.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.MainActivity
import com.example.thenobbery.data.Asset
import com.example.thenobbery.data.VaultDatabase
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.logic.BackupUtils
import com.example.thenobbery.ui.theme.*
import com.example.thenobbery.viewmodels.AssetFilter
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LibraryScreen(
    assets: List<Asset>,
    searchQuery: String,
    sortOrder: AssetDefinitions.SortOrder,
    filter: AssetFilter,
    allCategories: List<String>,
    allCollections: List<String>,
    allTags: List<String>,
    isImporting: Boolean,
    importProgressParam: Float,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (AssetDefinitions.SortOrder) -> Unit,
    onFilterChanged: (AssetFilter) -> Unit,
    onBulkImportRequested: () -> Unit,
    onUpdateAsset: (Asset) -> Unit,
    onDeleteAsset: (Asset) -> Unit,
    onUnlinkAsset: (Long) -> Unit,
    onLinkAssets: (Long, Long) -> Unit,
    onMergeAssets: (Asset, Asset) -> Unit,
    onBatchUpdateAssets: (List<Asset>, EditMode, Set<String>, Set<String>, EditMode, Set<String>, Set<String>, EditMode, String?) -> Unit,
    onDeleteFile: (Asset, com.example.thenobbery.ui.screens.profiledialog.FileType) -> Unit,
    onRefreshOrganization: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as MainActivity
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var isMultiselectMode by remember { mutableStateOf(false) }
    var selectedAssets by remember { mutableStateOf(setOf<Asset>()) }

    var activeActionAsset by remember { mutableStateOf<Asset?>(null) }
    var showProfileIndex by remember { mutableStateOf<Int?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Asset?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    
    var linkingInitiator by remember { mutableStateOf<Asset?>(null) }
    var mergingInitiator by remember { mutableStateOf<Asset?>(null) }
    var bulkMergeAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var exportingAsset by remember { mutableStateOf<Asset?>(null) }
    var studioAsset by remember { mutableStateOf<Asset?>(null) }
    var bulkAssetsForExport by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var bulkIsShare by remember { mutableStateOf(false) }
    var showNewImportsDialog by remember { mutableStateOf(false) }
    var showCollectionStatsDialog by remember { mutableStateOf(false) }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSingleFilesDialog by remember { mutableStateOf(false) }
    var batchEditAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    var showOrganizationManager by remember { mutableStateOf(false) }

    var isBackupInProgress by remember { mutableStateOf(false) }
    var backupProgressText by remember { mutableStateOf("") }
    var backupProgress by remember { mutableStateOf(0f) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US)
    val dateSuffix = dateFormat.format(Date())

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    isBackupInProgress = true
                    backupProgressText = "Starting backup..."
                    backupProgress = 0f
                    BackupUtils.exportCollectionToUri(context, it) { text, progress ->
                        backupProgressText = text
                        backupProgress = progress
                    }
                } catch (e: Exception) {
                    backupProgressText = "Error: ${e.message}"
                    Timber.e(e, "Failed to export backup to selected location")
                } finally {
                    isBackupInProgress = false
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    isBackupInProgress = true
                    backupProgressText = "Starting restore..."
                    backupProgress = 0f
                    BackupUtils.importCollection(context, it) { text, progress ->
                        backupProgressText = text
                        backupProgress = progress
                    }
                } catch (e: Exception) {
                    backupProgressText = "Error: ${e.message}"
                    Timber.e(e, "Failed to restore backup")
                } finally {
                    isBackupInProgress = false
                }
            }
        }
    }

    fun onExportBackup() {
        scope.launch {
            isBackupInProgress = true
            backupProgressText = "Starting backup..."
            backupProgress = 0f
            val result = BackupUtils.exportCollection(context) { text, progress ->
                backupProgressText = text
                backupProgress = progress
            }
            result.onFailure { e ->
                backupProgressText = "Error: ${e.message}"
                Timber.e(e, "Failed to export backup to Downloads")
            }
            isBackupInProgress = false
        }
    }

    fun onExportBackupToLocation() {
        exportBackupLauncher.launch("Nobbery_Backup_$dateSuffix.zip")
    }

    fun onImportBackup() {
        importBackupLauncher.launch(arrayOf("application/zip"))
    }

    fun clearMultiselect() {
        selectedAssets = emptySet()
        isMultiselectMode = false
    }

    Scaffold(
        topBar = {
            LibraryTopBar(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                onSortOrderChanged = onSortOrderChanged,
                onFilterChanged = onFilterChanged,
                currentFilter = filter,
                currentSortOrder = sortOrder,
                allCategories = allCategories,
                allCollections = allCollections,
                allTags = allTags,
                totalCount = assets.size,
                filteredCount = assets.size,
                isMultiselectMode = isMultiselectMode,
                selectedCount = selectedAssets.size,
                onMultiselectToggle = {
                    isMultiselectMode = !isMultiselectMode
                    if (!isMultiselectMode) {
                        selectedAssets = emptySet()
                    }
                },
                onBulkAction = { action ->
                    when (action) {
                        BulkAction.DELETE -> showBulkDeleteConfirm = true
                        BulkAction.EXPORT_SHARE -> {
                            if (selectedAssets.isNotEmpty()) {
                                bulkAssetsForExport = selectedAssets.toList()
                            }
                        }
                        BulkAction.VIEW_PROFILE -> {
                            if (selectedAssets.isNotEmpty()) {
                                val index = assets.indexOf(selectedAssets.first())
                                if (index >= 0) showProfileIndex = index
                                clearMultiselect()
                            }
                        }
                        BulkAction.LINK -> {
                            if (selectedAssets.isNotEmpty()) {
                                linkingInitiator = selectedAssets.first()
                            }
                        }
                        BulkAction.MERGE -> {
                            if (selectedAssets.size >= 2) {
                                bulkMergeAssets = selectedAssets.toList()
                            }
                        }
                        BulkAction.BATCH_EDIT -> {
                            if (selectedAssets.isNotEmpty()) {
                                batchEditAssets = selectedAssets.toList()
                                showBatchEditDialog = true
                            }
                        }
                    }
                },
                onNewImportsClick = { showNewImportsDialog = true },
                onCollectionStatsClick = { showCollectionStatsDialog = true },
                onExportBackup = { onExportBackup() },
                onExportBackupToLocation = { onExportBackupToLocation() },
                onImportBackup = { onImportBackup() },
                onShowBackupDialog = { showBackupDialog = true },
                onShowSingleFilesClick = { showSingleFilesDialog = true },
                onShowOrganizationManager = { showOrganizationManager = true }
            )
        },
        floatingActionButton = {
            if (!isMultiselectMode) {
                FloatingActionButton(
                    onClick = onBulkImportRequested,
                    containerColor = VaultWhite,
                    contentColor = VaultBlack,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import")
                }
            }
        },
        containerColor = VaultBlack
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isImporting) {
                LinearProgressIndicator(
                    progress = importProgressParam,
                    modifier = Modifier.fillMaxWidth(),
                    color = VaultWhite,
                    trackColor = VaultSurface
                )
            }

            if (isBackupInProgress) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(16.dp)
                ) {
                    Text(
                        backupProgressText,
                        color = VaultWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = backupProgress,
                        modifier = Modifier.fillMaxWidth(),
                        color = VaultWhite,
                        trackColor = VaultBlack
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(assets) { index, asset ->
                    AssetRow(
                        asset = asset,
                        allAssets = assets,
                        onClick = { showProfileIndex = index },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            activeActionAsset = asset
                        },
                        isMultiselectMode = isMultiselectMode,
                        isSelected = selectedAssets.contains(asset),
                        onSelect = {
                            selectedAssets = if (selectedAssets.contains(asset)) {
                                selectedAssets - asset
                            } else {
                                selectedAssets + asset
                            }
                        }
                    )
                    Divider(color = VaultOutline, thickness = 0.5.dp)
                }
            }
        }
    }

    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            containerColor = VaultBlack,
            modifier = Modifier.border(1.dp, VaultOutline, RoundedCornerShape(16.dp)),
            title = { Text("DELETE ${selectedAssets.size} ASSETS?", color = VaultWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone.", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    selectedAssets.forEach { onDeleteAsset(it) }
                    selectedAssets = emptySet()
                    isMultiselectMode = false
                    showBulkDeleteConfirm = false
                }) {
                    Text("DELETE", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }

    LaunchedEffect(bulkAssetsForExport) {
        if (bulkAssetsForExport.isNotEmpty()) {
            exportingAsset = bulkAssetsForExport.first()
        }
    }

    LibraryDialogDispatcher(
        activity = activity,
        activeActionAsset = activeActionAsset,
        showProfileIndex = showProfileIndex,
        showDeleteConfirm = showDeleteConfirm,
        mergingInitiator = mergingInitiator,
        linkingInitiator = linkingInitiator,
        exportingAsset = exportingAsset,
        studioAsset = studioAsset,
        assets = assets,
        onDismissActionMenu = { activeActionAsset = null },
        onDismissProfile = { 
            showProfileIndex = null
            if (isMultiselectMode) {
                selectedAssets = emptySet()
                isMultiselectMode = false
            }
        },
        onDismissDelete = { showDeleteConfirm = null },
        onDismissMerge = { mergingInitiator = null },
        onDismissLink = { linkingInitiator = null },
        onDismissExport = { 
            exportingAsset = null
            bulkAssetsForExport = emptyList()
            if (isMultiselectMode) {
                selectedAssets = emptySet()
                isMultiselectMode = false
            }
        },
        onDismissStudio = { studioAsset = null },
        onUpdateAsset = onUpdateAsset,
        onDeleteAsset = onDeleteAsset,
        onUnlinkAsset = onUnlinkAsset,
        onLinkRequested = { asset ->
            activeActionAsset = null
            linkingInitiator = asset 
        },
        onMergeRequested = { asset ->
            activeActionAsset = null
            mergingInitiator = asset 
        },
        onExportRequested = { asset ->
            activeActionAsset = null
            exportingAsset = asset      
        },
        onLaunchStudio = { asset ->
            activeActionAsset = null
            studioAsset = asset 
        },
        onLinkAssets = onLinkAssets,
        onMergeAssets = onMergeAssets,
        onDeleteFile = onDeleteFile,
        bulkExportAssets = bulkAssetsForExport
    )

    if (showNewImportsDialog) {
        NewImportsDialog(
            onDismiss = { showNewImportsDialog = false },
            onApprove = { project ->
                scope.launch {
                    processNewImport(context, project)
                }
            }
        )
    }
    
    if (showCollectionStatsDialog) {
        CollectionStatsDialog(
            onDismiss = { showCollectionStatsDialog = false }
        )
    }

    if (showBatchEditDialog) {
        BatchEditDialog(
            assets = batchEditAssets,
            allAssets = assets,
            categories = allCategories,
            collections = allCollections,
            tags = allTags,
            onDismiss = {
                showBatchEditDialog = false
                clearMultiselect()
            },
            onApply = { tagsMode, tagsToAdd, tagsToRemove, collectionsMode, collectionsToAdd, collectionsToRemove, categoryMode, category ->
                onBatchUpdateAssets(
                    batchEditAssets,
                    tagsMode, tagsToAdd, tagsToRemove,
                    collectionsMode, collectionsToAdd, collectionsToRemove,
                    categoryMode, category
                )
                showBatchEditDialog = false
                clearMultiselect()
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            onExportToDownloads = { onExportBackup() },
            onExportToLocation = { onExportBackupToLocation() },
            onImport = { onImportBackup() }
        )
    }

    if (showOrganizationManager) {
        OrganizationManagerDialog(
            onDismiss = { showOrganizationManager = false },
            onDataChanged = onRefreshOrganization
        )
    }

    val localContext = LocalContext.current

    if (showSingleFilesDialog) {
        SingleFilesDialog(
            assets = assets,
            onDismiss = { showSingleFilesDialog = false },
            onCreateAsset = { title, filePaths ->
                // Create new asset from single file(s)
                CoroutineScope(Dispatchers.IO).launch {
                    createAssetFromOrphanedFiles(context = localContext, title, filePaths)
                }
            }
        )
    }

    if (bulkMergeAssets.isNotEmpty()) {
        BulkMergeDialog(
            assetsToMerge = bulkMergeAssets,
            allAssets = assets,
            onDismiss = { bulkMergeAssets = emptyList() },
            onMergeComplete = {
                bulkMergeAssets = emptyList()
                selectedAssets = emptySet()
                isMultiselectMode = false
            }
        )
    }
}

private suspend fun processNewImport(context: Context, project: NewImportProject) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val appFilesDir = context.filesDir
    
    var asset = Asset(title = project.title)
    
    project.pntrFile?.let { pntr ->
        val uniqueName = makeUniqueFileName(appFilesDir, pntr.name)
        val destFile = File(appFilesDir, uniqueName)
        pntr.copyTo(destFile, overwrite = true)
        asset = asset.copy(pntrPaths = asset.pntrPaths + destFile.absolutePath)
    }
    
    project.transparentPng?.let { png ->
        val uniqueName = makeUniqueFileName(appFilesDir, png.name)
        val destFile = File(appFilesDir, uniqueName)
        png.copyTo(destFile, overwrite = true)
        asset = asset.copy(transparentPngPaths = asset.transparentPngPaths + destFile.absolutePath)
    }
    
    project.backgroundPng?.let { png ->
        val uniqueName = makeUniqueFileName(appFilesDir, png.name)
        val destFile = File(appFilesDir, uniqueName)
        png.copyTo(destFile, overwrite = true)
        asset = asset.copy(backgroundPngPaths = asset.backgroundPngPaths + destFile.absolutePath)
    }
    
    asset = asset.copy(mainPreviewPath = asset.primaryTransparentPngPath ?: asset.primaryBackgroundPngPath)
    
    dao.insertAsset(asset)
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

private suspend fun createAssetFromOrphanedFiles(context: Context, title: String, filePaths: Set<String>) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val appFilesDir = context.filesDir

    var asset = Asset(title = title)

    filePaths.forEach { filePath ->
        val file = File(filePath)
        if (file.exists()) {
            // Move/copy file to ensure unique name
            val uniqueName = makeUniqueFileName(appFilesDir, file.name)
            val destFile = File(appFilesDir, uniqueName)
            file.copyTo(destFile, overwrite = true)

            val ext = file.extension.lowercase()
            asset = when (ext) {
                "pntr" -> asset.copy(pntrPaths = asset.pntrPaths + destFile.absolutePath)
                "svg" -> asset.copy(svgPaths = asset.svgPaths + destFile.absolutePath)
                "png", "jpg", "jpeg", "gif", "webp", "bmp" -> {
                    // Check if it has transparency by checking filename or default to transparent
                    if (file.name.contains("transparent", ignoreCase = true) ||
                        file.name.contains("t_", ignoreCase = true)) {
                        asset.copy(transparentPngPaths = asset.transparentPngPaths + destFile.absolutePath)
                    } else {
                        asset.copy(backgroundPngPaths = asset.backgroundPngPaths + destFile.absolutePath)
                    }
                }
                else -> asset
            }
        }
    }

    // Set main preview
    asset = asset.copy(mainPreviewPath = asset.primaryTransparentPngPath ?: asset.primaryBackgroundPngPath)

    dao.insertAsset(asset)
}
