package com.dutchman.resumeiq.domain.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject

class FileStorage @Inject constructor(@ApplicationContext private val context: Context) {
    fun saveFile(inputStream: InputStream) {
        val file = File(context.filesDir, FILE_NAME)
        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun getDownloadedFile(): File? {
        val file = File(context.filesDir, FILE_NAME)
        return if (file.exists()) file else null
    }

    fun deleteFile(): Boolean {
        val file = File(context.filesDir, FILE_NAME)
        return file.exists() && file.delete()
    }

    companion object {
        const val FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val DISPLAY_NAME = "REM-4"
    }
}
