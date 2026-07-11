package com.dutchman.resumeiq.presentation.features.scan.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

object BitmapPerspective {

    fun rotateBitmap(source: Bitmap, degreesClockwise: Float): Bitmap {
        val m = Matrix().apply { postRotate(degreesClockwise) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
    }

    /**
     * Dewarps the region bounded by [quad] (TL, TR, BR, BL in normalized coords) into an upright rectangle.
     */
    fun warpQuadToRectangle(bitmap: Bitmap, quad: QuadNorm): Bitmap? {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        val tl = OffsetPx(quad.topLeft.x * bw, quad.topLeft.y * bh)
        val tr = OffsetPx(quad.topRight.x * bw, quad.topRight.y * bh)
        val br = OffsetPx(quad.bottomRight.x * bw, quad.bottomRight.y * bh)
        val bl = OffsetPx(quad.bottomLeft.x * bw, quad.bottomLeft.y * bh)

        val wTop = hypot(tr.x - tl.x, tr.y - tl.y).toInt().coerceAtLeast(1)
        val wBot = hypot(br.x - bl.x, br.y - bl.y).toInt().coerceAtLeast(1)
        val hLeft = hypot(bl.x - tl.x, bl.y - tl.y).toInt().coerceAtLeast(1)
        val hRight = hypot(br.x - tr.x, br.y - tr.y).toInt().coerceAtLeast(1)
        val outW = max(wTop, wBot)
        val outH = max(hLeft, hRight)

        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val src = floatArrayOf(tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y)
        val dst = floatArrayOf(
            0f, 0f,
            outW.toFloat(), 0f,
            outW.toFloat(), outH.toFloat(),
            0f, outH.toFloat(),
        )
        val matrix = Matrix()
        if (!matrix.setPolyToPoly(src, 0, dst, 0, 4)) {
            result.recycle()
            return null
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return result
    }

    fun saveJpegToCache(context: Context, bitmap: Bitmap): Uri {
        val dir = File(context.cacheDir, "camera_capture").apply { mkdirs() }
        val file = File(dir, "md_scan_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    private data class OffsetPx(val x: Float, val y: Float)
}
