package com.example.thenobbery.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
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
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvgConverterDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onSaveToAsset: (Asset, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var saveToAsset by remember { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var svgContent by remember { mutableStateOf<String?>(null) }
    var selectedFileType by remember { mutableStateOf("transparent") }
    var isConverting by remember { mutableStateOf(false) }
    var isFilled by remember { mutableStateOf(true) }
    var useBezier by remember { mutableStateOf(true) }
    var autoInvert by remember { mutableStateOf(true) }
    var manualInvert by remember { mutableStateOf(false) }
    var blurRadius by remember { mutableStateOf(1) }
    var bezierTolerance by remember { mutableStateOf(3f) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var conversionProgress by remember { mutableStateOf(0f) }
    var detectedBackground by remember { mutableStateOf<String?>(null) }
    var keepColors by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = conversionProgress,
        animationSpec = tween(durationMillis = 300)
    )

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
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    LinearProgressIndicator(
                                        progress = animatedProgress,
                                        modifier = Modifier
                                            .fillMaxWidth(0.7f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = AccentSuccess,
                                        trackColor = VaultOutline
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}%",
                                        color = VaultSilver,
                                        fontSize = 14.sp
                                    )
                                }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SMOOTH CURVES", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = useBezier,
                            onCheckedChange = { useBezier = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VaultWhite,
                                checkedTrackColor = VaultOutline
                            )
                        )
                    }

                    if (useBezier) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SMOOTHNESS", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${bezierTolerance.toInt()}", color = VaultWhite, fontSize = 12.sp)
                            Slider(
                                value = bezierTolerance,
                                onValueChange = { bezierTolerance = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                modifier = Modifier.width(120.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = VaultWhite,
                                    activeTrackColor = VaultOutline
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BLUR", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${blurRadius}px", color = VaultWhite, fontSize = 12.sp)
                        Slider(
                            value = blurRadius.toFloat(),
                            onValueChange = { blurRadius = it.toInt() },
                            valueRange = 0f..5f,
                            steps = 4,
                            modifier = Modifier.width(120.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = VaultWhite,
                                activeTrackColor = VaultOutline
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AUTO INVERT", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = autoInvert,
                            onCheckedChange = { autoInvert = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VaultWhite,
                                checkedTrackColor = VaultOutline
                            )
                        )
                    }

                    if (autoInvert && detectedBackground != null) {
                        Text(
                            "Detected: $detectedBackground background",
                            color = AccentSuccess,
                            fontSize = 11.sp
                        )
                    }

                    if (!autoInvert) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("INVERT COLORS", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = manualInvert,
                                onCheckedChange = { manualInvert = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VaultWhite,
                                    checkedTrackColor = VaultOutline
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("KEEP COLORS", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = keepColors,
                            onCheckedChange = { keepColors = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VaultWhite,
                                checkedTrackColor = VaultOutline
                            )
                        )
                    }

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

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isConverting = true
                                conversionProgress = 0f
                                scale = 1f
                                offset = androidx.compose.ui.geometry.Offset.Zero
                                val result = convertToSvg(
                                    asset, selectedFileType, isFilled, useBezier,
                                    autoInvert, manualInvert, blurRadius, bezierTolerance,
                                    keepColors
                                ) { progress ->
                                    conversionProgress = progress.coerceIn(0f, 1f)
                                }
                                svgContent = result.first
                                previewBitmap = result.second
                                detectedBackground = result.third
                                isConverting = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isConverting,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                    ) {
                        Icon(Icons.Default.Refresh, null, tint = VaultWhite)
                        Spacer(Modifier.width(8.dp))
                        Text("CONVERT TO SVG", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

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
    asset: Asset,
    selectedFileType: String,
    isFilled: Boolean,
    useBezier: Boolean,
    autoInvert: Boolean,
    manualInvert: Boolean,
    blurRadius: Int,
    bezierTolerance: Float,
    keepColors: Boolean,
    onProgress: (Float) -> Unit
): Triple<String?, Bitmap?, String?> = withContext(Dispatchers.IO) {
    try {
        val filePath = when (selectedFileType) {
            "background" -> asset.primaryBackgroundPngPath
            "transparent" -> asset.primaryTransparentPngPath
            else -> asset.mainPreviewPath
        } ?: return@withContext Triple(null, null, null)

        val file = File(filePath)
        if (!file.exists()) return@withContext Triple(null, null, null)

        var bitmap = BitmapFactory.decodeFile(filePath) ?: return@withContext Triple(null, null, null)
        onProgress(0.05f)

        val maxDim = maxOf(bitmap.width, bitmap.height)
        if (maxDim > 1200) {
            val scale = 1200f / maxDim
            val scaled = Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            if (scaled !== bitmap) {
                bitmap.recycle()
                bitmap = scaled
            }
        }

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val luminance = FloatArray(w * h)
        for (i in pixels.indices) {
            val r = (pixels[i] shr 16) and 0xff
            val g = (pixels[i] shr 8) and 0xff
            val b = pixels[i] and 0xff
            luminance[i] = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
        }
        onProgress(0.15f)

        val blurred = if (blurRadius > 0) {
            gaussianBlur(luminance, w, h, blurRadius)
        } else {
            luminance
        }
        onProgress(0.25f)

        val shouldInvert = if (autoInvert) {
            detectDarkBackground(blurred)
        } else {
            manualInvert
        }
        val bgType = if (shouldInvert) "dark" else "light"
        onProgress(0.30f)

        val threshold = otsuThreshold(blurred)
        onProgress(0.35f)

        val mask = BooleanArray(w * h)
        for (i in blurred.indices) {
            val pixelIsDark = blurred[i] < threshold / 255f
            mask[i] = if (shouldInvert) !pixelIsDark else pixelIsDark
        }
        onProgress(0.45f)

        val contours = extractContours(mask, w, h)
        onProgress(0.60f)

        val contourColors = if (keepColors) {
            contours.map { getContourColor(it, pixels, mask, w, h) }
        } else {
            emptyList()
        }
        val bgColor = if (keepColors) getBackgroundColor(pixels, mask, w, h) else "#000000"
        val contourWidths = if (!isFilled) {
            contours.map { getContourWidth(it, mask, w, h) }
        } else {
            emptyList()
        }
        onProgress(0.70f)

        val svg = if (useBezier) {
            buildSvgBezier(w, h, contours, isFilled, bezierTolerance, contourColors, contourWidths)
        } else {
            buildSvg(w, h, contours, isFilled, contourColors, contourWidths)
        }
        onProgress(0.85f)

        val preview = renderPreview(mask, w, h, contours, isFilled, contourColors, bgColor, contourWidths)
        onProgress(1.0f)

        Triple(svg, preview, bgType)
    } catch (e: Exception) {
        Timber.e(e, "Failed to convert image to SVG")
        Triple(null, null, null)
    }
}

private fun detectDarkBackground(luminance: FloatArray): Boolean {
    val sampleSize = min(1000, luminance.size)
    val step = max(1, luminance.size / sampleSize)
    var sum = 0f
    var count = 0
    var i = 0
    while (i < luminance.size) {
        sum += luminance[i]
        count++
        i += step
    }
    val avgLuminance = sum / count
    return avgLuminance < 0.5f
}

private fun getContourColor(
    contour: List<Pair<Float, Float>>,
    pixels: IntArray,
    mask: BooleanArray,
    w: Int,
    h: Int
): String {
    val radius = 3
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L

    for ((fx, fy) in contour) {
        val cx = fx.toInt()
        val cy = fy.toInt()
        for (dy in -radius..radius) {
            val py = cy + dy
            if (py < 0 || py >= h) continue
            for (dx in -radius..radius) {
                val px = cx + dx
                if (px < 0 || px >= w) continue
                val idx = py * w + px
                if (!mask[idx]) continue
                val c = pixels[idx]
                rSum += (c shr 16) and 0xff
                gSum += (c shr 8) and 0xff
                bSum += c and 0xff
                count++
            }
        }
    }

    if (count == 0L) return "#000000"
    val r = (rSum / count).coerceIn(0L, 255L).toInt()
    val g = (gSum / count).coerceIn(0L, 255L).toInt()
    val b = (bSum / count).coerceIn(0L, 255L).toInt()
    return String.format("#%02x%02x%02x", r, g, b)
}

private fun getBackgroundColor(pixels: IntArray, mask: BooleanArray, w: Int, h: Int): String {
    val sampleStep = max(1, pixels.size / 4000)
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L

    var i = 0
    while (i < pixels.size) {
        if (!mask[i]) {
            val c = pixels[i]
            rSum += (c shr 16) and 0xff
            gSum += (c shr 8) and 0xff
            bSum += c and 0xff
            count++
        }
        i += sampleStep
    }

    if (count == 0L) return "#000000"
    val r = (rSum / count).coerceIn(0L, 255L).toInt()
    val g = (gSum / count).coerceIn(0L, 255L).toInt()
    val b = (bSum / count).coerceIn(0L, 255L).toInt()
    return String.format("#%02x%02x%02x", r, g, b)
}

private fun getContourWidth(
    contour: List<Pair<Float, Float>>,
    mask: BooleanArray,
    w: Int,
    h: Int
): Float {
    if (contour.isEmpty()) return 2f

    var minX = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var minY = Int.MAX_VALUE
    var maxY = Int.MIN_VALUE
    for ((fx, fy) in contour) {
        minX = min(minX, fx.toInt())
        maxX = max(maxX, fx.toInt())
        minY = min(minY, fy.toInt())
        maxY = max(maxY, fy.toInt())
    }
    val cx = (minX + maxX) / 2f
    val cy = (minY + maxY) / 2f

    val samples = 16
    val step = max(1, contour.size / samples)
    var widthSum = 0f
    var widthCount = 0

    var i = 0
    while (i < contour.size) {
        val px = contour[i].first
        val py = contour[i].second

        val dx = cx - px
        val dy = cy - py
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-4f) {
            i += step
            continue
        }
        val nx = dx / len
        val ny = dy / len

        var inwardCount = 1
        var tx = px + nx
        var ty = py + ny
        var guard = 0
        while (tx >= 0f && tx < w.toFloat() && ty >= 0f && ty < h.toFloat() && ++guard < 100) {
            val ix = tx.toInt()
            val iy = ty.toInt()
            if (!mask[iy * w + ix]) break
            inwardCount++
            tx += nx
            ty += ny
        }

        val estimatedWidth = inwardCount * 2f
        widthSum += estimatedWidth
        widthCount++
        i += step
    }

    return if (widthCount == 0) {
        2f
    } else {
        (widthSum / widthCount).coerceIn(1f, 40f)
    }
}

private fun otsuThreshold(luminance: FloatArray): Int {
    val histogram = IntArray(256)
    for (l in luminance) {
        val bin = (l * 255).toInt().coerceIn(0, 255)
        histogram[bin]++
    }

    val total = luminance.size
    var sum = 0f
    for (i in 0..255) {
        sum += i * histogram[i]
    }

    var sumB = 0f
    var wB = 0
    var wF: Int
    var maxVariance = 0f
    var threshold = 0

    for (t in 0..255) {
        wB += histogram[t]
        if (wB == 0) continue

        wF = total - wB
        if (wF == 0) break

        sumB += t * histogram[t]

        val mB = sumB / wB
        val mF = (sum - sumB) / wF

        val variance = wB.toFloat() * wF * (mB - mF) * (mB - mF)
        if (variance > maxVariance) {
            maxVariance = variance
            threshold = t
        }
    }

    return threshold
}

private fun gaussianBlur(data: FloatArray, w: Int, h: Int, radius: Int): FloatArray {
    val result = FloatArray(w * h)
    val kernelSize = radius * 2 + 1
    val kernel = FloatArray(kernelSize)
    var kernelSum = 0f

    for (i in 0 until kernelSize) {
        val x = i - radius
        kernel[i] = exp(-x.toFloat() * x / (2 * radius * radius))
        kernelSum += kernel[i]
    }
    for (i in kernel.indices) {
        kernel[i] /= kernelSum
    }

    val temp = FloatArray(w * h)
    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0f
            for (k in -radius..radius) {
                val sx = (x + k).coerceIn(0, w - 1)
                sum += data[y * w + sx] * kernel[k + radius]
            }
            temp[y * w + x] = sum
        }
    }

    for (y in 0 until h) {
        for (x in 0 until w) {
            var sum = 0f
            for (k in -radius..radius) {
                val sy = (y + k).coerceIn(0, h - 1)
                sum += temp[sy * w + x] * kernel[k + radius]
            }
            result[y * w + x] = sum
        }
    }

    return result
}

