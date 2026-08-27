package com.example.thenobbery.ui.screens.menus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.thenobbery.data.Asset
import com.example.thenobbery.ui.theme.*

@Composable
fun VaultActionMenu(
    asset: Asset,
    onDismiss: () -> Unit,
    onProfile: (Asset) -> Unit,
    onLink: (Asset) -> Unit,
    onMerge: (Asset) -> Unit,
    onExport: (Asset) -> Unit,
    onConvertToSvg: (Asset) -> Unit,
    onDelete: (Asset) -> Unit
) {
    val isChild = asset.isChild()
    val linkLabel = if (isChild) "REMOVE LINK" else "LINK"
    val linkColor = if (isChild) Color(0xFFE57373) else VaultSilver

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = VaultSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = asset.title.uppercase(),
                    color = VaultWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )
                
                Text(
                    text = asset.classification.uppercase(),
                    color = VaultSilver.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                MenuOption("EDIT ASSET", VaultSilver) { 
                    onProfile(asset)
                    onDismiss() 
                }
                
                MenuOption(linkLabel, linkColor) { 
                    onLink(asset)
                    onDismiss() 
                }
                
                MenuOption("MERGE ASSETS", VaultSilver) { 
                    onMerge(asset)
                    onDismiss() 
                }
                
                MenuOption("EXPORT / SHARE", VaultWhite) { 
                    onExport(asset)
                    onDismiss() 
                }

                MenuOption("CONVERT TO SVG", VaultSilver) { 
                    onConvertToSvg(asset)
                    onDismiss() 
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                MenuOption("DELETE", Color(0xFFE57373)) { 
                    onDelete(asset)
                    onDismiss() 
                }
            }
        }
    }
}

@Composable
private fun MenuOption(
    label: String,
    textColor: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
