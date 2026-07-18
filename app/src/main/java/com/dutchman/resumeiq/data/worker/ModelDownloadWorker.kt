package com.dutchman.resumeiq.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.dutchman.resumeiq.domain.util.UserFactory
import com.dutchman.resumeiq.domain.util.FileStorage
import java.io.InputStream
import java.util.Locale

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

    private var lastTime = System.currentTimeMillis()
    private var lastBytes = 0L

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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
        val fileUrl = inputData.getString(KEY_URL) ?: return@withContext Result.failure(workDataOf("ERROR" to "Missing URL"))

        setForeground(createForegroundInfo(0, "0.0"))

        // Fast path: Check if file already exists in internal storage
        if (fileStorage.getDownloadedFile() != null) {
            return@withContext Result.success()
        }

        try {
            val url = URL(fileUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(workDataOf("ERROR" to "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}"))
            }

            var totalBytes = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                connection.contentLengthLong
            } else {
                connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
            }
            if (totalBytes <= 0) {
                totalBytes = connection.getHeaderField("x-linked-size")?.toLongOrNull() ?: -1L
            }
            val inputStream: InputStream = connection.inputStream

            val targetFile = File(applicationContext.getExternalFilesDir(null), FileStorage.FILE_NAME)
            
            FileOutputStream(targetFile).use { outputStream ->
                val data = ByteArray(4096)
                var total: Long = 0
                var count: Int
                
                lastTime = System.currentTimeMillis()
                lastBytes = 0L

                while (inputStream.read(data).also { count = it } != -1) {
                    if (isStopped) {
                        inputStream.close()
                        return@withContext Result.retry()
                    }
                    total += count
                    outputStream.write(data, 0, count)

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = (currentTime - lastTime) / 1000.0 // seconds

                    if (timeDiff >= 1.0 || total == totalBytes) { // Update progress every second
                        val bytesDiff = total - lastBytes
                        val speed = if (timeDiff > 0 && bytesDiff > 0) {
                            (bytesDiff / (1024.0 * 1024.0)) / timeDiff
                        } else {
                            0.0
                        }
                        val speedFormatted = String.format(Locale.US, "%.1f", speed)
                        val progress = if (totalBytes > 0) ((total * 100) / totalBytes).toInt() else 0

                        setProgress(workDataOf(
                            KEY_PROGRESS to progress,
                            KEY_DOWNLOADED_BYTES to total,
                            KEY_TOTAL_BYTES to totalBytes,
                            KEY_SPEED to speedFormatted
                        ))

                        setForeground(createForegroundInfo(progress, speedFormatted))
                        
                        lastTime = currentTime
                        lastBytes = total
                    }
                }
            }

            // Copy to internal storage via FileStorage once download is complete on external storage
            targetFile.inputStream().use { input ->
                fileStorage.saveFile(input)
            }
            targetFile.delete() // Clean up external file

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("ResumeIQ_Worker", "doWork error: ${e.message}", e)
            Result.failure(workDataOf(KEY_URL to e.toString(), KEY_PROGRESS to -1))
        }
    }
}
