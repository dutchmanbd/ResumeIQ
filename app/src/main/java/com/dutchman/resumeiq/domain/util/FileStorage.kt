package com.dutchman.resumeiq.domain.util

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
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

    private fun getAvailableDownloadSpace(targetDir: File = context.filesDir): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val appSpecificUuid: UUID = storageManager.getUuidForPath(targetDir)
                storageManager.getAllocatableBytes(appSpecificUuid)
            } catch (e: IOException) {
                targetDir.usableSpace
            }
        } else {
            // Fallback for Android 7.1 and lower
            targetDir.usableSpace
        }
    }


    fun prepareStorageForDownload(requiredBytes: Long): Boolean {
        val destinationFile = getDownloadedFile() ?: context.filesDir
        val availableBytes = getAvailableDownloadSpace()


        if (availableBytes < requiredBytes) {
            return false // Not enough space
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val appSpecificUuid = storageManager.getUuidForPath(destinationFile)

            // Tells Android to clean cache to free space for this download
            storageManager.allocateBytes(appSpecificUuid, requiredBytes)
        }

        return true
    }

    companion object {
        const val FILE_NAME = "gemma-4-E4B-it.litertlm"
//        const val FILE_NAME = "gemma-3n-E4B-it-int4.task"
        const val DISPLAY_NAME = "REM-4"
    }
}
