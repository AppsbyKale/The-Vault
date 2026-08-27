package com.example.thenobbery

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import com.example.thenobbery.ui.screens.LibraryScreen
import com.example.thenobbery.viewmodels.VaultViewModel
import com.example.thenobbery.ui.theme.NobberyVaultTheme
import com.example.thenobbery.logic.FileExportUtils
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.screens.EditMode

class MainActivity : ComponentActivity() {
    
    private val viewModel: VaultViewModel by viewModels()

    private var exportQueue = mutableListOf<Pair<String, String>>() // (sourcePath, fileName)
    private var isProcessingExport = false
    private var pendingImportUris: List<Uri>? = null

    private val pickFilesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle incoming shared files
        handleIncomingIntent(intent)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
        
        setContent {
            NobberyVaultTheme {
                val assets by viewModel.assets.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val sortOrder by viewModel.sortOrder.collectAsState()
                val filter by viewModel.filter.collectAsState()
                val allCategories by viewModel.allCategories.collectAsState()
                val allCollections by viewModel.allCollections.collectAsState()
                val allTags by viewModel.allTags.collectAsState()

                LibraryScreen(
                    assets = assets,
                    searchQuery = searchQuery,
                    sortOrder = sortOrder,
                    filter = filter,
                    allCategories = allCategories,
                    allCollections = allCollections,
                    allTags = allTags,
                    isImporting = false,
                    importProgressParam = 0f,
                    
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onSortOrderChanged = { viewModel.onSortOrderChanged(it) },
                    onFilterChanged = { viewModel.onFilterChanged(it) },
                    
                    onBulkImportRequested = { pickFilesLauncher.launch("*/*") },
                    
                    onUpdateAsset = { viewModel.updateAsset(it) },
                    onDeleteAsset = { viewModel.deleteAssetWithHandshake(it) },
                    
                    onUnlinkAsset = { childId -> viewModel.unlinkAsset(childId) },
                    
                    onLinkAssets = { childId, parentId -> 
                        viewModel.linkAssets(childId, parentId) 
                    },
       
                    onMergeAssets = { source: Asset, target: Asset -> 
                        viewModel.mergeAssets(source, target) 
                    },

                    onBatchUpdateAssets = { assets, tagsMode, tagsToAdd, tagsToRemove, collectionsMode, collectionsToAdd, collectionsToRemove, categoryMode, category ->
                        viewModel.batchUpdateAssets(assets, tagsMode, tagsToAdd, tagsToRemove, collectionsMode, collectionsToAdd, collectionsToRemove, categoryMode, category)
                    },

                    onDeleteFile = { asset, fileType ->
                        viewModel.deleteFileAsset(asset, fileType)
                    },
                    onRefreshOrganization = { viewModel.refreshOrganization() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIncomingIntent(it) }
    }

    private fun handleIncomingIntent(intent: Intent) {
        val action = intent.action
        val uris = mutableListOf<Uri>()
        
        when (action) {
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { uri ->
                        uris.add(uri)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                        uris.add(uri)
                    }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let { uriList ->
                        uris.addAll(uriList)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uriList ->
                        uris.addAll(uriList)
                    }
                }
            }
        }
        
        if (uris.isNotEmpty()) {
            pendingImportUris = uris
            viewModel.importUris(uris)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == 1001) {
            if (resultCode == Activity.RESULT_OK && data?.data != null) {
                val (sourcePath, _) = exportQueue.removeAt(0)
                FileExportUtils.writeFileToUri(this, sourcePath, data.data!!)
            } else {
                exportQueue.clear()
            }
            processNextExport()
        }
    }

    fun enqueueExport(items: List<Pair<String, String>>) {
        exportQueue.addAll(items)
        if (!isProcessingExport) {
            processNextExport()
        }
    }

    private fun processNextExport() {
        if (exportQueue.isEmpty()) {
            isProcessingExport = false
            return
        }
        isProcessingExport = true
        val (sourcePath, fileName) = exportQueue.first()
        val mimeType = when {
            fileName.endsWith(".zip") -> "application/zip"
            fileName.endsWith(".pntr") -> "application/octet-stream"
            else -> "image/png"
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        startActivityForResult(intent, 1001)
    }
}
