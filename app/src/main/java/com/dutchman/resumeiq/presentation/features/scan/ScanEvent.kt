package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.net.Uri

import android.graphics.Bitmap

sealed interface ScanEvent {
    data class OnFileSelected(val uri: Uri, val context: Context) : ScanEvent
    data class OnGenerateQuestionsClicked(val images: List<Bitmap>) : ScanEvent
    data class OnQuestionSelected(val id: Long) : ScanEvent
    data class OnSaveSelectedQuestions(val onSaved: () -> Unit) : ScanEvent
    data class OnJsonFileSelected(val uri: Uri, val context: Context) : ScanEvent
    data class OnJsonTextPasted(val json: String) : ScanEvent
    data class OnScannedImageReady(val bitmap: Bitmap) : ScanEvent
    data class OnSelectedImageBitmapChanged(val bitmap: Bitmap?) : ScanEvent
    data class OnPageSelectionToggled(val pageIndex: Int) : ScanEvent
    data class OnShowPasteDialogChanged(val show: Boolean) : ScanEvent
    data object OnSpeechMicToggle : ScanEvent
    data class OnMicrophonePermissionResult(val granted: Boolean) : ScanEvent
    data class OnPromptTextChanged(val text: String) : ScanEvent
    data object OnGenerateQuestionsFromPrompt : ScanEvent
    data class OnSaveQuickQuestion(val question: String, val onSaved: () -> Unit) : ScanEvent
    data object OnClearPreview : ScanEvent
}

sealed interface ScanEffect {
    data object NavigateToQuestionPreview : ScanEffect

}