private fun extractContours(mask: BooleanArray, w: Int, h: Int): List<List<Pair<Float, Float>>> {
    fun idx(x: Int, y: Int) = y * w + x
    fun solid(x: Int, y: Int) = x in 0 until w && y in 0 until h && mask[idx(x, y)]

    val dirDx = intArrayOf(1, 0, -1, 0)
    val dirDy = intArrayOf(0, 1, 0, -1)

    val rfDx = intArrayOf(0, -1, -1, 0)
    val rfDy = intArrayOf(0, 0, -1, -1)
    val lfDx = intArrayOf(0, 0, -1, -1)
    val lfDy = intArrayOf(-1, 0, 0, -1)

    val solidOfsDx = intArrayOf(0, -1, -1, 0)
    val solidOfsDy = intArrayOf(0, 0, -1, -1)

    val visited = BooleanArray(w * h)
    val contours = mutableListOf<List<Pair<Float, Float>>>()

    for (startY in 0 until h) {
        for (startX in 0 until w) {
            if (!solid(startX, startY) || visited[idx(startX, startY)]) continue
            val isBoundaryCell =
                !solid(startX - 1, startY) || !solid(startX + 1, startY) ||
                !solid(startX, startY - 1) || !solid(startX, startY + 1)
            if (!isBoundaryCell) continue

            var vx = startX
            var vy = startY
            var heading = 0

            val contour = mutableListOf<Pair<Float, Float>>()
            contour.add(Pair(vx.toFloat(), vy.toFloat()))
            visited[idx(startX, startY)] = true

            var guard = 0
            val maxGuard = w * h * 4 + 16
            var started = false
            while (true) {
                if (++guard > maxGuard) break
                if (started && vx == startX && vy == startY) break
                started = true

                val rightF = solid(vx + rfDx[heading], vy + rfDy[heading])
                val leftF = solid(vx + lfDx[heading], vy + lfDy[heading])

                val nextHeading: Int
                if (!rightF) {
                    nextHeading = (heading + 1) % 4
                } else if (leftF) {
                    nextHeading = (heading + 3) % 4
                } else {
                    nextHeading = heading
                }

                val cx = vx + solidOfsDx[nextHeading]
                val cy = vy + solidOfsDy[nextHeading]
                if (cx in 0 until w && cy in 0 until h) {
                    visited[idx(cx, cy)] = true
                }

                vx += dirDx[nextHeading]
                vy += dirDy[nextHeading]
                heading = nextHeading

                if (!(vx == startX && vy == startY)) {
                    contour.add(Pair(vx.toFloat(), vy.toFloat()))
                }
            }

            if (contour.size > 3) {
                contours.add(contour)
            }
        }
    }
    return contours
}

