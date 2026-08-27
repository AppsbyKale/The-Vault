package com.example.thenobbery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.thenobbery.data.Asset
import com.example.thenobbery.data.VaultDatabase
import com.example.thenobbery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SingleFilesDialog(
    assets: List<Asset>,
    onDismiss: () -> Unit,
    onCreateAsset: (String, Set<String>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var orphanedFiles by remember { mutableStateOf<List<OrphanedFile>>(emptyList()) }
    var allFiles by remember { mutableStateOf<List<OrphanedFile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newAssetTitle by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAllFiles by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        orphanedFiles = findOrphanedFiles(context)
        allFiles = findAllFiles(context)
        isLoading = false
    }

    val displayFiles = if (showAllFiles) allFiles else orphanedFiles

    fun refreshFiles() {
        scope.launch {
            isLoading = true
            orphanedFiles = findOrphanedFiles(context)
            allFiles = findAllFiles(context)
            selectedFiles = emptySet()
            isLoading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .border(1.dp, VaultOutline, RoundedCornerShape(16.dp)),
            color = VaultBlack,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "SINGLE FILES",
                            color = VaultWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (showAllFiles) "${allFiles.size} total files" else "${orphanedFiles.size} orphaned",
                            color = VaultSilver,
                            fontSize = 11.sp
                        )
                    }
                    Row {
                        TextButton(onClick = { showAllFiles = !showAllFiles }) {
                            Text(
                                if (showAllFiles) "SHOW ORPHANED" else "SHOW ALL",
                                color = VaultSilver,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultWhite)
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = VaultWhite)
                    }
                } else if (displayFiles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (showAllFiles) "No files found" else "No orphaned files found",
                            color = VaultSilver,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Action bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (selectedFiles.size == displayFiles.size) {
                                        selectedFiles = emptySet()
                                    } else {
                                        selectedFiles = displayFiles.map { it.path }.toSet()
                                    }
                                }
                            ) {
                                Text(
                                    if (selectedFiles.size == displayFiles.size) "DESELECT ALL" else "SELECT ALL",
                                    color = VaultWhite,
                                    fontSize = 12.sp
                                )
                            }

                            if (selectedFiles.isNotEmpty()) {
                                TextButton(
                                    onClick = { showDeleteConfirm = true }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = AccentBack, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DELETE (${selectedFiles.size})", color = AccentBack, fontSize = 12.sp)
                                }
                            }
                        }

                        // Info text
                        Text(
                            "Files in storage not linked to any asset.",
                            color = VaultSilver,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        // File list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            state = androidx.compose.foundation.lazy.rememberLazyListState()
                        ) {
                            items(
                                count = displayFiles.size,
                                key = { index -> displayFiles[index].path }
                            ) { index ->
                                val file = displayFiles[index]
                                OrphanedFileItem(
                                    file = file,
                                    isSelected = file.path in selectedFiles,
                                    onToggle = {
                                        selectedFiles = if (file.path in selectedFiles) {
                                            selectedFiles - file.path
                                        } else {
                                            selectedFiles + file.path
                                        }
                                    }
                                )
                            }
                        }

                        // Create asset section
                        if (selectedFiles.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VaultSurface)
                                    .padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = newAssetTitle,
                                    onValueChange = { newAssetTitle = it },
                                    label = { Text("Asset Title", color = VaultSilver) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = VaultWhite),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VaultWhite,
                                        unfocusedBorderColor = VaultOutline
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (newAssetTitle.isNotBlank()) {
                                            onCreateAsset(newAssetTitle, selectedFiles)
                                            onDismiss()
                                        }
                                    },
                                    enabled = newAssetTitle.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = VaultWhite,
                                        contentColor = VaultBlack
                                    )
                                ) {
                                    Text("CREATE ASSET WITH ${selectedFiles.size} FILE(S)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = VaultBlack,
            modifier = Modifier.border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
            title = { Text("DELETE ${selectedFiles.size} FILE(S)?", color = VaultWhite, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete the file(s) from storage. This cannot be undone.", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch(Dispatchers.IO) {
                        selectedFiles.forEach { path ->
                            File(path).delete()
                        }
                        withContext(Dispatchers.Main) {
                            refreshFiles()
                            showDeleteConfirm = false
                        }
                    }
                }) {
                    Text("DELETE", color = AccentBack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }
}

@Composable
private fun OrphanedFileItem(
    file: OrphanedFile,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) VaultWhite else VaultOutline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onToggle() },
        color = if (isSelected) VaultSurface else VaultBlack,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = VaultWhite,
                    uncheckedColor = VaultSilver
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            AsyncImage(
                model = File(file.path),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = VaultWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Row {
                    Text(
                        "${file.sizeKB} KB",
                        color = VaultSilver,
                        fontSize = 11.sp
                    )
                    if (file.assetName != null) {
                        Text(
                            " • ${file.assetName}",
                            color = AccentMaster,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            " • ORPHANED",
                            color = AccentBack,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

data class OrphanedFile(
    val path: String,
    val name: String,
    val sizeKB: Long,
    val extension: String,
    val assetName: String? = null  // null means orphaned
)

private suspend fun findOrphanedFiles(context: android.content.Context): List<OrphanedFile> = withContext(Dispatchers.IO) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val allAssets = dao.getAllAssetsSync()
    
    val filesDir = context.filesDir
    val cacheDir = context.cacheDir
    val allFiles = (filesDir.listFiles()?.toList() ?: emptyList()) + 
                   (cacheDir.listFiles()?.toList() ?: emptyList())

    // Collect all paths that are linked to any asset (from ALL assets in DB)
    val pathToAsset = mutableMapOf<String, String>()
    allAssets.forEach { asset ->
        asset.pntrPaths.forEach { pathToAsset[it] = asset.title }
        asset.svgPaths.forEach { pathToAsset[it] = asset.title }
        asset.transparentPngPaths.forEach { pathToAsset[it] = asset.title }
        asset.backgroundPngPaths.forEach { pathToAsset[it] = asset.title }
        asset.mainPreviewPath?.let { pathToAsset[it] = asset.title }
    }

    val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "pntr")
    val excludePrefixes = setOf("data_", "export_", "backup_", "Nobbery_", "temp_", "import_")

    // Filter to only include image-like files that are NOT linked
    allFiles
        .filter { file ->
            val ext = file.extension.lowercase()
            val name = file.name.lowercase()
            ext in imageExtensions && 
            file.absolutePath !in pathToAsset.keys &&
            !excludePrefixes.any { name.startsWith(it) }
        }
        .map { file ->
            OrphanedFile(
                path = file.absolutePath,
                name = file.name,
                sizeKB = file.length() / 1024,
                extension = file.extension,
                assetName = null
            )
        }
        .sortedByDescending { it.sizeKB }
}

private suspend fun findAllFiles(context: android.content.Context): List<OrphanedFile> = withContext(Dispatchers.IO) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val allAssets = dao.getAllAssetsSync()
    
    val filesDir = context.filesDir
    val cacheDir = context.cacheDir
    val allFiles = (filesDir.listFiles()?.toList() ?: emptyList()) + 
                   (cacheDir.listFiles()?.toList() ?: emptyList())

    // Map paths to asset titles
    val pathToAsset = mutableMapOf<String, String>()
    allAssets.forEach { asset ->
        asset.pntrPaths.forEach { pathToAsset[it] = asset.title }
        asset.svgPaths.forEach { pathToAsset[it] = asset.title }
        asset.transparentPngPaths.forEach { pathToAsset[it] = asset.title }
        asset.backgroundPngPaths.forEach { pathToAsset[it] = asset.title }
        asset.mainPreviewPath?.let { pathToAsset[it] = asset.title }
    }

    val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "pntr")
    val excludePrefixes = setOf("data_", "export_", "backup_", "Nobbery_", "temp_", "import_")

    allFiles
        .filter { file ->
            val ext = file.extension.lowercase()
            val name = file.name.lowercase()
            ext in imageExtensions && !excludePrefixes.any { name.startsWith(it) }
        }
        .map { file ->
            OrphanedFile(
                path = file.absolutePath,
                name = file.name,
                sizeKB = file.length() / 1024,
                extension = file.extension,
                assetName = pathToAsset[file.absolutePath]
            )
        }
        .sortedByDescending { it.sizeKB }
}
