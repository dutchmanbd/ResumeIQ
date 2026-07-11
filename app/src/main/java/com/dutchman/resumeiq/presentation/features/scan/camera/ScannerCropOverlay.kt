package com.dutchman.resumeiq.presentation.features.scan.camera

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

private const val NORM_EPS = 0.02f

private sealed class DragHandle {
    data class Corner(val index: Int) : DragHandle()
    data class Edge(val index: Int) : DragHandle()
}

private fun fitBitmapInBox(boxW: Float, boxH: Float, bmpW: Float, bmpH: Float): Rect {
    if (boxW <= 0f || boxH <= 0f || bmpW <= 0f || bmpH <= 0f) {
        return Rect(0f, 0f, boxW.coerceAtLeast(1f), boxH.coerceAtLeast(1f))
    }
    val boxAspect = boxW / boxH
    val bmpAspect = bmpW / bmpH
    return if (bmpAspect > boxAspect) {
        val h = boxW / bmpAspect
        val top = (boxH - h) * 0.5f
        Rect(0f, top, boxW, top + h)
    } else {
        val w = boxH * bmpAspect
        val left = (boxW - w) * 0.5f
        Rect(left, 0f, left + w, boxH)
    }
}

private fun normToScreen(fitted: Rect, nx: Float, ny: Float): Offset =
    Offset(fitted.left + nx * fitted.width, fitted.top + ny * fitted.height)

private fun screenDeltaToNorm(fitted: Rect, d: Offset): Offset =
    Offset(d.x / fitted.width, d.y / fitted.height)

private fun QuadNorm.withCornerMoved(index: Int, dn: Offset): QuadNorm {
    val c = corners().toMutableList()
    val p = c[index]
    c[index] = Offset(
        (p.x + dn.x).coerceIn(NORM_EPS, 1f - NORM_EPS),
        (p.y + dn.y).coerceIn(NORM_EPS, 1f - NORM_EPS),
    )
    return QuadNorm(c[0], c[1], c[2], c[3])
}

private fun QuadNorm.withEdgeMoved(index: Int, dn: Offset): QuadNorm {
    val i0 = index % 4
    val i1 = (index + 1) % 4
    val c = corners().toMutableList()
    for (i in listOf(i0, i1)) {
        val p = c[i]
        c[i] = Offset(
            (p.x + dn.x).coerceIn(NORM_EPS, 1f - NORM_EPS),
            (p.y + dn.y).coerceIn(NORM_EPS, 1f - NORM_EPS),
        )
    }
    return QuadNorm(c[0], c[1], c[2], c[3])
}

/** Move edge using full swipe: in/out (normal) + slide along edge (tangent), mapped to norm space. */
private fun QuadNorm.withEdgeMovedFromSwipe(index: Int, dragScreen: Offset, fitted: Rect): QuadNorm {
    val c = corners()
    val i0 = index % 4
    val i1 = (index + 1) % 4
    val p0 = normToScreen(fitted, c[i0].x, c[i0].y)
    val p1 = normToScreen(fitted, c[i1].x, c[i1].y)
    val ex = p1.x - p0.x
    val ey = p1.y - p0.y
    val len = hypot(ex, ey).coerceAtLeast(1e-4f)
    val tx = ex / len
    val ty = ey / len
    val nx = -ty
    val ny = tx
    val perp = dragScreen.x * nx + dragScreen.y * ny
    val para = dragScreen.x * tx + dragScreen.y * ty
    val screenPerp = Offset(nx * perp, ny * perp)
    val screenPara = Offset(tx * para, ty * para)
    val dn = screenDeltaToNorm(fitted, screenPerp) + screenDeltaToNorm(fitted, screenPara)
    return withEdgeMoved(index, dn)
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val l2 = abx * abx + aby * aby
    if (l2 < 1e-6f) return hypot(p.x - a.x, p.y - a.y)
    var t = ((p.x - a.x) * abx + (p.y - a.y) * aby) / l2
    t = t.coerceIn(0f, 1f)
    val px = a.x + t * abx
    val py = a.y + t * aby
    return hypot(p.x - px, p.y - py)
}

