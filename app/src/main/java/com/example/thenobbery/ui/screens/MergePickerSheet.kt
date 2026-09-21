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

/**
 * The Merge Picker allows the user to select a "Target" asset.
 * Data from the initiator will be moved into this selected asset.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergePickerSheet(
    targetAsset: Asset, // The asset we are starting from
    potentialParents: List<Asset>,
    onAssetSelected: (Asset) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter out the initiator itself and apply search
    val filteredList = remember(searchQuery, potentialParents) {
        potentialParents.filter { 
            it.uid != targetAsset.uid && 
            it.title.contains(searchQuery, ignoreCase = true)
        }
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
                    text = "MERGE INTO...",
                    color = VaultWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                
                Text(
                    text = "Choose the asset that will receive files and tags from ${targetAsset.title}",
                    color = VaultSilver,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // The Search Picker interface
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search destination...", color = VaultSilver) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultSilver) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VaultBlack,
                        unfocusedContainerColor = VaultBlack,
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        cursorColor = AccentPntr,
                        focusedIndicatorColor = AccentPntr,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // The List of selectable assets
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredList) { asset ->
                        PickerAssetRow(
                            asset = asset, 
                            onClick = { onAssetSelected(asset) }
                        )
                        // Using Divider to match AIDE/Material3 environment
                        Divider(
                            color = VaultOutline, 
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = VaultSilver, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
