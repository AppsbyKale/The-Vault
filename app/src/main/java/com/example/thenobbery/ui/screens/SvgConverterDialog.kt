package com.example.thenobbery.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvgConverterDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSaveToAsset: (Asset, String) -> Unit,
    onExportSvg: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var saveToAsset by remember { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var svgContent by remember { mutableStateOf<String?>(null) }
    var selectedFileType by remember { mutableStateOf("transparent") }
    var isConverting by remember { mutableStateOf(false) }
    var isFilled by remember { mutableStateOf(true) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/svg+xml")
    ) { uri ->
        uri?.let {
            svgContent?.let { svg ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { output ->
                        output.write(svg.toByteArray())
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to export SVG")
                }
            }
        }
    }

    val availableFiles = remember(asset) {
        val files = mutableListOf<Pair<String, String>>()
        asset.backgroundPngPaths.forEach { files.add("background" to it) }
        asset.transparentPngPaths.forEach { files.add("transparent" to it) }
        asset.mainPreviewPath?.let { files.add("preview" to it) }
        files.filter { File(it.second).exists() }
    }

    LaunchedEffect(asset) {
        if (availableFiles.isNotEmpty()) {
            selectedFileType = availableFiles.first().first
            // Load original image immediately
            val filePath = availableFiles.first().second
            previewBitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(filePath)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
            color = VaultBlack,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CONVERT TO SVG",
                        color = VaultWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = VaultSilver)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Preview Box - Always visible
                    Text("PREVIEW", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .border(1.dp, VaultOutline),
                        color = VaultSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isConverting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = VaultWhite,
                                    strokeWidth = 3.dp
                                )
                            } else if (previewBitmap != null) {
                                Image(
                                    bitmap = previewBitmap!!.asImageBitmap(),
                                    contentDescription = "Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                                offset += pan
                                            }
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    // Fill toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FILL SHAPES", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = isFilled,
                            onCheckedChange = { isFilled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VaultWhite,
                                checkedTrackColor = VaultOutline
                            )
                        )
                    }

                    // Source selector
                    if (availableFiles.isNotEmpty()) {
                        Text("SOURCE IMAGE", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableFiles.forEach { (type, _) ->
                                FilterChip(
                                    selected = selectedFileType == type,
                                    onClick = { selectedFileType = type },
                                    label = { Text(type.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = VaultSurface,
                                        selectedLabelColor = VaultWhite
                                    )
                                )
                            }
                        }
                    }

                    // Convert button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isConverting = true
                                scale = 1f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                                val result = convertToSvg(context, asset, selectedFileType, isFilled, 0.5f, 0.5f)
                                svgContent = result.first
                                previewBitmap = result.second
                                isConverting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = VaultWhite)
                        Spacer(Modifier.width(8.dp))
                        Text("CONVERT TO SVG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    }

                    // Footer
                    Row(
                        modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Checkbox(
                            checked = saveToAsset,
                            onCheckedChange = { saveToAsset = it },
                            colors = CheckboxDefaults.colors(checkedColor = VaultWhite)
                        )
                        Text("Save to Asset", color = VaultWhite, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            svgContent?.let { svg ->
                                if (saveToAsset) {
                                    onSaveToAsset(asset, svg)
                                }
                                // Export using file picker
                                exportLauncher.launch("${asset.title.replace(" ", "_")}.svg")
                            }
                        },
                        enabled = svgContent != null && !isConverting,
                        colors = ButtonDefaults.buttonColors(containerColor = VaultWhite, contentColor = VaultBlack)
                    ) {
                        Icon(Icons.Default.Download, null, tint = VaultBlack, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("EXPORT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private suspend fun convertToSvg(
    context: Context,
    asset: Asset,
    selectedFileType: String,
    isFilled: Boolean,
    threshold: Float,
    detailLevel: Float
): Pair<String, Bitmap?> = withContext(Dispatchers.IO) {
    try {
        val filePath = when (selectedFileType) {
            "background" -> asset.primaryBackgroundPngPath
            "transparent" -> asset.primaryTransparentPngPath
            else -> asset.mainPreviewPath
        } ?: return@withContext Pair("", null)

        val file = File(filePath)
        if (!file.exists()) return@withContext Pair("", null)

        val bitmap = BitmapFactory.decodeFile(filePath) ?: return@withContext Pair("", null)

        val width = bitmap.width
        val height = bitmap.height

        // Use marching squares algorithm
        val svgPathData = traceOutline(bitmap)
        
        // SVG for 3D printing - filled or stroked
        val fillAttr = if (isFilled) "black" else "none"
        val strokeAttr = if (isFilled) "none" else "black"
        val svg = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">")
            appendLine("<path d=\"$svgPathData\" fill=\"$fillAttr\" stroke=\"$strokeAttr\" stroke-width=\"2\"/>")
            appendLine("</svg>")
        }

        // Preview shows filled or stroked on black
        val previewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        if (isFilled) {
            // For filled preview, show solid white shapes
            val contours = findContours(bitmap)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var inside = false
                    for (contour in contours) {
                        if (pointInPolygon(x, y, contour)) {
                            inside = !inside
                        }
                    }
                    previewBitmap.setPixel(x, y, if (inside) 0xffffffff.toInt() else 0xff000000.toInt())
                }
            }
        } else {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = (pixel shr 24) and 0xff
                    previewBitmap.setPixel(x, y, if (alpha >= 128) 0xffffffff.toInt() else 0xff000000.toInt())
                }
            }
        }
        
        bitmap.recycle()

        Pair(svg, previewBitmap)
    } catch (e: Exception) {
        Timber.e(e, "Failed to convert image to SVG")
        Pair("", null)
    }
}

