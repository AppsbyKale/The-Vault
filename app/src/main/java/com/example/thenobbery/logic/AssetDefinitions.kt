package com.example.thenobbery.logic

/**
 * The "Ground Truth" for asset organization.
 * Modify these lists to add or remove global options.
 */

object AssetDefinitions {

    // renamed from Categories to DefaultCategories for consistency
    val DefaultCategories = listOf(
        "NOBS", "BOB", "COBS", "MOBS", "PATTERN", "TEMPLATE", "OTHER", "UNASSIGNED"
    )

    val DefaultCollections = listOf("COMMISSIONS", "PERSONAL WORK", "SKETCHBOOK")

    fun stringToCollections(collString: String): List<String> {
        if (collString.isBlank()) return emptyList()
        return collString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun collectionsToString(collections: List<String>): String {
        return collections.joinToString(",")
    }

    // Enum for sorting logic
    enum class SortOrder {
        NEWEST,
        ALPHABETICAL,
        CLASSIFICATION,
        MISSING_FILES
    }

    fun stringToTags(tagString: String): List<String> {
        if (tagString.isBlank()) return emptyList()
        return tagString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun tagsToString(tags: List<String>): String {
        return tags.joinToString(",")
    }
}
