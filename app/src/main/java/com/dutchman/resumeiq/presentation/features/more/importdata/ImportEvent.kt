package com.dutchman.resumeiq.presentation.features.more.importdata

import android.net.Uri

sealed interface ImportEvent {
    data class OnFileSelected(val uri: Uri?) : ImportEvent
    data object OnImportClicked : ImportEvent
}
