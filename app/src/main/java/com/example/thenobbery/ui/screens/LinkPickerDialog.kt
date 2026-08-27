package com.example.thenobbery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.components.PickerAssetRow
import com.example.thenobbery.ui.theme.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPickerDialog(
    initiator: Asset,
    allAssets: List<Asset>,
    onLinkSelected: (Asset) -> Unit, // Used for Child Mode
    onToggleLink: (Asset, Boolean) -> Unit, // Used for Parent Mode
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // 1. Determine Mode
    // If the initiator is already a Parent or has a pntr, we are managing its children.
    // Otherwise, we are a standalone looking for a parent.
    val isParentMode = initiator.isParent() || initiator.hasPntr()

    // 2. Filter logic
    val filteredAssets = remember(searchQuery, allAssets) {
        allAssets.filter { asset ->
            val matchesSearch = asset.title.contains(searchQuery, ignoreCase = true)
            val isNotSelf = asset.uid != initiator.uid
            
            if (isParentMode) {
                // Parent Mode: Show everyone except yourself (to pick children)
                matchesSearch && isNotSelf
            } else {
                // Child Mode: Show only assets that HAVE a pntr (eligible parents)
                matchesSearch && isNotSelf && asset.hasPntr()
            }
        }.sortedWith(compareByDescending<Asset> { 
            // In Parent Mode, put current children at the top
            if (isParentMode) it.sourceLinkId == initiator.uid else false 
        }.thenBy { it.title.lowercase() })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(28.dp),
            color = VaultSurface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (isParentMode) "MANAGE CHILDREN" else "SELECT PARENT",
                    color = VaultWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = if (isParentMode) 
                        "Select assets to link to ${initiator.title}" 
                        else "Link ${initiator.title} to a source file",
                    color = VaultSilver,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Search Bar
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search assets...", color = VaultSilver) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultSilver) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VaultBlack,
                        unfocusedContainerColor = VaultBlack,
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        cursorColor = VaultWhite,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Asset List
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredAssets) { asset ->
                        val isCurrentlyLinked = asset.sourceLinkId == initiator.uid
                        
                        PickerAssetRow(
                            asset = asset,
                            isSelected = if (isParentMode) isCurrentlyLinked else false,
                            isParentMode = isParentMode,
                            onClick = {
                                if (isParentMode) {
                                    onToggleLink(asset, !isCurrentlyLinked)
                                } else {
                                    onLinkSelected(asset)
                                    onDismiss()
                                }
                            },
                            onSelect = {
                                if (isParentMode) onToggleLink(asset, !isCurrentlyLinked)
                            }
                        )
                        Divider(color = VaultOutline, thickness = 0.5.dp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CLOSE", color = VaultSilver, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Helper row used specifically within the Picker Dialog
 */
@Composable
fun PickerAssetRow(
    asset: Asset,
    isSelected: Boolean,
    isParentMode: Boolean,
    onClick: () -> Unit,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail and Title (Standard UI from your PickerAssetRow)
        // Note: I'm assuming PickerAssetRow logic from your AssetComponents.kt
        com.example.thenobbery.ui.components.PickerAssetRow(
            asset = asset,
            onClick = onClick
        )
        
        // This takes the checkbox/selection logic out of the shared component 
        // to keep it specialized for this Dialog's needs.
        if (isParentMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = VaultWhite,
                    uncheckedColor = VaultOutline,
                    checkmarkColor = VaultBlack
                )
            )
        }
    }
}
