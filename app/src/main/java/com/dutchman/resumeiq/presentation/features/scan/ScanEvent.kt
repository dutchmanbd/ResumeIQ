package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.net.Uri

import android.graphics.Bitmap

sealed interface ScanEvent {
    data class OnFileSelected(val uri: Uri, val context: Context) : ScanEvent
    data class OnGenerateQuestionsClicked(val images: List<Bitmap>) : ScanEvent
    data class OnQuestionSelected(val id: Long) : ScanEvent
    data class OnSaveSelectedQuestions(val onSaved: () -> Unit) : ScanEvent
}

sealed interface ScanEffect {
    data object NavigateToQuestionPreview : ScanEffect

}
