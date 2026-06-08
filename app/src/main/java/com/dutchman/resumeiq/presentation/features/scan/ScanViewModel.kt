package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.OnFileSelected -> processFile(event.uri, event.context)
        }
    }

    private fun processFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val fileName = getFileName(uri, context)
            
            // Extract pages for PDF
            val bitmaps = extractPdfPages(uri, context)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    showPreview = true,
                    fileName = fileName,
                    pageCount = if (bitmaps.isEmpty()) 1 else bitmaps.size,
                    previewImages = bitmaps
                )
            }
        }
    }

    private suspend fun extractPdfPages(uri: Uri, context: Context): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        try {
            val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            fileDescriptor?.let { fd ->
                val renderer = PdfRenderer(fd)
                val pageCount = renderer.pageCount
                val pagesToExtract = minOf(pageCount, 10)

                for (i in 0 until pagesToExtract) {
                    val page = renderer.openPage(i)
                    // Scale bitmap to a reasonable preview size (e.g., 800 width)
                    val width = 800
                    val height = (width.toFloat() / page.width * page.height).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // Fill background with white because PDF might have transparent background
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmaps.add(bitmap)
                    page.close()
                }
                renderer.close()
                fd.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        bitmaps
    }

    private fun getFileName(uri: Uri, context: Context): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "Unknown file"
    }
}
