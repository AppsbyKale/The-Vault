package com.example.thenobbery.logic

import java.util.Locale

/**
 * Utility to clean up filenames for the gallery display.
 * Strips extensions, IP IDs, and workflow suffixes.
 */
object FileNameSanitizer {

    fun sanitizeTitle(rawName: String): String {
        var clean = rawName
            .substringBeforeLast(".") // Remove extension
            // 1. Remove IP ID strings in parentheses: (457224563)
            .replace(Regex("\\(\\d+\\)"), "") 
            // 2. Remove workflow suffixes like _transparent, _bg, _background, v1, etc.
            .replace(Regex("(?i)_?transparent|_?background|_?bg|_?v\\d+|_?copy|_?final"), "")
            // 3. Replace underscores/dashes with spaces
            .replace(Regex("[_\\-]"), " ")
            .trim()

        // 4. Fix for errant periods left over before/after the ID or at the end
        clean = clean.replace(Regex("\\.+$"), "").trim()

        // Capitalize for a clean gallery look (e.g., "batman" -> "Batman")
        return clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            word.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
        }.replace(Regex("\\s+"), " ") 
    }
}
