package com.dutchman.resumeiq.presentation.features.more.importdata

import android.net.Uri

data class ImportState(
    val selectedFileUri: Uri? = null,
    val selectedFileName: String? = null,
    val isImporting: Boolean = false,
    val importSuccess: Boolean? = null,
    val errorMessage: String? = null
)
