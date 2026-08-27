package com.example.thenobbery.ui.screens

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.thenobbery.logic.FileClassifier
import com.example.thenobbery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class NewImportProject(
    val title: String,
    val pntrFile: File?,
    val transparentPng: File?,
    val backgroundPng: File?
)

@Composable
fun NewImportsDialog(
    onDismiss: () -> Unit,
    onApprove: (NewImportProject) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isScanning by remember { mutableStateOf(true) }
    var newProjects by remember { mutableStateOf<List<NewImportProject>>(emptyList()) }
    var previewProject by remember { mutableStateOf<NewImportProject?>(null) }
    var showDeniedList by remember { mutableStateOf(false) }
    
    fun rescan() {
        isScanning = true
        scope.launch {
            Timber.d("Starting scan...")
            newProjects = scanForNewImports(context)
            Timber.d("Scan complete, found ${newProjects.size} projects")
            isScanning = false
        }
    }
    
    if (showDeniedList) {
        DeniedListDialog(
            onDismiss = { showDeniedList = false },
            onRescan = { rescan() }
        )
    }
    
    if (previewProject != null) {
        NewImportFullScreenPreview(
            project = previewProject!!,
            onDismiss = { previewProject = null }
        )
    }
    
    fun handleApprove(project: NewImportProject) {
        onApprove(project)
        newProjects = newProjects.filter { it != project }
    }
    
    fun handleDeny(project: NewImportProject) {
        scope.launch {
            denyNewImport(context, project)
            newProjects = newProjects.filter { it != project }
        }
    }
    
    fun handleApproveAll() {
        scope.launch {
            newProjects.toList().forEach { project ->
                onApprove(project)
            }
            newProjects = emptyList()
        }
    }
    
    fun handleDenyAll() {
        scope.launch {
            newProjects.toList().forEach { project ->
                denyNewImport(context, project)
            }
            newProjects = emptyList()
        }
    }
    
    LaunchedEffect(Unit) {
        rescan()
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
                .fillMaxHeight(0.85f)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { handleDenyAll() },
                            enabled = newProjects.isNotEmpty() && !isScanning
                        ) {
                            Text("DENY ALL", color = if (newProjects.isNotEmpty()) Color(0xFFE57373) else VaultSilver.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = { handleApproveAll() },
                            enabled = newProjects.isNotEmpty() && !isScanning
                        ) {
                            Text("ACCEPT ALL", color = if (newProjects.isNotEmpty()) VaultWhite else VaultSilver.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                    Row {
                        IconButton(onClick = { showDeniedList = true }) {
                            Icon(Icons.Default.Block, null, tint = VaultSilver)
                        }
                        IconButton(onClick = { rescan() }, enabled = !isScanning) {
                            Icon(Icons.Default.Refresh, null, tint = if (isScanning) VaultSilver.copy(alpha = 0.5f) else VaultSilver)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = VaultSilver)
                        }
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    if (isScanning) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Scanning for new projects...", color = VaultSilver, fontSize = 14.sp)
                        }
                    } else if (newProjects.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image, null, 
                                    tint = VaultSilver.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No new imports found",
                                    color = VaultSilver,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(newProjects) { project ->
                                NewImportRow(
                                    project = project,
                                    onApprove = { handleApprove(project) },
                                    onDeny = { handleDeny(project) },
                                    onPreview = { previewProject = project }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportFileTypeIcon(letter: String, color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = color,
            fontSize = 8.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun NewImportRow(
    project: NewImportProject,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onPreview: () -> Unit
) {
    val thumbnailPath = project.transparentPng?.absolutePath 
        ?: project.backgroundPng?.absolutePath
    val hasPreview = project.transparentPng != null || project.backgroundPng != null
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(VaultBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VaultSurface)
                .then(
                    if (hasPreview) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(onTap = { onPreview() })
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnailPath != null) {
                AsyncImage(
                    model = File(thumbnailPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Image, null,
                    tint = VaultSilver,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                project.title.uppercase(),
                color = VaultWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (project.backgroundPng != null) ImportFileTypeIcon("B", AccentBack)
                if (project.transparentPng != null) ImportFileTypeIcon("T", VaultSilver)
                if (project.pntrFile != null) ImportFileTypeIcon("IP", MutedViolet)
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onDeny,
                modifier = Modifier
                    .size(36.dp)
                    .background(VaultSurface, RoundedCornerShape(4.dp))
                    .border(1.dp, VaultOutline, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    Icons.Default.Close, null,
                    tint = Color(0xFFE57373),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onApprove,
                modifier = Modifier
                    .size(36.dp)
                    .background(VaultSurface, RoundedCornerShape(4.dp))
                    .border(1.dp, VaultOutline, RoundedCornerShape(4.dp))
            ) {
                Icon(
                    Icons.Default.Check, null,
                    tint = VaultWhite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private suspend fun scanForNewImports(context: Context): List<NewImportProject> = withContext(Dispatchers.IO) {
    Timber.d("scanForNewImports started")
    val projects = mutableListOf<NewImportProject>()
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val allAssets = dao.getAllAssetsSync()
    val existingTitles = allAssets.filter { it.classification != "DENIED" }.map { it.title }.toSet()
    val deniedTitles = allAssets.filter { it.classification == "DENIED" }.map { it.title }.toSet()
    Timber.d("Existing titles: $existingTitles")
    Timber.d("Denied titles: $deniedTitles")
    
    val infinitePainterDir = File(Environment.getExternalStorageDirectory(), "Documents/Infinite Painter/Projects")
    val pngDir = File(Environment.getExternalStorageDirectory(), "Pictures/Infinite Painter")
    
    Timber.d("Checking dir: ${infinitePainterDir.absolutePath}, exists: ${infinitePainterDir.exists()}")
    Timber.d("Checking dir: ${pngDir.absolutePath}, exists: ${pngDir.exists()}")
    
    if (!infinitePainterDir.exists()) {
        Timber.d("infinitePainterDir does not exist")
        return@withContext emptyList()
    }
    
    val allFilesInPntrDir = infinitePainterDir.listFiles()?.map { it.name } ?: emptyList()
    Timber.d("All files in Projects dir: $allFilesInPntrDir")
    
    val pntrFiles = infinitePainterDir.listFiles()?.filter { it.extension == "pntr" } ?: run {
        Timber.d("No pntr files found")
        return@withContext emptyList()
    }
    Timber.d("Found ${pntrFiles.size} pntr files: ${pntrFiles.map { it.name }}")
    
    if (!pngDir.exists()) {
        Timber.d("pngDir does not exist")
    }
    
    val fiveMinutesMillis = 5 * 60 * 1000L
    
    val pngFiles = pngDir.listFiles()?.filter { it.extension == "png" } ?: emptyList()
    Timber.d("Found ${pngFiles.size} PNG files")
    
    for (pntr in pntrFiles) {
        val baseName = pntr.nameWithoutExtension.replace(Regex("\\(\\d+\\)|copy"), "").trim()
        Timber.d("Processing pntr: ${pntr.name}, baseName: $baseName")
        if (baseName.startsWith("300dpi", ignoreCase = true) || baseName.startsWith("Project", ignoreCase = true)) {
            Timber.d("Skipping $baseName - filtered out")
            continue
        }
        if (baseName in existingTitles) {
            Timber.d("Skipping $baseName - already exists")
            continue
        }
        if (baseName in deniedTitles) {
            Timber.d("Skipping $baseName - previously denied")
            continue
        }
        
        val pntrTime = pntr.lastModified()
        
        val matchingPngs = pngFiles.filter { file ->
            file.name.contains(baseName, ignoreCase = true) &&
            kotlin.math.abs(file.lastModified() - pntrTime) <= fiveMinutesMillis
        }
        
        var transparentPng: File? = null
        var backgroundPng: File? = null
        
        for (png in matchingPngs) {
            val slot = FileClassifier.identifyPngType(png)
            Timber.d("  Classified ${png.name} as: $slot")
            when (slot) {
                FileClassifier.AssetSlot.IMAGE_TRANSPARENT -> transparentPng = png
                FileClassifier.AssetSlot.IMAGE_BACKGROUND -> backgroundPng = png
                else -> {}
            }
        }
        
        Timber.d("  transparentPng: ${transparentPng?.name}")
        Timber.d("  backgroundPng: ${backgroundPng?.name}")
        
        projects.add(NewImportProject(
            title = baseName,
            pntrFile = pntr,
            transparentPng = transparentPng,
            backgroundPng = backgroundPng
        ))
    }
    
    projects.sortedBy { it.title }.also {
        Timber.d("Final results: ${it.map { p -> p.title }}")
    }
    return@withContext projects
}

@Composable
fun NewImportFullScreenPreview(
    project: NewImportProject,
    onDismiss: () -> Unit
) {
    var showBackground by remember { mutableStateOf(true) }
    
    val backgroundPath = project.backgroundPng?.absolutePath
    val transparentPath = project.transparentPng?.absolutePath
    val hasBoth = backgroundPath != null && transparentPath != null
    
    val currentPath = if (showBackground) backgroundPath else transparentPath
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            currentPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            
            if (hasBoth) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.clickable { showBackground = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showBackground) VaultWhite else VaultSurface
                    ) {
                        Text(
                            "BACKGROUND",
                            color = if (showBackground) VaultBlack else VaultSilver,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.clickable { showBackground = false },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!showBackground) VaultWhite else VaultSurface
                    ) {
                        Text(
                            "TRANSPARENT",
                            color = if (!showBackground) VaultBlack else VaultSilver,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

private suspend fun denyNewImport(context: Context, project: NewImportProject) {
    project.pntrFile?.delete()
    project.transparentPng?.delete()
    project.backgroundPng?.delete()
    
    val dao = VaultDatabase.getDatabase(context).assetDao()
    val deniedAsset = Asset(
        title = project.title,
        classification = "DENIED",
        timestamp = System.currentTimeMillis()
    )
    dao.insertAsset(deniedAsset)
}

@Composable
private fun DeniedListDialog(
    onDismiss: () -> Unit,
    onRescan: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var deniedAssets by remember { mutableStateOf<List<Asset>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val dao = VaultDatabase.getDatabase(context).assetDao()
        val allAssets = dao.getAllAssetsSync()
        deniedAssets = allAssets.filter { it.classification == "DENIED" }.sortedBy { it.title.lowercase() }
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
                .fillMaxWidth(0.85f)
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
                        "DENIED PROJECTS",
                        color = VaultWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = VaultSilver)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    if (deniedAssets.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No denied projects",
                                color = VaultSilver,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(deniedAssets) { asset ->
                                DeniedAssetRow(
                                    asset = asset,
                                    onRemove = {
                                        scope.launch {
                                            removeFromDeniedList(context, asset)
                                            deniedAssets = deniedAssets.filter { it.uid != asset.uid }
                                            onRescan()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeniedAssetRow(
    asset: Asset,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(VaultBlack)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = asset.title.uppercase(),
            color = VaultWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Delete, null,
                tint = Color(0xFFE57373),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private suspend fun removeFromDeniedList(context: Context, asset: Asset) {
    val dao = VaultDatabase.getDatabase(context).assetDao()
    dao.deleteAsset(asset)
}