package com.example.thenobbery.ui.screens.profiledialog

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.theme.*
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetProfileDialog(
    pagerState: PagerState,
    assets: List<Asset>,
    onDismiss: () -> Unit,
    onUpdateAsset: (Asset) -> Unit,
    onUnlink: (Asset) -> Unit,
    onLinkToParent: (Asset, Asset) -> Unit,
    onDeleteFile: (Asset, com.example.thenobbery.ui.screens.profiledialog.FileType) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        // GEOMETRIC FLOATING CONTAINER (92% width, 85% height)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
            color = VaultBlack,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = true
                ) { page ->
                    AssetProfileContent(
                        asset = assets[page],
                        allAssets = assets,
                        onUpdateAsset = onUpdateAsset,
                        onUnlink = onUnlink,
                        onLinkToParent = onLinkToParent,
                        onDismiss = onDismiss,
                        onDeleteFile = onDeleteFile
                    )
                }
            }
        }
    }
}

@Composable
fun AssetProfileContent(
    asset: Asset,
    allAssets: List<Asset>,
    onUpdateAsset: (Asset) -> Unit,
    onUnlink: (Asset) -> Unit,
    onLinkToParent: (Asset, Asset) -> Unit,
    onDismiss: () -> Unit,
    onDeleteFile: (Asset, FileType) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFullScreen by remember { mutableStateOf(false) }
    val tabs = listOf("PROFILE", "ORG", "LINKS", "LORE", "FILES")

    if (showFullScreen) {
        FullScreenPreviewDialog(
            asset = asset,
            onDismiss = { showFullScreen = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // --- HERO HEADER (Static on all tabs) ---
        HeroHeader(asset, onDismiss, onUpdateAsset, onFullScreen = { showFullScreen = true })
        
        // --- CONTENT AREA ---
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> Box(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    ProfileTabContent(asset)
                }
                1 -> Box(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    OrganizationTabContent(asset, allAssets, onUpdateAsset)
                }
                2 -> Box(Modifier.padding(16.dp)) {
                    LinksTab(asset, allAssets, onUnlink, onLinkToParent)
                }
                3 -> Box(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                    LoreTabContent(asset, onUpdateAsset)
                }
                4 -> Box(Modifier.padding(16.dp)) {
                    FilesTabContent(asset, onUpdateAsset, onDeleteFile)
                }
            }
        }

        // --- BOTTOM NAVIGATION (Vault Tab Bar) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(VaultSurface)
                .border(width = 1.dp, color = VaultOutline)
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedTab = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Box(Modifier.width(20.dp).height(2.dp).background(VaultWhite))
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        title,
                        color = if (isSelected) VaultWhite else VaultSilver,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HeroHeader(asset: Asset, onDismiss: () -> Unit, onUpdateAsset: (Asset) -> Unit, onFullScreen: () -> Unit) {
    var titleText by remember(asset.uid) { mutableStateOf(asset.title) }
    var isFocused by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = { 
                    if (!isFocused) onFullScreen() 
                }
            )
        }
    ) {
        val displayPath = asset.mainPreviewPath ?: asset.primaryBackgroundPngPath ?: asset.primaryTransparentPngPath
        AsyncImage(
            model = displayPath?.let { File(it) },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Industrial Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, VaultBlack),
                        startY = 350f
                    )
                )
        )
        // Close Button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = VaultWhite)
        }
        
        // Anchored Labels
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Text(asset.classification.uppercase(), color = VaultSilver, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = titleText,
                onValueChange = { newValue ->
                    titleText = newValue
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = VaultWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(VaultWhite),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (isFocused && !focusState.isFocused) {
                            // Focus lost - save changes
                            val finalTitle = titleText.trim()
                            if (finalTitle.isNotEmpty() && finalTitle != asset.title) {
                                onUpdateAsset(asset.copy(title = finalTitle))
                            } else if (finalTitle.isEmpty()) {
                                titleText = asset.title
                            }
                        }
                        isFocused = focusState.isFocused
                    }
            )
        }
    }
}

@Composable
fun LoreTabContent(asset: Asset, onUpdateAsset: (Asset) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("LORE & BACKSTORY", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = asset.lore,
            onValueChange = { onUpdateAsset(asset.copy(lore = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = VaultWhite, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VaultBlack,
                unfocusedContainerColor = VaultBlack,
                focusedBorderColor = VaultWhite,
                unfocusedBorderColor = VaultSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("NOTES", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = asset.notes,
            onValueChange = { onUpdateAsset(asset.copy(notes = it)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
            textStyle = androidx.compose.ui.text.TextStyle(color = VaultWhite, fontSize = 14.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VaultBlack,
                unfocusedContainerColor = VaultBlack,
                focusedBorderColor = VaultWhite,
                unfocusedBorderColor = VaultSurface
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}