private fun douglasPeucker(points: List<Pair<Float, Float>>, epsilon: Float): List<Pair<Float, Float>> {
    if (points.size <= 2) return points

    var maxDist = 0f
    var maxIdx = 0
    val start = points.first()
    val end = points.last()

    for (i in 1 until points.size - 1) {
        val dist = perpendicularDistance(points[i], start, end)
        if (dist > maxDist) {
            maxDist = dist
            maxIdx = i
        }
    }

    return if (maxDist > epsilon) {
        val left = douglasPeucker(points.subList(0, maxIdx + 1), epsilon)
        val right = douglasPeucker(points.subList(maxIdx, points.size), epsilon)
        left.dropLast(1) + right
    } else {
        listOf(start, end)
    }
}

private fun perpendicularDistance(
    point: Pair<Float, Float>,
    lineStart: Pair<Float, Float>,
    lineEnd: Pair<Float, Float>
): Float {
    val dx = lineEnd.first - lineStart.first
    val dy = lineEnd.second - lineStart.second
    val length = sqrt(dx * dx + dy * dy)
    if (length == 0f) return sqrt(
        (point.first - lineStart.first).pow(2) + (point.second - lineStart.second).pow(2)
    )
    val area = abs((lineEnd.first - lineStart.first) * (lineStart.second - point.second) -
            (lineStart.first - point.first) * (lineEnd.second - lineStart.second))
    return area / length
}

