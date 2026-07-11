package com.dutchman.resumeiq.presentation.features.scan.camera

import androidx.compose.ui.geometry.Offset

private const val EPS = 0.02f

/** Document crop quad in normalized bitmap coordinates (0–1). Order: TL, TR, BR, BL. */
data class QuadNorm(
    val topLeft: Offset = Offset(0.08f, 0.08f),
    val topRight: Offset = Offset(0.92f, 0.08f),
    val bottomRight: Offset = Offset(0.92f, 0.92f),
    val bottomLeft: Offset = Offset(0.08f, 0.92f),
) {
    fun corners(): List<Offset> = listOf(topLeft, topRight, bottomRight, bottomLeft)

    fun clamped(): QuadNorm = QuadNorm(
        topLeft = clamp(topLeft),
        topRight = clamp(topRight),
        bottomRight = clamp(bottomRight),
        bottomLeft = clamp(bottomLeft),
    )

    companion object {
        fun defaultInset(): QuadNorm = QuadNorm()

        private fun clamp(o: Offset): Offset = Offset(
            o.x.coerceIn(EPS, 1f - EPS),
            o.y.coerceIn(EPS, 1f - EPS),
        )
    }
}
