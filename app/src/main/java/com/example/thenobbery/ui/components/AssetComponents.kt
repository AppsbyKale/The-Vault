package com.example.thenobbery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.ui.theme.*
import java.io.File

/**
 * SHARED COMPONENT: Used by LinkPickerDialog and MergePickerSheet
 */
@Composable
fun PickerAssetRow(asset: Asset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbPath = asset.primaryBackgroundPngPath ?: asset.primaryTransparentPngPath
        AsyncImage(
            model = thumbPath?.let { File(it) },
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(VaultBlack),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = asset.title.uppercase(),
                color = VaultWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            val collectionsList = AssetDefinitions.stringToCollections(asset.collections)
            Text(
                text = if (collectionsList.isEmpty()) "NONE" else collectionsList.joinToString(", ").uppercase(),
                color = VaultSilver,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Generic Searchable Picker for selecting assets (Linking/Merging)
 */
@Composable
fun <T> GenericAssetPicker(
    title: String,
    items: List<T>,
    searchPlaceholder: String,
    itemToTitle: (T) -> String,
    itemContent: @Composable (T) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, items) {
        items.filter { itemToTitle(it).contains(searchQuery, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = VaultBlack
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    color = VaultWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(searchPlaceholder, color = VaultSilver) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultSilver) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = VaultSurface,
                        unfocusedContainerColor = VaultSurface,
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        cursorColor = VaultWhite,
                        focusedIndicatorColor = VaultWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filteredItems) { item ->
                        itemContent(item)
                        Divider(color = VaultOutline, thickness = 0.5.dp)
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("CANCEL", color = VaultSilver, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
