package com.dutchman.resumeiq.presentation.features.scan

import android.graphics.Bitmap

data class ScanUiState(
    val showPreview: Boolean = false,
    val fileName: String = "",
    val pageCount: Int = 0,
    val previewImages: List<Bitmap> = emptyList(),
    val isProcessing: Boolean = false
)
