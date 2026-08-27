package com.example.thenobbery.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.WatermarkPreferenceManager
import com.example.thenobbery.ui.theme.*
import java.io.File
import kotlin.math.roundToInt

@Composable
fun WatermarkStudioDialog(
    asset: Asset,
    onDismiss: () -> Unit,
    onFinalize: (Float, Float, IntOffset, IntSize, IntSize) -> Unit,
    selectedFileType: String = "background"
) {
    val context = LocalContext.current
    val wmManager = remember { WatermarkPreferenceManager(context) }
    val watermarkPath = wmManager.getWatermarkPath()

    var opacity by remember { mutableStateOf(wmManager.lastOpacity) }
    var scale by remember { mutableStateOf(wmManager.lastScale) }
    var offset by remember { mutableStateOf(IntOffset(0, 0)) }
    
    var containerSize by remember { mutableStateOf(IntSize(0, 0)) }
    var imageSize by remember { mutableStateOf(IntSize(0, 0)) }

    val previewPath = when (selectedFileType) {
        "transparent" -> asset.primaryTransparentPngPath ?: asset.primaryBackgroundPngPath ?: asset.mainPreviewPath
        "pntr" -> asset.primaryPntrPath ?: asset.primaryBackgroundPngPath ?: asset.mainPreviewPath
        else -> asset.primaryBackgroundPngPath ?: asset.mainPreviewPath
    }
    
    LaunchedEffect(previewPath) {
        previewPath?.let { path ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, options)
            imageSize = IntSize(options.outWidth, options.outHeight)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .border(1.dp, VaultSurface, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = VaultBlack
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = VaultSilver)
                    }
                    Text("WATERMARK STUDIO", color = VaultWhite, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    IconButton(onClick = { 
                        wmManager.lastOpacity = opacity
                        wmManager.lastScale = scale
                        onFinalize(opacity, scale, offset, containerSize, imageSize) 
                    }) {
                        Icon(Icons.Default.Check, null, tint = AccentMaster)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(VaultSurface, RoundedCornerShape(16.dp))
                        .border(0.5.dp, VaultOutline, RoundedCornerShape(16.dp))
                        .onGloballyPositioned { coordinates ->
                            containerSize = IntSize(coordinates.size.width, coordinates.size.height)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = previewPath ?: asset.primaryBackgroundPngPath ?: asset.mainPreviewPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    watermarkPath?.let { path ->
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(scale)
                                .offset { offset }
                                .alpha(opacity)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offset = IntOffset(
                                            (offset.x + dragAmount.x).roundToInt(),
                                            (offset.y + dragAmount.y).roundToInt()
                                        )
                                    }
                                }
                        )
                    } ?: Text("No Watermark Uploaded", color = VaultSilver, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                StudioSlider("OPACITY", opacity) { opacity = it }
                Spacer(modifier = Modifier.height(16.dp))
                StudioSlider("SCALE", scale) { scale = it }
            }
        }
    }
}

@Composable
fun StudioSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", color = VaultWhite, fontSize = 10.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = VaultWhite,
                activeTrackColor = VaultSilver,
                inactiveTrackColor = VaultSurface
            )
        )
    }
}