data class CubicBezier(
    val p0: Pair<Float, Float>,
    val p1: Pair<Float, Float>,
    val p2: Pair<Float, Float>,
    val p3: Pair<Float, Float>
)

private fun fitBezierCurves(
    points: List<Pair<Float, Float>>,
    maxError: Float
): List<CubicBezier> {
    if (points.size < 2) return emptyList()

    val result = mutableListOf<CubicBezier>()
    fitBezierSegment(points, 0, points.size - 1, maxError, result)
    return result
}

private fun fitBezierSegment(
    points: List<Pair<Float, Float>>,
    start: Int,
    end: Int,
    maxError: Float,
    result: MutableList<CubicBezier>
) {
    if (end - start < 2) {
        result.add(CubicBezier(points[start], points[start], points[end], points[end]))
        return
    }

    val p0 = points[start]
    val p3 = points[end]
    val dx = p3.first - p0.first
    val dy = p3.second - p0.second
    val chordLen = sqrt(dx * dx + dy * dy)

    if (chordLen < 1e-6f) {
        result.add(CubicBezier(p0, p0, p3, p3))
        return
    }

    val maxParamDist = maxError * maxError * 4f
    var splitPoint = (start + end) / 2

    var maxDist = 0f
    for (i in start + 1 until end) {
        val t = (i - start).toFloat() / (end - start)
        val px = p0.first + t * dx
        val py = p0.second + t * dy
        val dist = (points[i].first - px).pow(2) + (points[i].second - py).pow(2)
        if (dist > maxDist) {
            maxDist = dist
            splitPoint = i
        }
    }

    if (maxDist <= maxParamDist) {
        val cp1 = computeControlPoint(points, start, end, p0, p3, true)
        val cp2 = computeControlPoint(points, start, end, p0, p3, false)
        result.add(CubicBezier(p0, cp1, cp2, p3))
        return
    }

    fitBezierSegment(points, start, splitPoint, maxError, result)
    fitBezierSegment(points, splitPoint, end, maxError, result)
}

