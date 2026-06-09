package com.dutchman.resumeiq.presentation.features.scan

import android.graphics.Bitmap

data class ParsedQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val question: String,
    val isSelected: Boolean = true
)

data class ScanUiState(
    val showPreview: Boolean = false,
    val fileName: String = "",
    val pageCount: Int = 0,
    val previewImages: List<Bitmap> = emptyList(),
    val isProcessing: Boolean = false,
    val isGenerating: Boolean = false,
    val generatedQuestions: String = "",
    val parsedQuestions: List<ParsedQuestion> = emptyList()
)
