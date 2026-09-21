package com.example.thenobbery.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.thenobbery.ui.theme.*

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    onExportToDownloads: () -> Unit,
    onExportToLocation: () -> Unit,
    onImport: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.dp, VaultOutline, RoundedCornerShape(8.dp)),
            color = VaultBlack,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(VaultSurface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "BACKUP / RESTORE",
                        color = VaultWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = VaultSilver)
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "EXPORT",
                        color = VaultSilver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onExportToDownloads()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                    ) {
                        Icon(Icons.Default.Download, null, tint = VaultWhite)
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Save to Downloads", fontSize = 14.sp)
                            Text("Quick backup to default location", fontSize = 11.sp, color = VaultSilver)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onExportToLocation()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                    ) {
                        Icon(Icons.Default.Save, null, tint = VaultWhite)
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Save to...", fontSize = 14.sp)
                            Text("Choose your own save location", fontSize = 11.sp, color = VaultSilver)
                        }
                    }

                    Divider(color = VaultOutline, modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        "IMPORT",
                        color = VaultSilver,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onImport()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultWhite)
                    ) {
                        Icon(Icons.Default.Restore, null, tint = VaultWhite)
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Restore from Backup", fontSize = 14.sp)
                            Text("Import a backup file", fontSize = 11.sp, color = VaultSilver)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
