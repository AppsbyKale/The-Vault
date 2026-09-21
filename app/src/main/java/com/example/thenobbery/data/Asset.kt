package com.example.thenobbery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) 
    val uid: Long = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // The ID of the Parent asset that holds the .pntr file
    var sourceLinkId: Long = 0,
    
    // File paths per asset. Stored as lists to support multiple files of each type.
    val pntrPaths: List<String> = emptyList(),
    val svgPaths: List<String> = emptyList(),
    val transparentPngPaths: List<String> = emptyList(),
    val backgroundPngPaths: List<String> = emptyList(),
    
    // Preview selection for the Library list
    val mainPreviewPath: String? = null,
    
    val tags: String = "",
    val collections: String = "",
    val lore: String = "",
    val notes: String = "", 
    val classification: String = "UNASSIGNED"
) {
    /**
     * An asset is a Parent if it owns its own UID as the sourceLinkId.
     */
    fun isParent(): Boolean = uid != 0L && uid == sourceLinkId

    /**
     * An asset is a Child if it points to a different UID.
     */
    fun isChild(): Boolean = sourceLinkId != 0L && sourceLinkId != uid

    /**
     * An asset is Standalone if it has no link association.
     */
    fun isStandalone(): Boolean = sourceLinkId == 0L

    /**
     * Returns true if the asset is part of a lineage (either Parent or Child).
     * This drives the "L" indicator in the AssetRow.
     */
    fun isLinked(): Boolean = sourceLinkId != 0L
    
    fun hasPntr(): Boolean = pntrPaths.isNotEmpty()

    fun hasSvg(): Boolean = svgPaths.isNotEmpty()

    fun hasTransparentPng(): Boolean = transparentPngPaths.isNotEmpty()

    fun hasBackgroundPng(): Boolean = backgroundPngPaths.isNotEmpty()

    val primaryPntrPath: String? get() = pntrPaths.firstOrNull()
    val primarySvgPath: String? get() = svgPaths.firstOrNull()
    val primaryTransparentPngPath: String? get() = transparentPngPaths.firstOrNull()
    val primaryBackgroundPngPath: String? get() = backgroundPngPaths.firstOrNull()
}