private fun traceOutline(bitmap: Bitmap): String {
    val w = bitmap.width
    val h = bitmap.height
    val path = StringBuilder()
    
    fun isSolid(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= w || y >= h) return false
        return (bitmap.getPixel(x, y) shr 24 and 0xff) > 120
    }
    
    for (y in 0 until h - 1) {
        for (x in 0 until w - 1) {
            val config = (if (isSolid(x, y)) 8 else 0) or 
                       (if (isSolid(x + 1, y)) 4 else 0) or 
                       (if (isSolid(x + 1, y + 1)) 2 else 0) or 
                       (if (isSolid(x, y + 1)) 1 else 0)
            
            when (config) {
                1, 14 -> path.append("M$x ${y+0.5} L${x+0.5} ${y+1} ")
                2, 13 -> path.append("M${x+0.5} ${y+1} L${x+1} ${y+0.5} ")
                3, 12 -> path.append("M$x ${y+0.5} L${x+1} ${y+0.5} ")
                4, 11 -> path.append("M${x+0.5} $y L${x+1} ${y+0.5} ")
                6, 9  -> path.append("M${x+0.5} $y L${x+0.5} ${y+1} ")
                7, 8  -> path.append("M$x ${y+0.5} L${x+0.5} $y ")
            }
        }
    }
    
    return path.toString()
}

private fun findContours(bitmap: Bitmap): List<List<Pair<Float, Float>>> {
    val w = bitmap.width
    val h = bitmap.height
    val visited = Array(w) { BooleanArray(h) }
    val contours = mutableListOf<List<Pair<Float, Float>>>()
    
    fun isSolid(x: Int, y: Int): Boolean {
        if (x < 0 || y < 0 || x >= w || y >= h) return false
        return (bitmap.getPixel(x, y) shr 24 and 0xff) > 120
    }
    
    for (y in 0 until h) {
        for (x in 0 until w) {
            if (isSolid(x, y) && !visited[x][y]) {
                val contour = mutableListOf<Pair<Float, Float>>()
                var cx = x
                var cy = y
                val maxSteps = w * h
                var steps = 0
                
                do {
                    visited[cx][cy] = true
                    contour.add(Pair(cx.toFloat(), cy.toFloat()))
                    
                    val neighbors = listOf(
                        Pair(cx - 1, cy), Pair(cx + 1, cy),
                        Pair(cx, cy - 1), Pair(cx, cy + 1)
                    )
                    var found = false
                    for ((nx, ny) in neighbors) {
                        if (nx in 0 until w && ny in 0 until h && isSolid(nx, ny) && !visited[nx][ny]) {
                            cx = nx
                            cy = ny
                            found = true
                            break
                        }
                    }
                    if (!found) break
                    steps++
                } while ((cx != x || cy != y) && steps < maxSteps)
                
                if (contour.size > 10) {
                    contours.add(contour)
                }
            }
        }
    }
    
    return contours
}

private fun pointInPolygon(x: Int, y: Int, polygon: List<Pair<Float, Float>>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val (xi, yi) = polygon[i]
        val (xj, yj) = polygon[j]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
            inside = !inside
        }
        j = i
    }
    return inside
}
