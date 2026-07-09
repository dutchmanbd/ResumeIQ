package com.dutchman.resumeiq.data.worker

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.dutchman.resumeiq.domain.util.UserFactory
import com.dutchman.resumeiq.domain.util.FileStorage

@HiltWorker
class ModelDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val userFactory: UserFactory,
    private val fileStorage: FileStorage
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_URL = "KEY_URL"
        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_TOTAL_BYTES = "KEY_TOTAL_BYTES"
        const val KEY_DOWNLOADED_BYTES = "KEY_DOWNLOADED_BYTES"
        const val KEY_SPEED = "KEY_SPEED" // MB/s
        
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "model_download_channel"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo(0, "0.0")
    }

    private fun createForegroundInfo(progress: Int, speed: String): ForegroundInfo {
        val title = "Downloading ${FileStorage.DISPLAY_NAME}"
        val content = "Progress: $progress% ($speed MB/s)"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, com.dutchman.resumeiq.presentation.activities.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .build()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val modelUrl = inputData.getString(KEY_URL) ?: return@withContext Result.failure()

        setForeground(createForegroundInfo(0, "0.0"))

        // Fast path: Check if file already exists in internal storage
        if (fileStorage.getDownloadedFile() != null) {
            return@withContext Result.success()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        var downloadId = userFactory.getDownloadId(FileStorage.FILE_NAME)

        // Check if download is already in progress or finished
        if (downloadId != -1L) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            downloadManager.query(query).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIdx != -1) {
                        val status = cursor.getInt(statusIdx)
                        if (status == DownloadManager.STATUS_FAILED) {
                            downloadId = -1L
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            if (fileStorage.getDownloadedFile() != null) {
                                return@withContext Result.success()
                            }
                            // Let the while loop copy the file if it hasn't been copied yet
                        }
                    } else {
                        downloadId = -1L
                    }
                } else {
                    downloadId = -1L
                }
            }
        }

        if (downloadId == -1L) {
            val request = DownloadManager.Request(modelUrl.toUri())
                .setTitle("Downloading AI Model")
                .setDescription("Preparing offline interview assistant")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalFilesDir(context, null, FileStorage.FILE_NAME)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadId = downloadManager.enqueue(request)
            userFactory.saveDownloadId(FileStorage.FILE_NAME, downloadId)
        }

        var lastProgress = -1
        var lastBytes = 0L
        var lastTime = System.currentTimeMillis()

        while (true) {
            if (isStopped) return@withContext Result.retry()

            val query = DownloadManager.Query().setFilterById(downloadId)
            val result = downloadManager.query(query).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    if (statusIdx != -1 && bytesIdx != -1 && totalIdx != -1) {
                        val status = cursor.getInt(statusIdx)
                        val bytesDownloaded = cursor.getLong(bytesIdx)
                        val totalBytes = cursor.getLong(totalIdx)

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                try {
                                    val uri = downloadManager.getUriForDownloadedFile(downloadId)
                                    if (uri != null) {
                                            context.contentResolver.openInputStream(uri)?.use { input ->
                                                fileStorage.saveFile(input)
                                            }
                                            downloadManager.remove(downloadId) // This removes the entry and the downloaded file
                                        } else {
                                            // Fallback if uri is null for some reason
                                            val externalFile = File(context.getExternalFilesDir(null), FileStorage.FILE_NAME)
                                            if (externalFile.exists()) {
                                                externalFile.inputStream().use { input ->
                                                    fileStorage.saveFile(input)
                                                }
                                                externalFile.delete()
                                            }
                                        }
                                        userFactory.removeDownloadId(FileStorage.FILE_NAME)
                                        Result.success()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Result.failure()
                                }
                            }
                            DownloadManager.STATUS_FAILED -> {
                                Log.e("ModelDownload", "doWork: ")
                                userFactory.removeDownloadId(FileStorage.FILE_NAME)
                                Result.failure()
                            }
                            else -> {
                                val currentTime = System.currentTimeMillis()
                                val timeDiff = (currentTime - lastTime) / 1000.0 // seconds
                                
                                val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
                                
                                if (timeDiff >= 1.0 || (progress != lastProgress && lastProgress == -1)) {
                                    val bytesDiff = bytesDownloaded - lastBytes
                                    val speed = if (timeDiff > 0) (bytesDiff / (1024.0 * 1024.0)) / timeDiff else 0.0
                                    val speedFormatted = String.format(java.util.Locale.US, "%.2f", speed)

                                    setProgress(workDataOf(
                                        KEY_PROGRESS to progress,
                                        KEY_DOWNLOADED_BYTES to bytesDownloaded,
                                        KEY_TOTAL_BYTES to totalBytes,
                                        KEY_SPEED to speedFormatted
                                    ))
                                    
                                    setForeground(createForegroundInfo(progress, speedFormatted))
                                    
                                    lastProgress = progress
                                    lastBytes = bytesDownloaded
                                    lastTime = currentTime
                                }
                                null // Continue loop
                            }
                        }
                    } else null
                } else null
            }

            if (result != null) return@withContext result

            delay(1000)
        }
        @Suppress("UNREACHABLE_CODE")
        Result.failure()
    }
}