private fun computeControlPoint(
    points: List<Pair<Float, Float>>,
    start: Int,
    end: Int,
    p0: Pair<Float, Float>,
    p3: Pair<Float, Float>,
    isFirst: Boolean
): Pair<Float, Float> {
    val n = end - start
    if (n < 1) return p0

    val dx = p3.first - p0.first
    val dy = p3.second - p0.second
    val chordLen = sqrt(dx * dx + dy * dy)

    val nx = if (chordLen > 1e-6f) -dy / chordLen else 0f
    val ny = if (chordLen > 1e-6f) dx / chordLen else 0f

    var sumT = 0f
    var sumT2 = 0f
    var sumTx = 0f
    var sumTy = 0f

    for (i in start..end) {
        val t = (i - start).toFloat() / n
        val pt = points[i]
        val lx = pt.first - p0.first - t * dx
        val ly = pt.second - p0.second - t * dy
        val dot = lx * nx + ly * ny

        sumT += t
        sumT2 += t * t
        sumTx += t * dot
        sumTy += dot
    }

    val det = n.toFloat() * sumT2 - sumT * sumT
    if (abs(det) < 1e-6f) {
        return if (isFirst) {
            Pair(p0.first + dx / 3f, p0.second + dy / 3f)
        } else {
            Pair(p3.first - dx / 3f, p3.second - dy / 3f)
        }
    }

    val alpha = (n.toFloat() * sumTx - sumT * sumTy) / det
    val scale = alpha / 3f

    return if (isFirst) {
        Pair(p0.first + dx / 3f + nx * scale, p0.second + dy / 3f + ny * scale)
    } else {
        Pair(p3.first - dx / 3f + nx * scale, p3.second - dy / 3f + ny * scale)
    }
}

