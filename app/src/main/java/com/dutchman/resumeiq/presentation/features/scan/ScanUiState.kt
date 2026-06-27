package com.dutchman.resumeiq.presentation.features.scan

import android.graphics.Bitmap
import com.dutchman.resumeiq.domain.models.Question

//data class Question(
//    val id: String = java.util.UUID.randomUUID().toString(),
//    val question: String,
//    val difficulty: String = "",
//    val category: String = "",
//    val isSelected: Boolean = true
//)

data class ScanUiState(
    val showPreview: Boolean = false,
    val fileName: String = "",
    val pageCount: Int = 0,
    val previewImages: List<Bitmap> = emptyList(),
    val isProcessing: Boolean = false,
    val isGenerating: Boolean = false,
    val generatedQuestions: String = "",
    val parsedQuestions: List<Question> = emptyList(),
    val selectedImageBitmap: Bitmap? = null,
    val selectedPages: List<Int> = listOf(0),
    val showPasteDialog: Boolean = false
)
