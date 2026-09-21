package com.example.thenobbery.logic

import android.content.Context
import android.graphics.*
import androidx.compose.ui.unit.IntOffset
import com.example.thenobbery.data.Asset
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

object BitmapUtils {

    fun applyWatermark(
        context: Context,
        asset: Asset,
        fileType: String,
        watermarkPath: String,
        opacity: Float,
        scaleFactor: Float,
        uiOffset: IntOffset,
        containerSize: androidx.compose.ui.unit.IntSize,
        imageSize: androidx.compose.ui.unit.IntSize,
        isWatermarked: Boolean = true
    ): String? {
        try {
            val originalPath = when (fileType) {
                "transparent" -> asset.primaryTransparentPngPath
                "pntr" -> asset.primaryPntrPath
                else -> asset.primaryBackgroundPngPath
            } ?: asset.mainPreviewPath

            if (originalPath == null) {
                Timber.e("originalPath is null")
                return null
            }

            // Decode image dimensions
            val sizeOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(originalPath, sizeOptions)
            val actualImageWidth = sizeOptions.outWidth.toFloat()
            val actualImageHeight = sizeOptions.outHeight.toFloat()

            if (actualImageWidth <= 0 || actualImageHeight <= 0) {
                Timber.e("Invalid image dimensions: ${sizeOptions.outWidth}x${sizeOptions.outHeight}")
                return null
            }

            if (containerSize.width <= 0 || containerSize.height <= 0) {
                Timber.e("Invalid container size: ${containerSize.width}x${containerSize.height}")
                return null
            }

            val mutableBitmap = BitmapFactory.decodeFile(originalPath)?.copy(Bitmap.Config.ARGB_8888, true)
            if (mutableBitmap == null) {
                Timber.e("Failed to decode or copy bitmap")
                return null
            }
            val watermarkBitmap = BitmapFactory.decodeFile(watermarkPath)
            if (watermarkBitmap == null) {
                Timber.e("Failed to decode watermark")
                return null
            }

            // Calculate how image is displayed with ContentScale.Fit
            val containerAspect = containerSize.width.toFloat() / containerSize.height.toFloat()
            val imageAspect = actualImageWidth / actualImageHeight

            val displayedWidth: Float
            val displayedHeight: Float

            if (imageAspect > containerAspect) {
                displayedWidth = containerSize.width.toFloat()
                displayedHeight = displayedWidth / imageAspect
            } else {
                displayedHeight = containerSize.height.toFloat()
                displayedWidth = displayedHeight * imageAspect
            }

            // Scale offset from container space to bitmap space
            val scaleX = actualImageWidth / displayedWidth
            val scaleY = actualImageHeight / displayedHeight

            // Position watermark: center + scaled offset
            val posX = (actualImageWidth / 2f) + (uiOffset.x * scaleX) - (watermarkBitmap.width * scaleFactor / 2f)
            val posY = (actualImageHeight / 2f) + (uiOffset.y * scaleY) - (watermarkBitmap.height * scaleFactor / 2f)

            val finalWmWidth = (watermarkBitmap.width * scaleFactor).toInt()
            val finalWmHeight = (watermarkBitmap.height * scaleFactor).toInt()
            val scaledWatermark = Bitmap.createScaledBitmap(watermarkBitmap, finalWmWidth, finalWmHeight, true)

            val canvas = Canvas(mutableBitmap)
            val paint = Paint().apply {
                alpha = (opacity * 255).toInt()
                isAntiAlias = true
            }
            canvas.drawBitmap(scaledWatermark, posX, posY, paint)

            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val safeTitle = asset.title.replace(Regex("[^a-zA-Z0-9\\-]"), "_")
            val typeStr = when (fileType) {
                "transparent" -> "Transparent"
                "pntr" -> "Pntr"
                else -> "Background"
            }
            val suffix = if (isWatermarked) "-Watermarked" else ""
            val fileName = "${safeTitle}-${typeStr}${suffix}.png"
            val exportFile = File(exportDir, fileName)

            FileOutputStream(exportFile).use { out ->
                mutableBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            Timber.d("Success: ${exportFile.absolutePath}")
            return exportFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply watermark")
            return null
        }
    }
}
