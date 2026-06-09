package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.net.Uri

import android.graphics.Bitmap

sealed interface ScanEvent {
    data class OnFileSelected(val uri: Uri, val context: Context) : ScanEvent
    data class OnGenerateQuestionsClicked(val images: List<Bitmap>) : ScanEvent
    data class OnQuestionSelected(val id: String) : ScanEvent
}