private fun buildSvgBezier(
    w: Int,
    h: Int,
    contours: List<List<Pair<Float, Float>>>,
    isFilled: Boolean,
    bezierTolerance: Float,
    contourColors: List<String>,
    contourWidths: List<Float>
): String {
    val defaultFill = if (isFilled) "black" else "none"
    val defaultStroke = if (isFilled) "none" else "black"
    val sw = if (isFilled) "" else " stroke-width=\"2\""

    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\">")

        for ((ci, contour) in contours.withIndex()) {
            if (contour.size < 4) continue

            val simplified = douglasPeucker(contour, bezierTolerance)
            if (simplified.size < 2) continue

            val curves = fitBezierCurves(simplified, bezierTolerance)
            if (curves.isEmpty()) continue

            val hasColor = contourColors.size > ci
            val fillAttr = if (hasColor) contourColors[ci] else defaultFill
            val strokeAttr = if (hasColor) contourColors[ci] else defaultStroke
            val strokeWidth = if (!isFilled && contourWidths.size > ci) {
                " stroke-width=\"${contourWidths[ci]}\""
            } else {
                sw
            }

            append("<path d=\"M${simplified[0].first} ${simplified[0].second}")

            for (curve in curves) {
                append(" C${curve.p1.first} ${curve.p1.second}, ${curve.p2.first} ${curve.p2.second}, ${curve.p3.first} ${curve.p3.second}")
            }

            appendLine("Z\" fill=\"$fillAttr\" stroke=\"$strokeAttr\"$strokeWidth/>")
        }
        appendLine("</svg>")
    }
}

private fun buildSvg(
    w: Int,
    h: Int,
    contours: List<List<Pair<Float, Float>>>,
    isFilled: Boolean,
    contourColors: List<String>,
    contourWidths: List<Float>
): String {
    val defaultFill = if (isFilled) "black" else "none"
    val defaultStroke = if (isFilled) "none" else "black"
    return buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        appendLine("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\">")
        for ((ci, contour) in contours.withIndex()) {
            if (contour.isEmpty()) continue
            val hasColor = contourColors.size > ci
            val fillAttr = if (hasColor) contourColors[ci] else defaultFill
            val strokeAttr = if (hasColor) contourColors[ci] else defaultStroke
            val strokeWidth = if (!isFilled && contourWidths.size > ci) {
                " stroke-width=\"${contourWidths[ci]}\""
            } else {
                " stroke-width=\"2\""
            }
            append("<path d=\"M${contour[0].first} ${contour[0].second}")
            for (i in 1 until contour.size) {
                append(" L${contour[i].first} ${contour[i].second}")
            }
            appendLine("Z\" fill=\"$fillAttr\" stroke=\"$strokeAttr\"$strokeWidth/>")
        }
        appendLine("</svg>")
    }
}

private fun renderPreview(
    mask: BooleanArray,
    w: Int,
    h: Int,
    contours: List<List<Pair<Float, Float>>>,
    isFilled: Boolean,
    contourColors: List<String> = emptyList(),
    bgColor: String = "#000000",
    contourWidths: List<Float> = emptyList()
): Bitmap {
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val bgArgb = android.graphics.Color.parseColor(bgColor)
    if (isFilled) {
        if (contourColors.isEmpty()) {
            val pixels = IntArray(w * h)
            for (i in pixels.indices) {
                pixels[i] = if (mask[i]) 0xffffffff.toInt() else 0xff000000.toInt()
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        } else {
            val canvas = Canvas(bitmap)
            canvas.drawColor(bgArgb)
            val paint = Paint().apply {
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            for ((ci, contour) in contours.withIndex()) {
                if (ci >= contourColors.size || contour.size < 2) continue
                paint.color = android.graphics.Color.parseColor(contourColors[ci])
                val path = Path()
                path.fillType = android.graphics.Path.FillType.EVEN_ODD
                path.moveTo(contour[0].first, contour[0].second)
                for (i in 1 until contour.size) {
                    path.lineTo(contour[i].first, contour[i].second)
                }
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    } else {
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgArgb)
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        for ((ci, contour) in contours.withIndex()) {
            if (contour.size < 2) continue
            paint.color = if (ci < contourColors.size) {
                android.graphics.Color.parseColor(contourColors[ci])
            } else {
                0xffffffff.toInt()
            }
            paint.strokeWidth = if (ci < contourWidths.size) contourWidths[ci] else 2f
            val path = Path()
            path.moveTo(contour[0].first, contour[0].second)
            for (i in 1 until contour.size) {
                path.lineTo(contour[i].first, contour[i].second)
            }
            path.close()
            canvas.drawPath(path, paint)
        }
    }
    return bitmap
}
