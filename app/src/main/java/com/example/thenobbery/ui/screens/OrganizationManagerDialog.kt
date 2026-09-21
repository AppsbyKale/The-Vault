package com.example.thenobbery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.logic.OrganizationPreferenceManager
import com.example.thenobbery.ui.theme.*

@Composable
fun OrganizationManagerDialog(
    onDismiss: () -> Unit,
    onDataChanged: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    var categories by remember {
        mutableStateOf(
            (OrganizationPreferenceManager.getCategories(context) + AssetDefinitions.DefaultCategories).distinct().sorted()
        )
    }
    var collections by remember {
        mutableStateOf(
            (OrganizationPreferenceManager.getCollections(context) + AssetDefinitions.DefaultCollections).distinct().sorted()
        )
    }
    var tags by remember {
        mutableStateOf(OrganizationPreferenceManager.getTags(context))
    }

    Dialog(onDismissRequest = onDismiss) {
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
                        "MANAGE ORGANIZATION",
                        color = VaultWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = VaultSilver)
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VaultSurface,
                    contentColor = VaultWhite,
                    divider = { Divider(color = VaultOutline) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("CATEGORIES", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("COLLECTIONS", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("TAGS", fontSize = 11.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> CategoryTab(
                        categories = categories,
                        onAdd = { name ->
                            categories = (categories + name.uppercase()).distinct().sorted()
                            OrganizationPreferenceManager.saveCategories(context, categories)
                            onDataChanged()
                        },
                        onEdit = { oldName, newName ->
                            categories = categories.map { if (it == oldName) newName.uppercase() else it }.sorted()
                            OrganizationPreferenceManager.saveCategories(context, categories)
                            onDataChanged()
                        },
                        onDelete = { name ->
                            categories = categories - name
                            OrganizationPreferenceManager.saveCategories(context, categories)
                            onDataChanged()
                        }
                    )
                    1 -> CollectionTab(
                        collections = collections,
                        onAdd = { name ->
                            collections = (collections + name.uppercase()).distinct().sorted()
                            OrganizationPreferenceManager.saveCollections(context, collections)
                            onDataChanged()
                        },
                        onEdit = { oldName, newName ->
                            collections = collections.map { if (it == oldName) newName.uppercase() else it }.sorted()
                            OrganizationPreferenceManager.saveCollections(context, collections)
                            onDataChanged()
                        },
                        onDelete = { name ->
                            collections = collections - name
                            OrganizationPreferenceManager.saveCollections(context, collections)
                            onDataChanged()
                        }
                    )
                    2 -> TagTab(
                        tags = tags,
                        onAdd = { name ->
                            tags = tags + name.uppercase()
                            OrganizationPreferenceManager.saveTags(context, tags)
                            onDataChanged()
                        },
                        onEdit = { oldName, newName ->
                            tags = tags.map { if (it == oldName) newName.uppercase() else it }.toSet()
                            OrganizationPreferenceManager.saveTags(context, tags)
                            onDataChanged()
                        },
                        onDelete = { name ->
                            tags = tags - name
                            OrganizationPreferenceManager.saveTags(context, tags)
                            onDataChanged()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTab(
    categories: List<String>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    ManageableList(
        items = categories,
        itemLabel = { it },
        defaultItems = AssetDefinitions.DefaultCategories.toSet(),
        onAdd = onAdd,
        onEdit = onEdit,
        onDelete = onDelete,
        addDialogTitle = "NEW CATEGORY",
        editDialogTitle = "EDIT CATEGORY"
    )
}

@Composable
private fun CollectionTab(
    collections: List<String>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    ManageableList(
        items = collections,
        itemLabel = { it },
        defaultItems = AssetDefinitions.DefaultCollections.toSet(),
        onAdd = onAdd,
        onEdit = onEdit,
        onDelete = onDelete,
        addDialogTitle = "NEW COLLECTION",
        editDialogTitle = "EDIT COLLECTION"
    )
}

@Composable
private fun TagTab(
    tags: Set<String>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    ManageableList(
        items = tags.toList().sorted(),
        itemLabel = { it },
        defaultItems = emptySet(),
        onAdd = onAdd,
        onEdit = onEdit,
        onDelete = onDelete,
        addDialogTitle = "NEW TAG",
        editDialogTitle = "EDIT TAG"
    )
}

@Composable
private fun <T> ManageableList(
    items: List<T>,
    itemLabel: (T) -> String,
    defaultItems: Set<String>,
    onAdd: (String) -> Unit,
    onEdit: (String, String) -> Unit,
    onDelete: (T) -> Unit,
    addDialogTitle: String,
    editDialogTitle: String
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<T?>(null) }
    var deletingItem by remember { mutableStateOf<T?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = VaultWhite, contentColor = VaultBlack)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("ADD NEW", fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            items(items, key = { itemLabel(it) }) { item ->
                val label = itemLabel(item)
                val isDefault = defaultItems.contains(label)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        color = VaultWhite,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isDefault) {
                        IconButton(onClick = { editingItem = item }) {
                            Icon(Icons.Default.Edit, null, tint = VaultSilver, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { deletingItem = item }) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373), modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Text(
                            "DEFAULT",
                            color = VaultSilver,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
                Divider(color = VaultOutline, thickness = 0.5.dp)
            }
        }
    }

    if (showAddDialog) {
        AddEditDialog(
            title = addDialogTitle,
            currentValue = "",
            onConfirm = { name ->
                onAdd(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    editingItem?.let { item ->
        AddEditDialog(
            title = editDialogTitle,
            currentValue = itemLabel(item),
            onConfirm = { name ->
                onEdit(itemLabel(item), name)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            containerColor = VaultBlack,
            modifier = Modifier.border(1.dp, VaultOutline, RoundedCornerShape(16.dp)),
            title = { Text("DELETE?", color = VaultWhite, fontWeight = FontWeight.Bold) },
            text = { Text("Remove \"${itemLabel(item)}\"?", color = VaultSilver) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(item)
                    deletingItem = null
                }) {
                    Text("DELETE", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("CANCEL", color = VaultSilver)
                }
            }
        )
    }
}

@Composable
private fun AddEditDialog(
    title: String,
    currentValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultBlack,
        modifier = Modifier.border(1.dp, VaultSurface, RoundedCornerShape(16.dp)),
        title = { Text(title, color = VaultWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
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
                if (value.isNotBlank()) {
                    onConfirm(value.trim())
                }
            }) {
                Text("CONFIRM", color = VaultWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = VaultSilver)
            }
        }
    )
}
