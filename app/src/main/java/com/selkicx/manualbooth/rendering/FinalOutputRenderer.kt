package com.selkicx.manualbooth.rendering

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import com.selkicx.manualbooth.data.local.entity.PhotoHolderEntity
import com.selkicx.manualbooth.data.local.entity.TemplateEntity
import com.selkicx.manualbooth.domain.model.PhotoHolderShape
import com.selkicx.manualbooth.domain.model.PhotoMode
import java.io.File
import java.io.FileOutputStream

/**
 * Renders the Final Output image (spec section 32, pipeline order in
 * section 35): for each selected photo -> apply ORIGINAL/BLACK_AND_WHITE
 * -> center crop -> apply the holder's shape mask -> place at the
 * holder's normalized coordinates -> composite the template artwork on
 * top -> save.
 *
 * Original photo files on disk are only ever read, never modified (spec
 * rule #15). Bitmaps are decoded downsampled to each holder's actual
 * target pixel size to avoid loading full DSLR resolution unnecessarily
 * (spec section 35).
 */
class FinalOutputRenderer {

    /**
     * @param orderedPhotoPaths full-resolution file paths, in the
     *   operator's selection order; index i fills holders[i] after
     *   holders are sorted by slotNumber (spec rule #14: selection order
     *   determines holder assignment).
     */
    fun render(
        template: TemplateEntity,
        holders: List<PhotoHolderEntity>,
        orderedPhotoPaths: List<String>,
        photoMode: PhotoMode,
        outputWidthPx: Int,
        outputHeightPx: Int,
        outputFile: File
    ): File {
        require(orderedPhotoPaths.size == holders.size) {
            "Selected photo count (${orderedPhotoPaths.size}) must match holder count (${holders.size})"
        }

        val canvasBitmap = Bitmap.createBitmap(outputWidthPx, outputHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.WHITE)

        val sortedHolders = holders.sortedBy { it.slotNumber }

        sortedHolders.forEachIndexed { index, holder ->
            val photoPath = orderedPhotoPaths[index]
            val holderRect = holder.toPixelRect(outputWidthPx, outputHeightPx)
            if (holderRect.width() <= 0 || holderRect.height() <= 0) return@forEachIndexed

            val decoded = decodeDownsampledForTarget(photoPath, holderRect.width(), holderRect.height())
                ?: return@forEachIndexed

            val processed = when (photoMode) {
                PhotoMode.ORIGINAL -> decoded
                PhotoMode.BLACK_AND_WHITE -> toBlackAndWhite(decoded)
            }

            val cropped = centerCrop(processed, holderRect.width(), holderRect.height())
            val masked = applyShapeMask(cropped, holder.shape)

            canvas.drawBitmap(masked, holderRect.left.toFloat(), holderRect.top.toFloat(), null)

            if (masked !== cropped) cropped.recycle()
            if (processed !== decoded) decoded.recycle()
            masked.recycle()
        }

        drawTemplateArtwork(canvas, template.artworkPath, outputWidthPx, outputHeightPx)

        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            canvasBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        canvasBitmap.recycle()

        return outputFile
    }

    // ---- Decoding -----------------------------------------------------

