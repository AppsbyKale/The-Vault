package com.example.thenobbery.ui.screens.profiledialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.ui.theme.*

@Composable
fun OrganizationTabContent(
    asset: Asset,
    allAssets: List<Asset>,
    onUpdate: (Asset) -> Unit
) {
    val existingCategories = (allAssets.map { it.classification } + listOf(asset.classification))
        .distinct().filter { it.isNotBlank() }.sorted()
    
    val existingCollections = (allAssets.flatMap { AssetDefinitions.stringToCollections(it.collections) } + AssetDefinitions.stringToCollections(asset.collections))
        .distinct().filter { it.isNotBlank() }.sorted()

    var showAddDialog by remember { mutableStateOf<String?>(null) }
    var newValue by remember { mutableStateOf("") }
    var tagToEdit by remember { mutableStateOf<String?>(null) }
    var tagToDelete by remember { mutableStateOf<String?>(null) }
    var collectionToRemove by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = 32.dp)) {
        
        OrgDropdown(
            label = "CATEGORY",
            currentValue = asset.classification,
            options = existingCategories,
            onSelect = { onUpdate(asset.copy(classification = it)) },
            onAddNew = { showAddDialog = "CAT" }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("COLLECTIONS", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentCollections = AssetDefinitions.stringToCollections(asset.collections)
            currentCollections.forEach { coll ->
                TagChip(
                    tag = coll,
                    onRemove = { collectionToRemove = coll }
                )
            }
            
            IconButton(
                onClick = { showAddDialog = "COLL" },
                modifier = Modifier
                    .size(36.dp)
                    .background(VaultSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, VaultOutline, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Add, null, tint = VaultWhite, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("TAGS", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentTags = AssetDefinitions.stringToTags(asset.tags)
            currentTags.forEach { tag ->
                TagChip(
                    tag = tag,
                    onRemove = { tagToDelete = tag },
                    onEdit = { tagToEdit = tag }
                )
            }
            
            IconButton(
                onClick = { showAddDialog = "TAG" },
                modifier = Modifier
                    .size(36.dp)
                    .background(VaultSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, VaultOutline, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Default.Add, null, tint = VaultWhite, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showAddDialog != null) {
        AlertDialog(
            onDismissRequest = { showAddDialog = null; newValue = "" },
            modifier = Modifier.border(1.dp, VaultSurface, RoundedCornerShape(16.dp)),
            containerColor = VaultBlack,
            title = { 
                Text(
                    text = when(showAddDialog) {
                        "CAT" -> "NEW CATEGORY"
                        "COLL" -> "NEW COLLECTION"
                        else -> "ADD TAG"
                    }, 
                    color = VaultWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = VaultWhite, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        focusedContainerColor = VaultBlack,
                        unfocusedContainerColor = VaultBlack,
                        focusedBorderColor = VaultWhite,
                        unfocusedBorderColor = VaultSurface,
                        cursorColor = VaultWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newValue.isNotBlank()) {
                        val cleanVal = newValue.trim().uppercase()
                        when(showAddDialog) {
                            "CAT" -> onUpdate(asset.copy(classification = cleanVal))
                            "COLL" -> {
                                val currentCollections = AssetDefinitions.stringToCollections(asset.collections).toMutableList()
                                if (!currentCollections.contains(cleanVal)) {
                                    currentCollections.add(cleanVal)
                                    onUpdate(asset.copy(collections = AssetDefinitions.collectionsToString(currentCollections)))
                                }
                            }
                            "TAG" -> {
                                val currentTags = AssetDefinitions.stringToTags(asset.tags).toMutableList()
                                if (!currentTags.contains(cleanVal)) {
                                    currentTags.add(cleanVal)
                                    onUpdate(asset.copy(tags = AssetDefinitions.tagsToString(currentTags)))
                                }
                            }
                        }
                    }
                    showAddDialog = null
                    newValue = ""
                }) {
                    Text("CONFIRM", color = VaultWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = null; newValue = "" }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }

    tagToEdit?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToEdit = null },
            modifier = Modifier.border(1.dp, VaultSurface, RoundedCornerShape(16.dp)),
            containerColor = VaultBlack,
            title = { Text("EDIT TAG", color = VaultWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newValue,
                    onValueChange = { newValue = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = VaultWhite, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        focusedContainerColor = VaultBlack,
                        unfocusedContainerColor = VaultBlack,
                        focusedBorderColor = VaultWhite,
                        unfocusedBorderColor = VaultSurface,
                        cursorColor = VaultWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newValue.isNotBlank()) {
                        val cleanVal = newValue.trim().uppercase()
                        val currentTags = AssetDefinitions.stringToTags(asset.tags).toMutableList()
                        val index = currentTags.indexOf(tag)
                        if (index >= 0 && !currentTags.contains(cleanVal)) {
                            currentTags[index] = cleanVal
                            onUpdate(asset.copy(tags = AssetDefinitions.tagsToString(currentTags)))
                        }
                    }
                    tagToEdit = null
                    newValue = ""
                }) {
                    Text("SAVE", color = VaultWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToEdit = null; newValue = "" }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
        LaunchedEffect(tag) { newValue = tag }
    }

    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            modifier = Modifier.border(1.dp, VaultSurface, RoundedCornerShape(16.dp)),
            containerColor = VaultBlack,
            title = { Text("DELETE TAG?", color = VaultWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"$tag\" from this asset?", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    val currentTags = AssetDefinitions.stringToTags(asset.tags).toMutableList()
                    currentTags.remove(tag)
                    onUpdate(asset.copy(tags = AssetDefinitions.tagsToString(currentTags)))
                    tagToDelete = null
                }) {
                    Text("DELETE", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }

    collectionToRemove?.let { coll ->
        AlertDialog(
            onDismissRequest = { collectionToRemove = null },
            modifier = Modifier.border(1.dp, VaultSurface, RoundedCornerShape(16.dp)),
            containerColor = VaultBlack,
            title = { Text("REMOVE COLLECTION?", color = VaultWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"$coll\" from this asset?", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    val currentCollections = AssetDefinitions.stringToCollections(asset.collections).toMutableList()
                    currentCollections.remove(coll)
                    onUpdate(asset.copy(collections = AssetDefinitions.collectionsToString(currentCollections)))
                    collectionToRemove = null
                }) {
                    Text("REMOVE", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToRemove = null }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }
}

@Composable
fun OrgDropdown(
    label: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onAddNew: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VaultBlack, RoundedCornerShape(8.dp))
                    .border(1.dp, VaultSurface, RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(currentValue.uppercase(), color = VaultWhite, fontSize = 13.sp)
                Icon(Icons.Default.ArrowDropDown, null, tint = VaultSilver)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(VaultSurface).border(1.dp, VaultOutline).fillMaxWidth(0.7f)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt.uppercase(), color = VaultWhite, fontSize = 12.sp) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(VaultOutline))
                DropdownMenuItem(
                    text = { Text("+ CREATE NEW", color = VaultSilver, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    onClick = { onAddNew(); expanded = false }
                )
            }
        }
    }
}

@Composable
fun TagChip(tag: String, onRemove: () -> Unit, onEdit: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .background(VaultSurface, RoundedCornerShape(4.dp))
            .border(1.dp, VaultOutline, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(tag.uppercase(), color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.Default.Close, 
                null, 
                tint = VaultSilver, 
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
