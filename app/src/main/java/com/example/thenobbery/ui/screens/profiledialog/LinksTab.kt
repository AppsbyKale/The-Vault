package com.example.thenobbery.ui.screens.profiledialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.components.PickerAssetRow
import com.example.thenobbery.ui.screens.LinkPickerDialog
import com.example.thenobbery.ui.theme.*

/**
 * The Lineage management tab within the Asset Profile.
 * Shows current parent/children and allows linking/unlinking.
 */
@Composable
fun LinksTab(
    asset: Asset,
    allAssets: List<Asset>,
    onUnlink: (Asset) -> Unit,
    onLinkToParent: (Asset, Asset) -> Unit
) {
    var showLinkPicker by remember { mutableStateOf(false) }

    val children = remember(asset, allAssets) {
        allAssets.filter { it.sourceLinkId == asset.uid && it.uid != asset.uid }
    }
    
    val parent = remember(asset, allAssets) {
        allAssets.find { it.uid == asset.sourceLinkId && it.uid != asset.uid }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "LINEAGE & LINKS",
            color = VaultWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // --- PARENT SECTION ---
        LineageHeader(title = "SOURCE PARENT")
        if (parent != null) {
            LinkedAssetItem(parent, isParent = true) { onUnlink(asset) }
        } else if (asset.hasPntr()) {
            InfoBox("This asset is a PRIMARY SOURCE (Parent).")
        } else {
            EmptyLinkState("No source link assigned.") { showLinkPicker = true }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- CHILDREN SECTION ---
        LineageHeader(title = "VARIANTS (CHILDREN)")
        if (children.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(children) { child ->
                    LinkedAssetItem(child, isParent = false) { onUnlink(child) }
                    Divider(color = VaultOutline, thickness = 0.5.dp)
                }
            }
        } else {
            InfoBox("No variants are linked to this asset.")
        }

        // Action Button to add links
        if (asset.hasPntr() || asset.sourceLinkId == 0L) {
            Button(
                onClick = { showLinkPicker = true },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VaultSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = VaultWhite)
                Spacer(Modifier.width(8.dp))
                Text("MANAGE LINKS", color = VaultWhite)
            }
        }
    }

    // Fixed Parameter mismatch: LinkPickerDialog call
    if (showLinkPicker) {
        LinkPickerDialog(
            initiator = asset,
            allAssets = allAssets,
            onDismiss = { showLinkPicker = false },
            onLinkSelected = { selectedParent ->
                onLinkToParent(asset, selectedParent)
                showLinkPicker = false
            },
            onToggleLink = { targetChild, shouldLink ->
                if (shouldLink) onLinkToParent(targetChild, asset)
                else onUnlink(targetChild)
            }
        )
    }
}

@Composable
private fun LineageHeader(title: String) {
    Text(
        text = title,
        color = VaultSilver,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun LinkedAssetItem(target: Asset, isParent: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            PickerAssetRow(asset = target, onClick = {})
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.LinkOff,
                contentDescription = "Unlink",
                tint = Color(0xFFE57373)
            )
        }
    }
}

@Composable
private fun InfoBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultBlack, RoundedCornerShape(8.dp))
            .border(0.5.dp, VaultOutline, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(text = text, color = VaultSilver, fontSize = 12.sp)
    }
}

@Composable
private fun EmptyLinkState(text: String, onAdd: () -> Unit) {
    OutlinedButton(
        onClick = onAdd,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, VaultOutline)
    ) {
        Icon(Icons.Default.Link, contentDescription = null, tint = VaultSilver)
        Spacer(Modifier.width(8.dp))
        Text(text, color = VaultSilver, fontSize = 12.sp)
    }
}
