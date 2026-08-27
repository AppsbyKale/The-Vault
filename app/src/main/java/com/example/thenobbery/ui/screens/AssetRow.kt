package com.example.thenobbery.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.ui.theme.*
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssetRow(
    asset: Asset,
    allAssets: List<Asset> = emptyList(),
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isMultiselectMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {}
) {
    val isLinked = remember(asset, allAssets) {
        asset.sourceLinkId != 0L || allAssets.any { it.sourceLinkId == asset.uid }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(if (isSelected) VaultSurface else VaultBlack)
            .combinedClickable(
                onClick = { 
                    if (isMultiselectMode) {
                        onSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isMultiselectMode) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) AccentMaster else VaultSilver,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        val previewPath = asset.mainPreviewPath ?: asset.primaryBackgroundPngPath ?: asset.primaryTransparentPngPath
        AsyncImage(
            model = previewPath?.let { File(it) },
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(VaultSurface),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = asset.title.uppercase(),
                color = VaultWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (asset.classification != "UNASSIGNED") {
                Text(
                    text = asset.classification.uppercase(),
                    color = VaultSilver,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (asset.hasBackgroundPng()) FileTypeIcon("B", MutedRed, false)
            if (asset.hasTransparentPng()) FileTypeIcon("T", VaultSilver, false)
            if (asset.hasPntr()) FileTypeIcon("IP", MutedViolet, asset.isParent())
            if (asset.hasSvg()) FileTypeIcon("S", MutedBlue, false)
            if (isLinked) {
                FileTypeIcon("L", VaultParchment, false)
            }
        }
    }
}

@Composable
fun FileTypeIcon(letter: String, color: Color, isSource: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(
                width = if (isSource) 1.5.dp else 1.dp,
                color = if (isSource) VaultWhite else color.copy(alpha = 0.5f),
                shape = RoundedCornerShape(4.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = if (isSource) VaultWhite else color,
            fontSize = 12.sp,
            fontWeight = if (isSource) FontWeight.Bold else FontWeight.Normal
        )
    }
}
