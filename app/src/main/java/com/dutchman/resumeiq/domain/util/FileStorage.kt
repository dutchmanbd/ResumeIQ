package com.dutchman.resumeiq.domain.util

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
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

//    fun deleteFile(): Boolean {
//        val filesDir = context.filesDir
//        var allDeleted = true
//
//        // List all files and subdirectories inside filesDir
//        val contents = filesDir.listFiles()
//
//        contents?.forEach { file ->
//            val success = if (file.isDirectory) {
//                file.deleteRecursively()
//            } else {
//                file.delete()
//            }
//            if (!success) {
//                allDeleted = false
//            }
//        }
//        return allDeleted
//    }


    suspend fun deleteFile(): Boolean = withContext(Dispatchers.IO) {
        val filesDir = context.filesDir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            clearViaNio(filesDir.toPath())
        } else {
            clearViaLegacySequence(filesDir)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun clearViaNio(rootPath: Path): Boolean {
        var success = true
        try {
            Files.walkFileTree(rootPath, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    return try {
                        Files.delete(file)
                        FileVisitResult.CONTINUE
                    } catch (e: IOException) {
                        success = false
                        FileVisitResult.CONTINUE
                    }
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (exc != null) {
                        success = false
                        return FileVisitResult.CONTINUE
                    }
                    // Preserve the root directory itself
                    if (dir != rootPath) {
                        try {
                            Files.delete(dir)
                        } catch (e: IOException) {
                            success = false
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
                    success = false
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (e: Exception) {
            success = false
        }
        return success
    }

    /**
     * Fallback for API < 26:
     * Uses Kotlin's lazy FileTreeWalk (walkBottomUp) to delete contents bottom-up
     * without loading full arrays into memory simultaneously.
     */
    private fun clearViaLegacySequence(rootDir: File): Boolean {
        var success = true
        // walkBottomUp ensures files and child directories are deleted before parent folders
        rootDir.walkBottomUp().forEach { file ->
            if (file != rootDir) {
                val deleted = file.delete()
                if (!deleted) {
                    success = false
                }
            }
        }
        return success
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
