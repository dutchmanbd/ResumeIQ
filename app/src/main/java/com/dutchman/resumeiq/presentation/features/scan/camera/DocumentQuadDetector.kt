package com.dutchman.resumeiq.presentation.features.scan.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.geometry.Offset
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Auto-detects a document-like quadrilateral (CamScanner-style) using OpenCV:
 * downscale → grayscale → Canny (and optionally adaptive threshold) → contours → approxPolyDP.
 */
object DocumentQuadDetector {

    private const val TAG = "DocumentQuadDetector"
    private const val MAX_PROCESSING_SIDE = 640.0

    @Volatile
    private var loadAttempted = false

    @Volatile
    private var loadSuccess = false

    private fun ensureOpenCv(): Boolean {
        if (loadAttempted) return loadSuccess
        synchronized(this) {
            if (loadAttempted) return loadSuccess
            loadAttempted = true
            loadSuccess = runCatching { OpenCVLoader.initLocal() }
                .onFailure { Log.w(TAG, "OpenCV initLocal failed", it) }
                .getOrDefault(false)
            return loadSuccess
        }
    }

    /**
     * Returns [QuadNorm] in normalized image coordinates, or null if detection fails.
     */
    fun detectQuad(bitmap: Bitmap): QuadNorm? {
        if (!ensureOpenCv()) return null
        val src = Mat()
        return try {
            Utils.bitmapToMat(bitmap, src)
            if (src.empty()) return null
            detectOnMat(src)
        } catch (t: Throwable) {
            Log.w(TAG, "detectQuad failed", t)
            null
        } finally {
            src.release()
        }
    }

    private fun detectOnMat(src: Mat): QuadNorm? {
        val maxDim = max(src.cols(), src.rows()).toDouble()
        val scale = min(1.0, MAX_PROCESSING_SIDE / maxDim)
        val small = Mat()
        val gray = Mat()
        try {
            if (scale < 1.0) {
                Imgproc.resize(src, small, Size(), scale, scale, Imgproc.INTER_AREA)
            } else {
                src.copyTo(small)
            }
            val w = small.cols()
            val h = small.rows()
            Imgproc.cvtColor(small, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

            val ordered = findBestQuadFromGray(gray, w, h)
                ?: findBestQuadFromAdaptive(gray, w, h)
            if (ordered == null) return null
            if (!pointsDistinct(ordered, w, h)) return null
            return quadNormFromOrdered(ordered, w, h).clamped()
        } finally {
            gray.release()
            small.release()
        }
    }

    private fun findBestQuadFromGray(gray: Mat, w: Int, h: Int): Array<Point>? {
        val edges = Mat()
        try {
            Imgproc.Canny(gray, edges, 55.0, 165.0)
            val k3 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
            Imgproc.morphologyEx(edges, edges, Imgproc.MORPH_CLOSE, k3)
            k3.release()
            return findBestQuadFromBinary(edges, w, h)
        } finally {
            edges.release()
        }
    }

    private fun findBestQuadFromAdaptive(gray: Mat, w: Int, h: Int): Array<Point>? {
        val bin = Mat()
        try {
            Imgproc.adaptiveThreshold(
                gray,
                bin,
                255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY_INV,
                19,
                8.0,
            )
            val k5 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
            Imgproc.morphologyEx(bin, bin, Imgproc.MORPH_CLOSE, k5)
            k5.release()
            return findBestQuadFromBinary(bin, w, h)
        } finally {
            bin.release()
        }
    }

    private fun findBestQuadFromBinary(binary: Mat, w: Int, h: Int): Array<Point>? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        try {
            Imgproc.findContours(
                binary,
                contours,
                hierarchy,
                Imgproc.RETR_LIST,
                Imgproc.CHAIN_APPROX_SIMPLE,
            )
            val imgArea = (w * h).toDouble()
            val minArea = imgArea * 0.08
            val maxArea = imgArea * 0.96
            var bestArea = 0.0
            var bestOrdered: Array<Point>? = null
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area < minArea || area > maxArea) continue
                val c2f = MatOfPoint2f(*contour.toArray())
                val peri = Imgproc.arcLength(c2f, true)
                if (peri < 1e-3) {
                    c2f.release()
                    continue
                }
                var chosen: Array<Point>? = null
                for (eps in listOf(0.012, 0.018, 0.025, 0.035, 0.05, 0.07, 0.09)) {
                    val approx = MatOfPoint2f()
                    try {
                        Imgproc.approxPolyDP(c2f, approx, eps * peri, true)
                        if (approx.total().toInt() == 4) {
                            val pts = approx.toArray()
                            if (isConvexQuad(pts) && pointsDistinct(pts, w, h)) {
                                val ordered = orderQuadCorners(pts)
                                if (pointsDistinct(ordered, w, h)) {
                                    chosen = ordered
                                    break
                                }
                            }
                        }
                    } finally {
                        approx.release()
                    }
                }
                c2f.release()
                if (chosen != null && area > bestArea) {
                    bestArea = area
                    bestOrdered = chosen
                }
            }
            return bestOrdered
        } finally {
            contours.forEach { it.release() }
            hierarchy.release()
        }
    }

    private fun isConvexQuad(pts: Array<Point>): Boolean {
        if (pts.size != 4) return false
        var sign = 0
        for (i in 0 until 4) {
            val a = pts[i]
            val b = pts[(i + 1) % 4]
            val c = pts[(i + 2) % 4]
            val z = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
            when {
                z > 1.0 -> {
                    if (sign == 0) sign = 1
                    else if (sign < 0) return false
                }
                z < -1.0 -> {
                    if (sign == 0) sign = -1
                    else if (sign > 0) return false
                }
            }
        }
        return sign != 0
    }

    private fun orderQuadCorners(pts: Array<Point>): Array<Point> {
        val sums = pts.map { it.x + it.y }
        val diffs = pts.map { it.x - it.y }
        val tl = pts[sums.indices.minByOrNull { sums[it] }!!]
        val br = pts[sums.indices.maxByOrNull { sums[it] }!!]
        val tr = pts[diffs.indices.minByOrNull { diffs[it] }!!]
        val bl = pts[diffs.indices.maxByOrNull { diffs[it] }!!]
        return arrayOf(tl, tr, br, bl)
    }

    private fun pointsDistinct(pts: Array<Point>, w: Int, h: Int): Boolean {
        val minDist = min(w, h) * 0.025
        for (i in 0 until 4) {
            for (j in i + 1 until 4) {
                val d = hypot(pts[i].x - pts[j].x, pts[i].y - pts[j].y)
                if (d < minDist) return false
            }
        }
        return true
    }

    private fun quadNormFromOrdered(ordered: Array<Point>, w: Int, h: Int): QuadNorm {
        fun n(p: Point) = Offset(
            (p.x / w).toFloat(),
            (p.y / h).toFloat(),
        )
        return QuadNorm(
            topLeft = n(ordered[0]),
            topRight = n(ordered[1]),
            bottomRight = n(ordered[2]),
            bottomLeft = n(ordered[3]),
        )
    }
}
