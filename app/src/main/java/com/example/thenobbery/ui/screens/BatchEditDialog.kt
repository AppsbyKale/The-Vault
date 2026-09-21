package com.example.thenobbery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditDialog(
    assets: List<Asset>,
    allAssets: List<Asset>,
    categories: List<String> = AssetDefinitions.DefaultCategories,
    collections: List<String> = AssetDefinitions.DefaultCollections,
    tags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onApply: (tagsMode: EditMode, tagsToAdd: Set<String>, tagsToRemove: Set<String>,
              collectionsMode: EditMode, collectionsToAdd: Set<String>, collectionsToRemove: Set<String>,
              categoryMode: EditMode, category: String?) -> Unit
) {
    val allTags = remember(assets, allAssets, tags) {
        (tags + assets.flatMap { AssetDefinitions.stringToTags(it.tags) } + allAssets.flatMap { AssetDefinitions.stringToTags(it.tags) }).toSet().sorted()
    }
    val allCollections = remember(assets, allAssets, collections) {
        (collections + assets.flatMap { AssetDefinitions.stringToCollections(it.collections) } + allAssets.flatMap { AssetDefinitions.stringToCollections(it.collections) }).toSet().sorted()
    }
    val existingCategories = remember(allAssets, categories) {
        (categories + allAssets.map { it.classification }).distinct().filter { it.isNotBlank() }
    }
    val topTags = remember(assets) {
        assets.flatMap { AssetDefinitions.stringToTags(it.tags) }
            .groupingBy { it }.eachCount()
            .toList().sortedByDescending { it.second }.take(5).map { it.first }
    }
    val topCollections = remember(assets) {
        assets.flatMap { AssetDefinitions.stringToCollections(it.collections) }
            .groupingBy { it }.eachCount()
            .toList().sortedByDescending { it.second }.take(5).map { it.first }
    }

    var selectedTags by remember { mutableStateOf(allTags.toSet()) }
    var selectedCollections by remember { mutableStateOf(allCollections.toSet()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var tagsMode by remember { mutableStateOf(EditMode.REPLACE) }
    var collectionsMode by remember { mutableStateOf(EditMode.REPLACE) }
    var categoryMode by remember { mutableStateOf(EditMode.REPLACE) }

    var showTagsDropdown by remember { mutableStateOf(false) }
    var showCollectionsDropdown by remember { mutableStateOf(false) }

    var newTagsText by remember { mutableStateOf("") }
    var newCollectionsText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
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
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "BATCH EDIT (${assets.size} assets)",
                        color = VaultWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = VaultSilver)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("CATEGORY", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = categoryMode == EditMode.REPLACE,
                                onClick = { categoryMode = EditMode.REPLACE },
                                label = { Text("Replace", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultSurface,
                                    selectedLabelColor = VaultWhite
                                )
                            )
                            FilterChip(
                                selected = categoryMode == EditMode.ADD,
                                onClick = { categoryMode = EditMode.ADD },
                                label = { Text("If Unassigned", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultSurface,
                                    selectedLabelColor = VaultWhite
                                )
                            )
                        }
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                            ) {
                                Text(
                                    selectedCategory ?: "Select category...",
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = VaultWhite)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(VaultBlack).border(1.dp, VaultOutline)
                            ) {
                                existingCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat, color = VaultWhite, fontSize = 13.sp) },
                                        onClick = {
                                            selectedCategory = cat
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Divider(color = VaultOutline)
                    }

                    item {
                        Text("COLLECTIONS", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = collectionsMode == EditMode.ADD,
                                onClick = { collectionsMode = EditMode.ADD },
                                label = { Text("Add", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultSurface,
                                    selectedLabelColor = VaultWhite
                                )
                            )
                            FilterChip(
                                selected = collectionsMode == EditMode.REPLACE,
                                onClick = { collectionsMode = EditMode.REPLACE },
                                label = { Text("Replace", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultSurface,
                                    selectedLabelColor = VaultWhite
                                )
                            )
                        }
                    }

                    item {
                        if (topCollections.isNotEmpty()) {
                            Text("Top 5:", color = VaultSilver, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(topCollections) { coll ->
                                    FilterChip(
                                        selected = coll in selectedCollections,
                                        onClick = {
                                            selectedCollections = if (coll in selectedCollections) {
                                                selectedCollections - coll
                                            } else {
                                                selectedCollections + coll
                                            }
                                        },
                                        label = {
                                            Text(
                                                if (coll in selectedCollections) "$coll ✓" else coll,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VaultSurface,
                                            selectedLabelColor = VaultWhite
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box {
                            OutlinedButton(
                                onClick = { showCollectionsDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                            ) {
                                Text("All collections: ${selectedCollections.size} selected", fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = VaultWhite)
                            }
                            DropdownMenu(
                                expanded = showCollectionsDropdown,
                                onDismissRequest = { showCollectionsDropdown = false },
                                modifier = Modifier.background(VaultBlack).border(1.dp, VaultOutline)
                            ) {
                                allCollections.forEach { coll ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCollections = if (coll in selectedCollections) {
                                                    selectedCollections - coll
                                                } else {
                                                    selectedCollections + coll
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = coll in selectedCollections,
                                            onCheckedChange = {
                                                selectedCollections = if (it) selectedCollections + coll else selectedCollections - coll
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = VaultWhite)
                                        )
                                        Text(coll, color = VaultWhite, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = newCollectionsText,
                            onValueChange = { newCollectionsText = it },
                            label = { Text("Add new collections (comma-separated)", color = VaultSilver, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VaultWhite,
                                unfocusedTextColor = VaultWhite,
                                focusedBorderColor = VaultWhite,
                                unfocusedBorderColor = VaultOutline
                            ),
                            singleLine = true
                        )
                    }

                    item {
                        Divider(color = VaultOutline)
                    }

                    item {
                        Text("TAGS", color = VaultSilver, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tagsMode == EditMode.ADD,
                                onClick = { tagsMode = EditMode.ADD },
                                label = { Text("Add", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultSurface,
                                    selectedLabelColor = VaultWhite
                                )
                            )
                            FilterChip(
                                selected = tagsMode == EditMode.REPLACE,
                                onClick = { tagsMode = EditMode.REPLACE },
                                label = { Text("Replace", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = VaultSurface,
                                    selectedLabelColor = VaultWhite
                                )
                            )
                        }
                    }

                    item {
                        if (topTags.isNotEmpty()) {
                            Text("Top 5:", color = VaultSilver, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(topTags) { tag ->
                                    FilterChip(
                                        selected = tag in selectedTags,
                                        onClick = {
                                            selectedTags = if (tag in selectedTags) {
                                                selectedTags - tag
                                            } else {
                                                selectedTags + tag
                                            }
                                        },
                                        label = {
                                            Text(
                                                if (tag in selectedTags) "$tag ✓" else tag,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = VaultSurface,
                                            selectedLabelColor = VaultWhite
                                        )
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Box {
                            OutlinedButton(
                                onClick = { showTagsDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                            ) {
                                Text("All tags: ${selectedTags.size} selected", fontSize = 12.sp)
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, null, tint = VaultWhite)
                            }
                            DropdownMenu(
                                expanded = showTagsDropdown,
                                onDismissRequest = { showTagsDropdown = false },
                                modifier = Modifier.background(VaultBlack).border(1.dp, VaultOutline)
                            ) {
                                allTags.forEach { tag ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedTags = if (tag in selectedTags) {
                                                    selectedTags - tag
                                                } else {
                                                    selectedTags + tag
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = tag in selectedTags,
                                            onCheckedChange = {
                                                selectedTags = if (it) selectedTags + tag else selectedTags - tag
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = VaultWhite)
                                        )
                                        Text(tag, color = VaultWhite, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = newTagsText,
                            onValueChange = { newTagsText = it },
                            label = { Text("Add new tags (comma-separated)", color = VaultSilver, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = VaultWhite,
                                unfocusedTextColor = VaultWhite,
                                focusedBorderColor = VaultWhite,
                                unfocusedBorderColor = VaultOutline
                            ),
                            singleLine = true
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultSilver)
                    ) {
                        Text("CANCEL")
                    }
                    Button(
                        onClick = {
                            val tagsToAdd = newTagsText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .toSet()
                            val collectionsToAdd = newCollectionsText.split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                                .toSet()

                            onApply(
                                tagsMode, tagsToAdd, selectedTags,
                                collectionsMode, collectionsToAdd, selectedCollections,
                                categoryMode, selectedCategory
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultWhite, contentColor = VaultBlack)
                    ) {
                        Text("APPLY", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

enum class EditMode {
    ADD,
    REPLACE
}
