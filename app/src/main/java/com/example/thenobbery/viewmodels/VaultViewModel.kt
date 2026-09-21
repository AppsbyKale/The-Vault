package com.example.thenobbery.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.thenobbery.data.VaultDatabase
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.logic.BulkImportWorker
import com.example.thenobbery.logic.OrganizationPreferenceManager
import com.example.thenobbery.logic.UriListPersistence
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssetFilter(
    val category: String? = null,
    val collections: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val showUncategorized: Boolean = false,
    val showUncollected: Boolean = false,
    val showUntagged: Boolean = false
) {
    val isActive: Boolean
        get() = category != null || collections.isNotEmpty() || tags.isNotEmpty() || 
                showUncategorized || showUncollected || showUntagged
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = VaultDatabase.getDatabase(application).assetDao()
    private val workManager = WorkManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(AssetDefinitions.SortOrder.NEWEST)
    val sortOrder: StateFlow<AssetDefinitions.SortOrder> = _sortOrder.asStateFlow()

    private val _filter = MutableStateFlow(AssetFilter())
    val filter: StateFlow<AssetFilter> = _filter.asStateFlow()

    private val _heirSelectionRequired = MutableStateFlow<Asset?>(null)
    val heirSelectionRequired: StateFlow<Asset?> = _heirSelectionRequired.asStateFlow()

    private val allAssetsFlow = dao.getAllAssets()

    val assets: StateFlow<List<Asset>> = combine(
        allAssetsFlow,
        _searchQuery,
        _sortOrder,
        _filter
    ) { list, query, sort, filter ->
        var filteredList = if (query.isBlank()) {
            list
        } else {
            list.filter { asset ->
                asset.title.contains(query, ignoreCase = true) ||
                asset.tags.contains(query, ignoreCase = true) ||
                asset.classification.contains(query, ignoreCase = true) ||
                asset.collections.contains(query, ignoreCase = true)
            }
        }

        if (filter.isActive) {
            filteredList = filteredList.filter { asset ->
                val assetCollections = AssetDefinitions.stringToCollections(asset.collections)
                val assetTags = AssetDefinitions.stringToTags(asset.tags)

                val categoryMatch = filter.category == null || asset.classification == filter.category
                val collectionsMatch = filter.collections.isEmpty() || assetCollections.any { it in filter.collections }
                val tagsMatch = filter.tags.isEmpty() || assetTags.any { it in filter.tags }
                val uncategorizedMatch = !filter.showUncategorized || asset.classification == "UNASSIGNED"
                val uncollectedMatch = !filter.showUncollected || assetCollections.isEmpty()
                val untaggedMatch = !filter.showUntagged || assetTags.isEmpty()

                categoryMatch && collectionsMatch && tagsMatch && uncategorizedMatch && uncollectedMatch && untaggedMatch
            }
        }

        fun countMissingFiles(asset: Asset): Int {
            var missing = 0
            if (asset.pntrPaths.isEmpty()) missing += 4
            if (asset.svgPaths.isEmpty()) missing += 2
            if (asset.transparentPngPaths.isEmpty()) missing += 1
            if (asset.backgroundPngPaths.isEmpty()) missing += 1
            return missing
        }

        when (sort) {
            AssetDefinitions.SortOrder.NEWEST -> filteredList.sortedByDescending { it.timestamp }
            AssetDefinitions.SortOrder.ALPHABETICAL -> filteredList.sortedBy { it.title.lowercase() }
            AssetDefinitions.SortOrder.CLASSIFICATION -> filteredList.sortedBy { it.classification }
            AssetDefinitions.SortOrder.MISSING_FILES -> filteredList.sortedWith(
                compareByDescending<Asset> { countMissingFiles(it) }
                    .thenByDescending { it.timestamp }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _categories = MutableStateFlow(OrganizationPreferenceManager.getCategories(getApplication()))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _collections = MutableStateFlow(OrganizationPreferenceManager.getCollections(getApplication()))
    val collections: StateFlow<List<String>> = _collections.asStateFlow()

    private val _tags = MutableStateFlow(OrganizationPreferenceManager.getTags(getApplication()))
    val tags: StateFlow<Set<String>> = _tags.asStateFlow()

    val allCategories: StateFlow<List<String>> = allAssetsFlow.map { list ->
        (_categories.value + list.map { it.classification }).distinct().filter { it.isNotBlank() }.sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCollections: StateFlow<List<String>> = allAssetsFlow.map { list ->
        (_collections.value + list.flatMap { AssetDefinitions.stringToCollections(it.collections) }).distinct().filter { it.isNotBlank() }.sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<String>> = combine(allAssetsFlow, _tags) { list, tagsSet ->
        (tagsSet + list.flatMap { AssetDefinitions.stringToTags(it.tags) }).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshOrganization() {
        _categories.value = OrganizationPreferenceManager.getCategories(getApplication())
        _collections.value = OrganizationPreferenceManager.getCollections(getApplication())
        _tags.value = OrganizationPreferenceManager.getTags(getApplication())
    }

    /**
     * Safety Handshake: Initiates deletion. 
     * If Parent, triggers heir selection. If Child or Standalone, deletes normally.
     */
    fun deleteAssetWithHandshake(asset: Asset) {
        viewModelScope.launch {
            if (asset.isParent()) {
                val children = dao.getChildrenForParent(asset.uid)
                if (children.isNotEmpty()) {
                    // Trigger UI dialog to pick a new parent for orphans
                    _heirSelectionRequired.value = asset
                } else {
                    dao.deleteAsset(asset)
                }
            } else {
                dao.deleteAsset(asset)
            }
        }
    }

    /**
     * Completes the Handshake: Transfers the .pntr source to a new parent
     * and updates all children to point to the new heir.
     */
    fun transferOwnershipAndCompleteDelete(oldParent: Asset, newHeir: Asset) {
        viewModelScope.launch {
            // 1. Move the pntrPaths to the new heir
            val updatedHeir = newHeir.copy(
                pntrPaths = oldParent.pntrPaths,
                sourceLinkId = newHeir.uid // Ensure it becomes a Parent
            )
            dao.updateAsset(updatedHeir)

            // 2. Re-point all other children to the new heir using DAO query
            dao.reLinkChildren(oldParent.uid, newHeir.uid)

            // 3. Finally delete the old parent
            dao.deleteAsset(oldParent)
            _heirSelectionRequired.value = null
        }
    }

    /**
     * Links a child to a parent using IDs.
     */
    fun linkAssets(childId: Long, parentId: Long) {
        viewModelScope.launch {
            dao.linkToParent(childId, parentId)
        }
    }

    /**
     * Unlinks a child asset to make it standalone.
     */
    fun unlinkAsset(childId: Long) {
        viewModelScope.launch {
            dao.unlinkChild(childId)
        }
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch { dao.updateAsset(asset) }
    }

    /**
     * Merging Logic: Combines files and tags from source into target, then deletes source.
     */
    fun mergeAssets(source: Asset, target: Asset) {
        viewModelScope.launch {
            val mergedTarget = target.copy(
                pntrPaths = (target.pntrPaths + source.pntrPaths).distinct(),
                svgPaths = (target.svgPaths + source.svgPaths).distinct(),
                transparentPngPaths = (target.transparentPngPaths + source.transparentPngPaths).distinct(),
                backgroundPngPaths = (target.backgroundPngPaths + source.backgroundPngPaths).distinct(),
                tags = (AssetDefinitions.stringToTags(target.tags) + 
                        AssetDefinitions.stringToTags(source.tags))
                        .distinct().joinToString(",")
            )
            dao.updateAsset(mergedTarget)
            dao.deleteAsset(source)
        }
    }

    fun importUris(uris: List<Uri>) {
        val app = getApplication<Application>()
        val fileName = UriListPersistence.saveUris(app, uris)
        
        val workData = workDataOf("KEY_URI_FILE" to fileName)
        val importRequest = OneTimeWorkRequestBuilder<BulkImportWorker>()
            .setInputData(workData)
            .addTag("BULK_IMPORT")
            .build()
        workManager.enqueueUniqueWork("bulk_import_job", ExistingWorkPolicy.APPEND_OR_REPLACE, importRequest)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
    
    fun onSortOrderChanged(order: AssetDefinitions.SortOrder) {
        _sortOrder.value = order
    }

    fun onFilterChanged(newFilter: AssetFilter) {
        _filter.value = newFilter
    }

    fun clearFilter() {
        _filter.value = AssetFilter()
    }

    fun batchUpdateAssets(
        assets: List<Asset>,
        tagsMode: com.example.thenobbery.ui.screens.EditMode,
        tagsToAdd: Set<String>,
        tagsToRemove: Set<String>,
        collectionsMode: com.example.thenobbery.ui.screens.EditMode,
        collectionsToAdd: Set<String>,
        collectionsToRemove: Set<String>,
        categoryMode: com.example.thenobbery.ui.screens.EditMode,
        category: String?
    ) {
        viewModelScope.launch {
            assets.forEach { asset ->
                val currentTags = AssetDefinitions.stringToTags(asset.tags).toMutableSet()
                val currentCollections = AssetDefinitions.stringToCollections(asset.collections).toMutableSet()

                val newTags = when (tagsMode) {
                    com.example.thenobbery.ui.screens.EditMode.ADD -> (currentTags + tagsToAdd - tagsToRemove).toList()
                    com.example.thenobbery.ui.screens.EditMode.REPLACE -> (tagsToAdd + tagsToRemove).toList()
                }

                val newCollections = when (collectionsMode) {
                    com.example.thenobbery.ui.screens.EditMode.ADD -> (currentCollections + collectionsToAdd - collectionsToRemove).toList()
                    com.example.thenobbery.ui.screens.EditMode.REPLACE -> (collectionsToAdd + collectionsToRemove).toList()
                }

                val newCategory = when (categoryMode) {
                    com.example.thenobbery.ui.screens.EditMode.ADD -> if (asset.classification == "UNASSIGNED") category else asset.classification
                    com.example.thenobbery.ui.screens.EditMode.REPLACE -> category ?: asset.classification
                }

                val updatedAsset = asset.copy(
                    tags = newTags.joinToString(","),
                    collections = newCollections.joinToString(","),
                    classification = newCategory ?: asset.classification
                )
                dao.updateAsset(updatedAsset)
            }
        }
    }
    
    fun cancelHeirSelection() {
        _heirSelectionRequired.value = null
    }

    fun deleteFileAsset(asset: Asset, fileType: com.example.thenobbery.ui.screens.profiledialog.FileType) {
        viewModelScope.launch {
            val updatedAsset = when (fileType) {
                com.example.thenobbery.ui.screens.profiledialog.FileType.PNTR -> asset.copy(pntrPaths = emptyList())
                com.example.thenobbery.ui.screens.profiledialog.FileType.SVG -> asset.copy(svgPaths = emptyList())
                com.example.thenobbery.ui.screens.profiledialog.FileType.TRANSPARENT -> asset.copy(transparentPngPaths = emptyList())
                com.example.thenobbery.ui.screens.profiledialog.FileType.BACKGROUND -> asset.copy(backgroundPngPaths = emptyList())
                com.example.thenobbery.ui.screens.profiledialog.FileType.OTHER -> asset.copy(mainPreviewPath = null)
            }
            dao.updateAsset(updatedAsset)
        }
    }
}
