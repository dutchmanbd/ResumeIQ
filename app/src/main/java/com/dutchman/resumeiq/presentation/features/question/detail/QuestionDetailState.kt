package com.dutchman.resumeiq.presentation.features.question.detail

import android.graphics.Bitmap
import com.dutchman.resumeiq.domain.models.Question

data class QuestionDetailState(
    val question: Question? = null,
    val isGenerating: Boolean = false,
    val generatedAnswer: String = "",
    val copyText: String = "",
    val externalApp: String = "",
    val isModelDownloaded: Boolean = false,
    val translateIcon: Bitmap? = null
)
