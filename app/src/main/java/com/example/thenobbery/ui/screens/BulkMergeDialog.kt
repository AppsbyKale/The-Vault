package com.example.thenobbery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun BulkMergeDialog(
    assetsToMerge: List<Asset>,
    allAssets: List<Asset>,
    onDismiss: () -> Unit,
    onMergeComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("select") } // "select", "new", "existing"
    var selectedAssets by remember { mutableStateOf(assetsToMerge.toSet()) }
    var newTitle by remember { mutableStateOf("") }
    var existingTarget by remember { mutableStateOf<Asset?>(null) }

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
                    Text(
                        "MERGE ASSETS",
                        color = VaultWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultWhite)
                    }
                }

                when (mode) {
                    "select" -> {
                        // Selection mode
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Select assets to merge (${selectedAssets.size} selected)",
                                    color = VaultSilver,
                                    fontSize = 14.sp
                                )
                                TextButton(onClick = {
                                    if (selectedAssets.size == assetsToMerge.size) {
                                        selectedAssets = emptySet()
                                    } else {
                                        selectedAssets = assetsToMerge.toSet()
                                    }
                                }) {
                                    Text(
                                        if (selectedAssets.size == assetsToMerge.size) "DESELECT ALL" else "SELECT ALL",
                                        color = VaultWhite,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(assetsToMerge) { asset ->
                                    MergeAssetItem(
                                        asset = asset,
                                        isSelected = asset in selectedAssets,
                                        onToggle = {
                                            selectedAssets = if (asset in selectedAssets) {
                                                selectedAssets - asset
                                            } else {
                                                selectedAssets + asset
                                            }
                                        }
                                    )
                                }
                            }

                            if (selectedAssets.size >= 2) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(VaultSurface)
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { mode = "new" },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VaultWhite,
                                                contentColor = VaultBlack
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("NEW ASSET")
                                        }
                                        Button(
                                            onClick = { mode = "existing" },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = VaultSurface,
                                                contentColor = VaultWhite
                                            ),
                                            border = BorderStroke(1.dp, VaultWhite)
                                        ) {
                                            Text("PICK EXISTING")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "new" -> {
                        // Create new asset mode
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Create new asset from selected files:",
                                color = VaultSilver,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )

                            // Show selected assets with X to remove
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(selectedAssets.toList()) { asset ->
                                    MergeAssetItem(
                                        asset = asset,
                                        isSelected = true,
                                        onToggle = { selectedAssets = selectedAssets - asset }
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VaultSurface)
                                    .padding(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = newTitle,
                                    onValueChange = { newTitle = it },
                                    label = { Text("New Asset Title", color = VaultSilver) },
                                    textStyle = androidx.compose.ui.text.TextStyle(color = VaultWhite),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = VaultWhite,
                                        unfocusedBorderColor = VaultOutline
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { mode = "select" },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VaultSurface,
                                            contentColor = VaultWhite
                                        )
                                    ) {
                                        Text("BACK")
                                    }
                                    Button(
                                        onClick = {
                                            if (newTitle.isNotBlank() && selectedAssets.isNotEmpty()) {
                                                scope.launch(Dispatchers.IO) {
                                                    mergeAssetsToNew(context, selectedAssets.toList(), newTitle)
                                                    withContext(Dispatchers.Main) {
                                                        onMergeComplete()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = newTitle.isNotBlank(),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VaultWhite,
                                            contentColor = VaultBlack
                                        )
                                    ) {
                                        Text("MERGE")
                                    }
                                }
                            }
                        }
                    }

                    "existing" -> {
                        // Pick existing asset mode
                        Column(modifier = Modifier.fillMaxSize()) {
                            Text(
                                "Select target asset to merge into:",
                                color = VaultSilver,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(allAssets) { asset ->
                                    MergeAssetItem(
                                        asset = asset,
                                        isSelected = asset == existingTarget,
                                        onToggle = { existingTarget = asset }
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VaultSurface)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { mode = "select" },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VaultSurface,
                                            contentColor = VaultWhite
                                        )
                                    ) {
                                        Text("BACK")
                                    }
                                    Button(
                                        onClick = {
                                            existingTarget?.let { target ->
                                                scope.launch(Dispatchers.IO) {
                                                    mergeAssets(context, selectedAssets.toList(), target)
                                                    withContext(Dispatchers.Main) {
                                                        onMergeComplete()
                                                    }
                                                }
                                            }
                                        },
                                        enabled = existingTarget != null,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VaultWhite,
                                            contentColor = VaultBlack
                                        )
                                    ) {
                                        Text("MERGE INTO ${existingTarget?.title ?: "?"}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeAssetItem(
    asset: Asset,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val previewPath = asset.mainPreviewPath ?: asset.primaryBackgroundPngPath ?: asset.primaryTransparentPngPath

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
                model = previewPath?.let { File(it) },
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    asset.title,
                    color = VaultWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${asset.pntrPaths.size + asset.svgPaths.size + asset.transparentPngPaths.size + asset.backgroundPngPaths.size} files",
                    color = VaultSilver,
                    fontSize = 11.sp
                )
            }
            if (!isSelected) {
                // Show X for remove in edit modes
            }
        }
    }
}

private suspend fun mergeAssetsToNew(context: android.content.Context, assets: List<Asset>, newTitle: String) = withContext(Dispatchers.IO) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    
    var newAsset = Asset(title = newTitle)
    
    // Collect all files from all assets
    assets.forEach { asset ->
        newAsset = newAsset.copy(
            pntrPaths = newAsset.pntrPaths + asset.pntrPaths,
            svgPaths = newAsset.svgPaths + asset.svgPaths,
            transparentPngPaths = newAsset.transparentPngPaths + asset.transparentPngPaths,
            backgroundPngPaths = newAsset.backgroundPngPaths + asset.backgroundPngPaths
        )
    }
    
    // Set main preview
    newAsset = newAsset.copy(
        mainPreviewPath = newAsset.primaryTransparentPngPath ?: newAsset.primaryBackgroundPngPath
    )
    
    dao.insertAsset(newAsset)
    
    // Delete the source assets
    assets.forEach { dao.deleteAsset(it) }
}

private suspend fun mergeAssets(context: android.content.Context, sourceAssets: List<Asset>, targetAsset: Asset) = withContext(Dispatchers.IO) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    
    // Merge all files into target
    var mergedAsset = targetAsset
    sourceAssets.forEach { asset ->
        mergedAsset = mergedAsset.copy(
            pntrPaths = (mergedAsset.pntrPaths + asset.pntrPaths).distinct(),
            svgPaths = (mergedAsset.svgPaths + asset.svgPaths).distinct(),
            transparentPngPaths = (mergedAsset.transparentPngPaths + asset.transparentPngPaths).distinct(),
            backgroundPngPaths = (mergedAsset.backgroundPngPaths + asset.backgroundPngPaths).distinct()
        )
    }
    
    // Update main preview if needed
    mergedAsset = mergedAsset.copy(
        mainPreviewPath = mergedAsset.mainPreviewPath ?: mergedAsset.primaryTransparentPngPath ?: mergedAsset.primaryBackgroundPngPath
    )
    
    dao.updateAsset(mergedAsset)
    
    // Delete the source assets
    sourceAssets.forEach { dao.deleteAsset(it) }
}
