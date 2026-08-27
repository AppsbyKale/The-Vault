package com.example.thenobbery.logic

import android.content.Context
import android.content.SharedPreferences

object OrganizationPreferenceManager {
    private const val PREFS_NAME = "organization_prefs"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_COLLECTIONS = "collections"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCategories(context: Context): List<String> {
        val prefs = getPrefs(context)
        val categories = prefs.getStringSet(KEY_CATEGORIES, null)
        return if (categories != null) {
            categories.toList().sorted()
        } else {
            AssetDefinitions.DefaultCategories
        }
    }

    fun saveCategories(context: Context, categories: List<String>) {
        val prefs = getPrefs(context)
        prefs.edit().putStringSet(KEY_CATEGORIES, categories.toSet()).apply()
    }

    fun getCollections(context: Context): List<String> {
        val prefs = getPrefs(context)
        val collections = prefs.getStringSet(KEY_COLLECTIONS, null)
        return if (collections != null) {
            collections.toList().sorted()
        } else {
            AssetDefinitions.DefaultCollections
        }
    }

    fun saveCollections(context: Context, collections: List<String>) {
        val prefs = getPrefs(context)
        prefs.edit().putStringSet(KEY_COLLECTIONS, collections.toSet()).apply()
    }

    fun getTags(context: Context): Set<String> {
        return AssetDefinitions.stringToTags(
            getPrefs(context).getString("tags", "") ?: ""
        ).toSet()
    }

    fun saveTags(context: Context, tags: Set<String>) {
        val prefs = getPrefs(context)
        prefs.edit().putString("tags", AssetDefinitions.tagsToString(tags.toList())).apply()
    }
}
