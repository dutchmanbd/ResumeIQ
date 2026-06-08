package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.net.Uri

sealed interface ScanEvent {
    data class OnFileSelected(val uri: Uri, val context: Context) : ScanEvent
}
