package com.example.thenobbery.ui.screens.profiledialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thenobbery.data.Asset
import com.example.thenobbery.logic.AssetDefinitions
import com.example.thenobbery.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileTabContent(asset: Asset) {
    Column(modifier = Modifier.fillMaxWidth()) {
        
        // --- COLLECTION SECTION ---
        Text("COLLECTIONS", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val collectionsList = AssetDefinitions.stringToCollections(asset.collections)
        if (collectionsList.isEmpty()) {
            Text("No collections", color = VaultSilver.copy(alpha = 0.5f), fontSize = 14.sp)
        } else {
            Text(
                text = collectionsList.joinToString(", ").uppercase(),
                color = VaultWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- TAGS SECTION ---
        Text("TAGS", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        // Horizontal list of tags
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tags = AssetDefinitions.stringToTags(asset.tags)
            if (tags.isEmpty()) {
                Text("No tags added", color = VaultSilver.copy(alpha = 0.5f), fontSize = 12.sp)
            } else {
                tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(VaultSurface, RoundedCornerShape(4.dp))
                            .border(1.dp, VaultOutline, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(tag.uppercase(), color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // --- DATE ADDED SECTION ---
        Text("DATE ADDED", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(asset.timestamp))
        Text(
            text = dateString,
            color = VaultWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- FILE TYPE INDICATORS ---
        Text("FILE ASSETS", color = VaultSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (asset.hasBackgroundPng()) QuickIndicator("BG (${asset.backgroundPngPaths.size})", AccentBack)
            if (asset.hasTransparentPng()) QuickIndicator("PNG (${asset.transparentPngPaths.size})", VaultSilver)
            if (asset.hasPntr()) QuickIndicator("IP (${asset.pntrPaths.size})", MutedViolet)
            if (asset.hasSvg()) QuickIndicator("SVG (${asset.svgPaths.size})", MutedBlue)
        }
    }
}

@Composable
fun QuickIndicator(label: String, color: Color) {
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
