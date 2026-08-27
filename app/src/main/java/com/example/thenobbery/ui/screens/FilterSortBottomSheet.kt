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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.ui.theme.*
import com.example.thenobbery.viewmodels.AssetFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSortBottomSheet(
    currentFilter: AssetFilter,
    currentSortOrder: AssetDefinitions.SortOrder,
    allCategories: List<String>,
    allCollections: List<String>,
    allTags: List<String>,
    totalCount: Int,
    filteredCount: Int,
    onFilterChanged: (AssetFilter) -> Unit,
    onSortOrderChanged: (AssetDefinitions.SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    var localFilter by remember(currentFilter) { mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VaultBlack,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FILTER & SORT",
                    color = VaultWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = VaultSilver)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SectionTitle("SORT BY")
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssetDefinitions.SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = currentSortOrder == order,
                        onClick = { onSortOrderChanged(order) },
                        label = { Text(order.name, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VaultSurface,
                            selectedLabelColor = VaultWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onFilterChanged(localFilter)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VaultWhite, contentColor = VaultBlack)
            ) {
                Text("APPLY", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (localFilter.isActive) {
                TextButton(
                    onClick = { localFilter = AssetFilter() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Clear Filters", color = VaultSilver, fontSize = 12.sp)
                }
            }

            Text(
                if (localFilter.isActive || currentFilter.isActive) {
                    "Showing $filteredCount of $totalCount assets"
                } else {
                    "$totalCount assets"
                },
                color = VaultSilver,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    SectionTitle("QUICK FILTERS")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = localFilter.showUncategorized,
                            onClick = { localFilter = localFilter.copy(showUncategorized = !localFilter.showUncategorized) },
                            label = { Text("Uncategorized", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VaultSurface,
                                selectedLabelColor = VaultWhite
                            )
                        )
                        FilterChip(
                            selected = localFilter.showUncollected,
                            onClick = { localFilter = localFilter.copy(showUncollected = !localFilter.showUncollected) },
                            label = { Text("Uncollected", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VaultSurface,
                                selectedLabelColor = VaultWhite
                            )
                        )
                        FilterChip(
                            selected = localFilter.showUntagged,
                            onClick = { localFilter = localFilter.copy(showUntagged = !localFilter.showUntagged) },
                            label = { Text("Untagged", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VaultSurface,
                                selectedLabelColor = VaultWhite
                            )
                        )
                    }
                }

                item {
                    SectionTitle("CATEGORY")
                    Spacer(modifier = Modifier.height(8.dp))
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                        ) {
                            Text(
                                localFilter.category ?: "All Categories",
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.FilterList, null, tint = VaultSilver)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(VaultBlack).border(1.dp, VaultOutline)
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Categories", color = VaultWhite, fontSize = 13.sp) },
                                onClick = {
                                    localFilter = localFilter.copy(category = null)
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (localFilter.category == null) {
                                        Icon(Icons.Default.Check, null, tint = VaultWhite)
                                    }
                                }
                            )
                            Divider(color = VaultOutline)
                            allCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = VaultWhite, fontSize = 13.sp) },
                                    onClick = {
                                        localFilter = localFilter.copy(category = cat)
                                        expanded = false
                                    },
                                    leadingIcon = {
                                        if (localFilter.category == cat) {
                                            Icon(Icons.Default.Check, null, tint = VaultWhite)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    SectionTitle("COLLECTIONS")
                    Spacer(modifier = Modifier.height(8.dp))
                    MultiSelectDropdown(
                        options = allCollections,
                        selectedOptions = localFilter.collections,
                        onSelectionChanged = { localFilter = localFilter.copy(collections = it) },
                        placeholder = "All Collections"
                    )
                }

                item {
                    SectionTitle("TAGS")
                    Spacer(modifier = Modifier.height(8.dp))
                    MultiSelectDropdown(
                        options = allTags,
                        selectedOptions = localFilter.tags,
                        onSelectionChanged = { localFilter = localFilter.copy(tags = it) },
                        placeholder = "All Tags"
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun MultiSelectDropdown(
    options: List<String>,
    selectedOptions: Set<String>,
    onSelectionChanged: (Set<String>) -> Unit,
    placeholder: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
        ) {
            Text(
                if (selectedOptions.isEmpty()) {
                    placeholder
                } else {
                    "${selectedOptions.size} selected"
                },
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.FilterList, null, tint = VaultSilver)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(VaultBlack)
                .border(1.dp, VaultOutline)
                .heightIn(max = 300.dp)
        ) {
            if (selectedOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable {
                            onSelectionChanged(emptySet())
                        }
                ) {
                    Text("Clear", color = VaultSilver, fontSize = 12.sp)
                }
                Divider(color = VaultOutline)
            }
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectionChanged(
                                if (option in selectedOptions) {
                                    selectedOptions - option
                                } else {
                                    selectedOptions + option
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = option in selectedOptions,
                        onCheckedChange = {
                            onSelectionChanged(
                                if (it) selectedOptions + option else selectedOptions - option
                            )
                        },
                        colors = CheckboxDefaults.colors(checkedColor = VaultWhite)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(option, color = VaultWhite, fontSize = 13.sp)
                }
            }
        }
    }
}
