package com.example.thenobbery.logic

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File

object FileClassifier {

    enum class AssetSlot {
        SOURCE_PNTR, VECTOR_SVG, IMAGE_TRANSPARENT, IMAGE_BACKGROUND, UNKNOWN
    }

    fun classify(context: Context, file: File): AssetSlot {
        val extension = file.extension.lowercase()

        return when (extension) {
            "pntr" -> AssetSlot.SOURCE_PNTR
            "svg" -> AssetSlot.VECTOR_SVG
            "png" -> identifyPngType(file)
            else -> AssetSlot.UNKNOWN
        }
    }

    fun identifyPngType(file: File): AssetSlot {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = 4 
        }
        
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, options) ?: return AssetSlot.UNKNOWN
        
        // 10-point Cross-Pattern Check (Center, 4 corners, 4 edges, and 1/4 marks)
        val w = bitmap.width
        val h = bitmap.height
        val checkPoints = listOf(
            Pair(w / 2, h / 2),     // Center
            Pair(5, 5),             // Top Left
            Pair(w - 5, 5),         // Top Right
            Pair(5, h - 5),         // Bottom Left
            Pair(w - 5, h - 5),     // Bottom Right
            Pair(w / 4, h / 2),     // Left Mid
            Pair(3 * w / 4, h / 2), // Right Mid
            Pair(w / 2, h / 4),     // Top Mid
            Pair(w / 2, 3 * h / 4), // Bottom Mid
            Pair(w / 3, h / 3)      // Offset center
        )

        var hasTransparency = false
        for (point in checkPoints) {
            // Safety check to stay within bounds
            val x = point.first.coerceIn(0, w - 1)
            val y = point.second.coerceIn(0, h - 1)
            
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel shr 24) and 0xff
            if (alpha < 250) { // Slight tolerance for "almost" transparent pixels
                hasTransparency = true
                break
            }
        }
        
        bitmap.recycle()
        return if (hasTransparency) AssetSlot.IMAGE_TRANSPARENT else AssetSlot.IMAGE_BACKGROUND
    }
}