private fun hitTest(
    pos: Offset,
    quad: QuadNorm,
    fitted: Rect,
    cornerHitPx: Float,
    edgeHitPx: Float,
): DragHandle? {
    val corners = quad.corners()
    corners.forEachIndexed { i, co ->
        val p = normToScreen(fitted, co.x, co.y)
        if (hypot(pos.x - p.x, pos.y - p.y) <= cornerHitPx) return DragHandle.Corner(i)
    }
    var best: DragHandle? = null
    var bestDist = Float.MAX_VALUE
    for (edge in 0 until 4) {
        val a = normToScreen(fitted, corners[edge].x, corners[edge].y)
        val b = normToScreen(fitted, corners[(edge + 1) % 4].x, corners[(edge + 1) % 4].y)
        val d = distanceToSegment(pos, a, b)
        if (d <= edgeHitPx && d < bestDist) {
            bestDist = d
            best = DragHandle.Edge(edge)
        }
    }
    return best
}

/**
 * Shows the captured [bitmap] with a CamScanner-style quad overlay and eight draggable handles.
 * Dragging stays smooth because pointer handling does not restart when the quad updates.
 */
@Composable
fun ScannerCropEditor(
    bitmap: Bitmap,
    quad: QuadNorm,
    onQuadChange: (QuadNorm) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val cornerHitPx = with(density) { 44.dp.toPx() }
    val edgeHitPx = with(density) { 36.dp.toPx() }
    val handleRadiusPx = with(density) { 14.dp.toPx() }
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val dimColor = Color.Black.copy(alpha = 0.45f)
    val edgeColor = MaterialTheme.colorScheme.primary
    val handleFill = MaterialTheme.colorScheme.surface
    val handleStroke = MaterialTheme.colorScheme.primary

    val quadRef = rememberUpdatedState(quad)
    val onQuadRef = rememberUpdatedState(onQuadChange)

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val fitted = remember(boxW, boxH, bitmap.width, bitmap.height) {
            fitBitmapInBox(boxW, boxH, bitmap.width.toFloat(), bitmap.height.toFloat())
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(fitted, cornerHitPx, edgeHitPx) {
                    var activeHandle: DragHandle? = null
                    detectDragGestures(
                        onDragStart = { start ->
                            activeHandle = hitTest(
                                start,
                                quadRef.value,
                                fitted,
                                cornerHitPx,
                                edgeHitPx,
                            )
                        },
                        onDragEnd = { activeHandle = null },
                        onDragCancel = { activeHandle = null },
                        onDrag = { _, dragAmount ->
                            val h = activeHandle ?: return@detectDragGestures
                            val q = quadRef.value
                            val next = when (h) {
                                is DragHandle.Corner ->
                                    q.withCornerMoved(h.index, screenDeltaToNorm(fitted, dragAmount))
                                is DragHandle.Edge ->
                                    q.withEdgeMovedFromSwipe(h.index, dragAmount, fitted)
                            }
                            onQuadRef.value(next.clamped())
                        },
                    )
                },
        ) {
            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            Canvas(Modifier.fillMaxSize()) {
                val cornersPx = quad.corners().map { normToScreen(fitted, it.x, it.y) }
                val dimPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, size.width, size.height))
                    moveTo(cornersPx[0].x, cornersPx[0].y)
                    for (i in 1..4) {
                        val p = cornersPx[i % 4]
                        lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(dimPath, dimColor)

                for (i in 0 until 4) {
                    val a = cornersPx[i]
                    val b = cornersPx[(i + 1) % 4]
                    drawLine(
                        color = edgeColor,
                        start = a,
                        end = b,
                        strokeWidth = strokeWidthPx,
                    )
                }

                cornersPx.forEach { center ->
                    drawCircle(color = handleFill, radius = handleRadiusPx, center = center)
                    drawCircle(
                        color = handleStroke,
                        radius = handleRadiusPx,
                        center = center,
                        style = Stroke(width = strokeWidthPx),
                    )
                }

                for (edge in 0 until 4) {
                    val a = quad.corners()[edge]
                    val b = quad.corners()[(edge + 1) % 4]
                    val mid = Offset((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
                    val c = normToScreen(fitted, mid.x, mid.y)
                    val r = handleRadiusPx * 0.9f
                    drawCircle(color = handleFill, radius = r, center = c)
                    drawCircle(
                        color = handleStroke,
                        radius = r,
                        center = c,
                        style = Stroke(width = strokeWidthPx),
                    )
                }
            }
        }
    }
}
