package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageCompressor {
    private const val MAX_WIDTH_PX = 480
    private const val JPEG_QUALITY = 68

    data class CompressedImageResult(
        val base64Data: String,
        val bitmap: Bitmap,
        val width: Int,
        val height: Int,
        val sizeKb: Int
    )

    /**
     * Compresses any Bitmap using an Android Canvas to max width 480px and JPEG 68% quality
     * so Firestore documents and local memory stay compact and fast.
     */
    fun compressBitmap(source: Bitmap): CompressedImageResult {
        val ratio = if (source.width > MAX_WIDTH_PX) {
            MAX_WIDTH_PX.toFloat() / source.width.toFloat()
        } else {
            1.0f
        }
        val targetWidth = (source.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (source.height * ratio).toInt().coerceAtLeast(1)

        val canvasBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.parseColor("#F7F8F6"))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val destRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
        canvas.drawBitmap(source, null, destRect, paint)

        val outputStream = ByteArrayOutputStream()
        canvasBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val jpegBytes = outputStream.toByteArray()
        val base64String = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val decodedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: canvasBitmap

        return CompressedImageResult(
            base64Data = base64String,
            bitmap = decodedBitmap,
            width = targetWidth,
            height = targetHeight,
            sizeKb = (jpegBytes.size / 1024).coerceAtLeast(1)
        )
    }

    fun compressFromUri(context: Context, uri: Uri): CompressedImageResult? {
        return try {
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > MAX_WIDTH_PX * 2) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            compressBitmap(rawBitmap)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(base64Data: String): Bitmap? {
        if (base64Data.isBlank()) return null
        return try {
            val cleanBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
