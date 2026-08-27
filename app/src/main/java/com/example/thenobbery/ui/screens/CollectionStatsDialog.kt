package com.example.thenobbery.ui.screens

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thenobbery.data.VaultDatabase
import com.example.thenobbery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@Composable
fun CollectionStatsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isLoading by remember { mutableStateOf(true) }
    var stats by remember { mutableStateOf<CollectionStats?>(null) }
    
    fun loadStats() {
        isLoading = true
        scope.launch {
            stats = loadCollectionStats(context)
            isLoading = false
        }
    }
    
    LaunchedEffect(Unit) {
        loadStats()
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.6f)
                .border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
            color = VaultBlack,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "COLLECTION INFO",
                        color = VaultWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { loadStats() }, enabled = !isLoading) {
                            Icon(
                                Icons.Default.Refresh, null,
                                tint = if (isLoading) VaultSilver.copy(alpha = 0.5f) else VaultSilver
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = VaultSilver)
                        }
                    }
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading...", color = VaultSilver, fontSize = 14.sp)
                    }
                } else if (stats != null) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            StatsHeader(stats = stats!!)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                exportChecklistAsTxt(context)
                            }
                        },
                        enabled = !isLoading && (stats?.totalAssets ?: 0) > 0,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VaultWhite
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(VaultOutline)
                        )
                    ) {
                        Icon(Icons.Default.Description, null, tint = VaultWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TXT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                exportChecklistAsCsv(context)
                            }
                        },
                        enabled = !isLoading && (stats?.totalAssets ?: 0) > 0,
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = VaultWhite
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(VaultOutline)
                        )
                    ) {
                        Icon(Icons.Default.TableChart, null, tint = VaultWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsHeader(stats: CollectionStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultSurface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatBox(
                icon = Icons.Default.Folder,
                value = stats.totalAssets.toString(),
                label = "ASSETS"
            )
            StatBox(
                icon = Icons.Default.InsertDriveFile,
                value = stats.totalFiles.toString(),
                label = "FILES"
            )
            StatBox(
                icon = Icons.Default.Category,
                value = stats.topCategories.size.toString(),
                label = "CATEGORIES"
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "TOP CATEGORIES",
            color = VaultSilver,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (stats.topCategories.isEmpty()) {
            Text("No categories set", color = VaultSilver.copy(alpha = 0.5f), fontSize = 11.sp)
        } else {
            stats.topCategories.forEachIndexed { index, (category, count) ->
                Text(
                    "${index + 1}. $category ($count)",
                    color = VaultWhite,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "COLLECTIONS",
            color = VaultSilver,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (stats.allCollections.isEmpty()) {
            Text("No collections set", color = VaultSilver.copy(alpha = 0.5f), fontSize = 11.sp)
        } else {
            stats.allCollections.sorted().forEachIndexed { index, collection ->
                Text(
                    "${index + 1}. $collection",
                    color = VaultWhite,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "TAGS",
            color = VaultSilver,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (stats.allTags.isEmpty()) {
            Text("No tags set", color = VaultSilver.copy(alpha = 0.5f), fontSize = 11.sp)
        } else {
            stats.allTags.sorted().forEachIndexed { index, tag ->
                Text(
                    "${index + 1}. $tag",
                    color = VaultSilver,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon, null,
            tint = VaultSilver,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            color = VaultWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            color = VaultSilver,
            fontSize = 9.sp
        )
    }
}

data class CollectionStats(
    val totalAssets: Int,
    val totalFiles: Int,
    val topCategories: List<Pair<String, Int>>,
    val allCollections: Set<String>,
    val allTags: Set<String>
)

private suspend fun loadCollectionStats(context: Context): CollectionStats = withContext(Dispatchers.IO) {
    Timber.d("Loading collection stats...")
    
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val assets = dao.getAllAssetsSync()
    
    val totalAssets = assets.size
    
    val totalFiles = assets.sumOf { asset ->
        asset.pntrPaths.size + asset.svgPaths.size + asset.transparentPngPaths.size + asset.backgroundPngPaths.size
    }
    
    val categoryCounts = assets
        .filter { it.classification != "UNASSIGNED" }
        .groupBy { it.classification }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(3)
    
    val allCollections = assets
        .flatMap { it.collections.split(",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
    
    val allTags = assets
        .flatMap { it.tags.split(",") }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
    
    Timber.d("Stats: $totalAssets assets, $totalFiles files")
    Timber.d("Categories: $categoryCounts")
    Timber.d("Collections: $allCollections")
    Timber.d("Tags: $allTags")
    
    CollectionStats(
        totalAssets = totalAssets,
        totalFiles = totalFiles,
        topCategories = categoryCounts,
        allCollections = allCollections,
        allTags = allTags
    )
}

private suspend fun exportChecklistAsTxt(context: Context) = withContext(Dispatchers.IO) {
    try {
        val dao = VaultDatabase.getDatabase(context).assetDao()
        val assets = dao.getAllAssetsSync().sortedBy { it.title.lowercase() }
        
        val content = buildString {
            appendLine("COLLECTION CHECKLIST")
            appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}")
            appendLine("Total: ${assets.size} assets")
            appendLine()
            appendLine("=" .repeat(50))
            appendLine()
            
            assets.forEachIndexed { index, asset ->
                appendLine("${index + 1}. ${asset.title}")
                if (asset.classification != "UNASSIGNED") {
                    appendLine("   Category: ${asset.classification}")
                }
                if (asset.collections.isNotBlank()) {
                    appendLine("   Collection: ${asset.collections}")
                }
                if (asset.tags.isNotBlank()) {
                    appendLine("   Tags: ${asset.tags}")
                }
            }
        }
        
        val exportDir = File(Environment.getExternalStorageDirectory(), "Documents/The Nobbery Collection Lists")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, "collection_checklist_${System.currentTimeMillis()}.txt")
        file.writeText(content)
        Timber.d("Exported checklist to ${file.absolutePath}")
    } catch (e: Exception) {
        Timber.e("Error exporting TXT: ${e.message}")
    }
}

private suspend fun exportChecklistAsCsv(context: Context) = withContext(Dispatchers.IO) {
    try {
        val dao = VaultDatabase.getDatabase(context).assetDao()
        val assets = dao.getAllAssetsSync().sortedBy { it.title.lowercase() }
        
        val content = buildString {
            appendLine("Title,Category,Collection,Tags,Has PNG,Has Transparent,Has IP")
            
            assets.forEach { asset ->
                val title = asset.title.replace(",", ";")
                val category = if (asset.classification == "UNASSIGNED") "" else asset.classification.replace(",", ";")
                val collection = asset.collections.replace(",", ";")
                val tags = asset.tags.replace(",", ";")
                val hasPng = if (asset.hasBackgroundPng()) "Yes" else "No"
                val hasTransparent = if (asset.hasTransparentPng()) "Yes" else "No"
                val hasIp = if (asset.hasPntr()) "Yes" else "No"
                
                appendLine("\"$title\",\"$category\",\"$collection\",\"$tags\",\"$hasPng\",\"$hasTransparent\",\"$hasIp\"")
            }
        }
        
        val exportDir = File(Environment.getExternalStorageDirectory(), "Documents/The Nobbery Collection Lists")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, "collection_checklist_${System.currentTimeMillis()}.csv")
        file.writeText(content)
        Timber.d("Exported checklist to ${file.absolutePath}")
    } catch (e: Exception) {
        Timber.e("Error exporting CSV: ${e.message}")
    }
}