    /** Decodes with an inSampleSize chosen for the target size - never loads full DSLR resolution unnecessarily. */
    private fun decodeDownsampledForTarget(path: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, targetWidth, targetHeight)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }

    private fun calculateInSampleSize(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var sampleSize = 1
        if (sourceHeight > targetHeight || sourceWidth > targetWidth) {
            val halfHeight = sourceHeight / 2
            val halfWidth = sourceWidth / 2
            while ((halfHeight / sampleSize) >= targetHeight && (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    // ---- Photo mode -----------------------------------------------------

    /** Print-friendly monochrome conversion, preserving highlight/shadow detail (spec section 11). */
    private fun toBlackAndWhite(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    // ---- Cropping ---------------------------------------------------------

    /** CENTER_CROP: scale to cover the target box, then crop the overflow evenly (spec section 28). */
    private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceRatio = source.width.toFloat() / source.height
        val targetRatio = targetWidth.toFloat() / targetHeight

        val scale = if (sourceRatio > targetRatio) {
            targetHeight.toFloat() / source.height
        } else {
            targetWidth.toFloat() / source.width
        }

        val scaledWidth = (source.width * scale).toInt().coerceAtLeast(1)
        val scaledHeight = (source.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        val x = ((scaledWidth - targetWidth) / 2).coerceAtLeast(0)
        val y = ((scaledHeight - targetHeight) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(
            scaled,
            x, y,
            targetWidth.coerceAtMost(scaledWidth - x),
            targetHeight.coerceAtMost(scaledHeight - y)
        )
        if (scaled !== cropped) scaled.recycle()
        return cropped
    }

    // ---- Masking ------------------------------------------------------

    /** Clips to the holder's shape (spec section 29). Only 3 shapes exist for V1. */
    private fun applyShapeMask(source: Bitmap, shape: PhotoHolderShape): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val path = when (shape) {
            PhotoHolderShape.RECTANGLE -> rectanglePath(source.width, source.height)
            PhotoHolderShape.HEART -> heartPath(source.width, source.height)
            PhotoHolderShape.UNIQUE -> uniquePath(source.width, source.height)
        }

        canvas.drawPath(path, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    private fun rectanglePath(width: Int, height: Int): Path = Path().apply {
        addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
    }

    /** Simple two-lobe heart, parameterized to the holder's own width/height. */
    private fun heartPath(width: Int, height: Int): Path {
        val w = width.toFloat()
        val h = height.toFloat()
        return Path().apply {
            moveTo(w / 2f, h * 0.94f)
            cubicTo(-w * 0.1f, h * 0.55f, w * 0.05f, h * 0.05f, w / 2f, h * 0.32f)
            cubicTo(w * 0.95f, h * 0.05f, w * 1.1f, h * 0.55f, w / 2f, h * 0.94f)
            close()
        }
    }

    /** One predefined organic/blob shape (spec section 26: only one Unique shape, no shape library yet). */
    private fun uniquePath(width: Int, height: Int): Path {
        val w = width.toFloat()
        val h = height.toFloat()
        return Path().apply {
            moveTo(w * 0.50f, h * 0.02f)
            cubicTo(w * 0.80f, h * 0.00f, w * 1.00f, h * 0.22f, w * 0.97f, h * 0.45f)
            cubicTo(w * 1.02f, h * 0.68f, w * 0.85f, h * 0.92f, w * 0.58f, h * 0.98f)
            cubicTo(w * 0.32f, h * 1.04f, w * 0.05f, h * 0.90f, w * 0.02f, h * 0.62f)
            cubicTo(-w * 0.02f, h * 0.35f, w * 0.15f, h * 0.05f, w * 0.50f, h * 0.02f)
            close()
        }
    }

    // ---- Template compositing ------------------------------------------

    /** Draws the template artwork on top of the placed photos (spec section 35's documented order). */
    private fun drawTemplateArtwork(canvas: Canvas, artworkPath: String, targetWidth: Int, targetHeight: Int) {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(artworkPath, boundsOptions)
        if (boundsOptions.outWidth <= 0) return

        val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight, targetWidth, targetHeight)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val artwork = BitmapFactory.decodeFile(artworkPath, decodeOptions) ?: return

        val matrix = Matrix().apply {
            val scaleX = targetWidth.toFloat() / artwork.width
            val scaleY = targetHeight.toFloat() / artwork.height
            setScale(scaleX, scaleY)
        }
        canvas.drawBitmap(artwork, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        artwork.recycle()
    }
}

/** Converts a holder's normalized coordinates to pixel bounds on the output canvas. */
private fun PhotoHolderEntity.toPixelRect(canvasWidth: Int, canvasHeight: Int): Rect {
    val left = (x * canvasWidth).toInt()
    val top = (y * canvasHeight).toInt()
    val right = (left + width * canvasWidth).toInt()
    val bottom = (top + height * canvasHeight).toInt()
    return Rect(left, top, right, bottom)
}
