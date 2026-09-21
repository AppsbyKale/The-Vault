package com.example.thenobbery.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.ui.theme.*
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.logic.WatermarkPreferenceManager
import com.example.thenobbery.data.Asset
import com.example.thenobbery.viewmodels.AssetFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryTopBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onSortOrderChanged: (AssetDefinitions.SortOrder) -> Unit,
    onFilterChanged: (AssetFilter) -> Unit,
    currentFilter: AssetFilter,
    currentSortOrder: AssetDefinitions.SortOrder,
    allCategories: List<String>,
    allCollections: List<String>,
    allTags: List<String>,
    totalCount: Int,
    filteredCount: Int,
    isMultiselectMode: Boolean = false,
    selectedCount: Int = 0,
    onMultiselectToggle: () -> Unit = {},
    onBulkAction: (BulkAction) -> Unit = {},
    onNewImportsClick: () -> Unit = {},
    onCollectionStatsClick: () -> Unit = {},
    onExportBackup: () -> Unit = {},
    onExportBackupToLocation: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    onBatchEdit: (List<Asset>) -> Unit = { _ -> },
    onShowBackupDialog: () -> Unit = {},
    onShowSingleFilesClick: () -> Unit = {},
    onShowOrganizationManager: () -> Unit = {}
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showBulkMenu by remember { mutableStateOf(false) }
    var showFilterSortSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val wmManager = remember { WatermarkPreferenceManager(context) }

    val pickWatermarkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { wmManager.saveWatermarkFile(it) }
    }

    Column(modifier = Modifier.background(VaultBlack)) {
        CenterAlignedTopAppBar(
            title = { 
                if (isMultiselectMode) {
                    Text(
                        "$selectedCount SELECTED", 
                        color = AccentMaster, 
                        fontSize = 16.sp, 
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "THE NOBBERY VAULT", 
                        color = VaultWhite, 
                        fontSize = 18.sp, 
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                if (isMultiselectMode) {
                    IconButton(onClick = onMultiselectToggle) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = VaultSilver)
                    }
                }
            },
            actions = {
                if (isMultiselectMode) {
                    Box {
                        IconButton(onClick = { showBulkMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Bulk Actions", tint = VaultWhite)
                        }
                        DropdownMenu(
                            expanded = showBulkMenu,
                            onDismissRequest = { showBulkMenu = false },
                            modifier = Modifier.background(VaultBlack).border(1.dp, VaultOutline)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row {
                                        TextButton(
                                            onClick = {
                                                onBulkAction(BulkAction.EXPORT_SHARE)
                                                showBulkMenu = false
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = VaultWhite)
                                        ) {
                                            Icon(Icons.Default.FileDownload, null, tint = VaultWhite, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("EXPORT", fontSize = 14.sp)
                                        }
                                        Text("  /  ", color = VaultSilver, fontSize = 14.sp)
                                        TextButton(
                                            onClick = {
                                                onBulkAction(BulkAction.EXPORT_SHARE)
                                                showBulkMenu = false
                                            },
                                            colors = ButtonDefaults.textButtonColors(contentColor = VaultWhite)
                                        ) {
                                            Icon(Icons.Default.Share, null, tint = VaultWhite, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("SHARE", fontSize = 14.sp)
                                        }
                                    }
                                },
                                onClick = {}
                            )
                            Divider(color = VaultOutline)
                            DropdownMenuItem(
                                text = { Text(BulkAction.DELETE.displayName, color = Color(0xFFE57373), fontSize = 14.sp) },
                                onClick = { 
                                    onBulkAction(BulkAction.DELETE)
                                    showBulkMenu = false
                                },
                                leadingIcon = { 
                                    Icon(Icons.Default.Delete, null, tint = Color(0xFFE57373))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(BulkAction.VIEW_PROFILE.displayName, color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    onBulkAction(BulkAction.VIEW_PROFILE)
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Visibility, null, tint = VaultWhite) }
                            )
                            Divider(color = VaultOutline)
                            DropdownMenuItem(
                                text = { Text(BulkAction.LINK.displayName, color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    onBulkAction(BulkAction.LINK)
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Link, null, tint = VaultWhite) }
                            )
                            DropdownMenuItem(
                                text = { Text(BulkAction.MERGE.displayName, color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    onBulkAction(BulkAction.MERGE)
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.MergeType, null, tint = VaultWhite) }
                            )
                            DropdownMenuItem(
                                text = { Text(BulkAction.BATCH_EDIT.displayName, color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    onBulkAction(BulkAction.BATCH_EDIT)
                                    showBulkMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, null, tint = VaultWhite) }
                            )
                        }
                    }
                } else {
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = VaultSilver)
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false },
                            modifier = Modifier.background(VaultBlack).border(1.dp, VaultOutline)
                        ) {
                            DropdownMenuItem(
                                text = { Text("MULTISELECT", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    onMultiselectToggle()
                                },
                                leadingIcon = { Icon(Icons.Default.Checklist, null, tint = VaultWhite) }
                            )
                            Divider(color = VaultOutline)
                            DropdownMenuItem(
                                text = { Text("UPLOAD WATERMARK", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    pickWatermarkLauncher.launch("image/png")
                                },
                                leadingIcon = { Icon(Icons.Default.Image, null, tint = VaultWhite) }
                            )
                            Divider(color = VaultOutline)
                            DropdownMenuItem(
                                text = { Text("COLLECTION STATS", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    onCollectionStatsClick()
                                },
                                leadingIcon = { Icon(Icons.Default.BarChart, null, tint = VaultWhite) }
                            )
                            DropdownMenuItem(
                                text = { Text("MANAGE ORGANIZATION", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    onShowOrganizationManager()
                                },
                                leadingIcon = { Icon(Icons.Default.Category, null, tint = VaultWhite) }
                            )
                            DropdownMenuItem(
                                text = { Text("NEW IMPORTS", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    onNewImportsClick()
                                },
                                leadingIcon = { Icon(Icons.Default.Download, null, tint = VaultWhite) }
                            )
                            Divider(color = VaultOutline)
                            DropdownMenuItem(
                                text = { Text("BACKUP / RESTORE", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    onShowBackupDialog()
                                },
                                leadingIcon = { Icon(Icons.Default.Backup, null, tint = VaultWhite) }
                            )
                            DropdownMenuItem(
                                text = { Text("SINGLE FILES", color = VaultWhite, fontSize = 14.sp) },
                                onClick = { 
                                    showSettingsMenu = false
                                    onShowSingleFilesClick()
                                },
                                leadingIcon = { Icon(Icons.Default.InsertDriveFile, null, tint = VaultWhite) }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = VaultBlack
            )
        )

        if (!isMultiselectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search Vault...", color = VaultSilver, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = VaultSilver) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = VaultSilver)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultWhite,
                        unfocusedTextColor = VaultWhite,
                        focusedContainerColor = VaultSurface,
                        unfocusedContainerColor = VaultSurface,
                        focusedBorderColor = VaultWhite,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))
                
                Box {
                    IconButton(
                        onClick = { showFilterSortSheet = true },
                        modifier = Modifier.background(VaultSurface, RoundedCornerShape(12.dp))
                    ) {
                        Box {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter & Sort", tint = VaultWhite)
                            if (currentFilter.isActive) {
                                Badge(
                                    containerColor = VaultWhite,
                                    contentColor = VaultBlack,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Text(
                                        text = listOfNotNull(
                                            currentFilter.category,
                                            currentFilter.collections.size.takeIf { it > 0 },
                                            currentFilter.tags.size.takeIf { it > 0 },
                                            currentFilter.showUncategorized,
                                            currentFilter.showUncollected,
                                            currentFilter.showUntagged
                                        ).size.toString(),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSortSheet) {
        FilterSortBottomSheet(
            currentFilter = currentFilter,
            currentSortOrder = currentSortOrder,
            allCategories = allCategories,
            allCollections = allCollections,
            allTags = allTags,
            totalCount = totalCount,
            filteredCount = filteredCount,
            onFilterChanged = onFilterChanged,
            onSortOrderChanged = onSortOrderChanged,
            onDismiss = { showFilterSortSheet = false }
        )
    }
}

enum class BulkAction(val displayName: String) {
    LINK("LINK TO PARENT"),
    MERGE("MERGE ASSETS"),
    EXPORT_SHARE("EXPORT / SHARE"),
    DELETE("DELETE"),
    VIEW_PROFILE("VIEW PROFILE"),
    BATCH_EDIT("BATCH EDIT")
}
