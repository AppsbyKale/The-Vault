package com.example.thenobbery.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.theme.*

@Composable
fun ExportDispatcher(
    asset: Asset,
    isBulkExport: Boolean = false,
    bulkAssetCount: Int = 1,
    bulkAssets: List<Asset> = emptyList(),
    onExportToDevice: (Asset, List<String>, Boolean) -> Unit,
    onShareSheet: (Asset, List<String>, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onLaunchWatermarkStudio: (Asset, List<Asset>, List<String>, String) -> Unit = { _, _, _, _ -> }
) {
    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var useWatermark by remember { mutableStateOf(false) }

    val hasSelection = selectedTypes.isNotEmpty()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .border(1.dp, VaultSilver.copy(alpha = 0.2f), RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            color = VaultBlack
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isBulkExport) "EXPORT ${bulkAssetCount} ASSETS" else "EXPORT ASSET",
                    color = VaultWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                
                if (!isBulkExport) {
                    Text(
                        text = asset.title.uppercase(),
                        color = VaultSilver,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text(
                    text = "SELECT FILES",
                    color = VaultSilver,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, VaultOutline, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    if (asset.hasBackgroundPng()) {
                        FileTypeCheckbox(
                            label = "Background PNG (${asset.backgroundPngPaths.size})",
                            checked = selectedTypes.contains("background"),
                            onCheckedChange = {
                                selectedTypes = if (selectedTypes.contains("background")) {
                                    selectedTypes - "background"
                                } else {
                                    selectedTypes + "background"
                                }
                            }
                        )
                    }
                    if (asset.hasTransparentPng()) {
                        FileTypeCheckbox(
                            label = "Transparent PNG (${asset.transparentPngPaths.size})",
                            checked = selectedTypes.contains("transparent"),
                            onCheckedChange = {
                                selectedTypes = if (selectedTypes.contains("transparent")) {
                                    selectedTypes - "transparent"
                                } else {
                                    selectedTypes + "transparent"
                                }
                            }
                        )
                    }
                    if (asset.hasPntr()) {
                        FileTypeCheckbox(
                            label = "Infinite Painter (.pntr) (${asset.pntrPaths.size})",
                            checked = selectedTypes.contains("pntr"),
                            onCheckedChange = {
                                selectedTypes = if (selectedTypes.contains("pntr")) {
                                    selectedTypes - "pntr"
                                } else {
                                    selectedTypes + "pntr"
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (useWatermark) VaultSurface else VaultSurface.copy(alpha = 0.5f)
                ) {
                    Text(
                        if (useWatermark) "⚠ Watermark will be applied to selected files"
                        else "Enable watermark to apply it to selected files",
                        color = if (useWatermark) AccentMaster else VaultSilver,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Apply Watermark", color = VaultWhite, fontSize = 14.sp)
                        Text("PNG files only", color = VaultSilver, fontSize = 10.sp)
                    }
                    Switch(
                        checked = useWatermark,
                        onCheckedChange = { useWatermark = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VaultWhite,
                            checkedTrackColor = VaultSilver
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        ActionButton(
                            text = "SAVE",
                            icon = Icons.Default.Download,
                            modifier = Modifier.weight(1f),
                            enabled = hasSelection
                        ) {
                            val types = selectedTypes.toList()
                            if (useWatermark) {
                                onLaunchWatermarkStudio(asset, bulkAssets, types, "device")
                            } else {
                                onExportToDevice(asset, types, false)
                            }
                        }

                        ActionButton(
                            text = "SHARE",
                            icon = Icons.Default.Share,
                            modifier = Modifier.weight(1f),
                            enabled = hasSelection
                        ) {
                            val types = selectedTypes.toList()
                            if (useWatermark) {
                                onLaunchWatermarkStudio(asset, bulkAssets, types, "share")
                            } else {
                                onShareSheet(asset, types, false)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun FileTypeCheckbox(
    label: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange() }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (checked) VaultWhite else Color.Transparent,
                    RoundedCornerShape(4.dp)
                )
                .border(1.dp, if (checked) VaultWhite else VaultSilver, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = VaultBlack,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = VaultWhite,
            fontSize = 13.sp
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = VaultWhite,
            contentColor = VaultBlack,
            disabledContainerColor = VaultSurface,
            disabledContentColor = VaultSilver
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}
