package com.dutchman.resumeiq.presentation.features.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.dutchman.resumeiq.data.worker.ModelDownloadWorker
import com.dutchman.resumeiq.domain.util.Constants
import com.dutchman.resumeiq.domain.util.FileStorage
import com.dutchman.resumeiq.domain.util.UserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.dutchman.resumeiq.domain.ai.GemmaInferenceHelper

data class DownloadState(
    val progress: Int = 0,
    val currentBytes: Long = 0,
    val totalBytes: Long = 0,
    val speedMbPerSec: String = "0.0",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val estimatedTimeRemaining: String = "",
    val fileName: String = ""
)

enum class DownloadStatus {
    IDLE, DOWNLOADING, SUCCESS, ERROR
}

@HiltViewModel
class ModelDownloadViewModel @Inject constructor(
    application: Application,
    private val userFactory: UserFactory,
    private val gemmaInferenceHelper: GemmaInferenceHelper,
    private val fileStorage: FileStorage
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    fun startDownload(modelUrl: String = Constants.MODEL_4B) {
        _downloadState.value = _downloadState.value.copy(fileName = FileStorage.DISPLAY_NAME)
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString(ModelDownloadWorker.KEY_URL, modelUrl)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        // Enqueue unique work to avoid multiple downloads
        workManager.enqueueUniqueWork(
            "ModelDownloadWork",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )

        // Observe the work progress
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData("ModelDownloadWork").observeForever { workInfos ->
                val workInfo = workInfos?.firstOrNull { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED } ?: workInfos?.firstOrNull()
                if (workInfo != null) {
                    val progress = workInfo.progress.getInt(ModelDownloadWorker.KEY_PROGRESS, 0)
                    val currentBytes = workInfo.progress.getLong(ModelDownloadWorker.KEY_DOWNLOADED_BYTES, 0L)
                    val totalBytes = workInfo.progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, 0L)
                    val speed = workInfo.progress.getString(ModelDownloadWorker.KEY_SPEED) ?: "0.0"

                    when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val speedValue = speed.toDoubleOrNull() ?: 0.0
                            val timeRemaining = if (totalBytes > 0 && speedValue > 0.1) {
                                val remainingBytes = totalBytes - currentBytes
                                val remainingSeconds = (remainingBytes / (1024.0 * 1024.0)) / speedValue
                                val locale = java.util.Locale.US
                                when {
                                    remainingSeconds > 3600 -> String.format(locale, "%.1f h", remainingSeconds / 3600.0)
                                    remainingSeconds > 60 -> String.format(locale, "%.1f m", remainingSeconds / 60.0)
                                    else -> String.format(locale, "%d s", remainingSeconds.toInt())
                                }
                            } else ""

                            _downloadState.value = _downloadState.value.copy(
                                progress = progress,
                                currentBytes = currentBytes,
                                totalBytes = totalBytes,
                                speedMbPerSec = speed,
                                status = DownloadStatus.DOWNLOADING,
                                estimatedTimeRemaining = timeRemaining.ifEmpty { downloadState.value.estimatedTimeRemaining }
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            userFactory.saveIsModelDownloaded(true)
                            _downloadState.value = _downloadState.value.copy(
                                progress = 100,
                                currentBytes = totalBytes,
                                totalBytes = totalBytes,
                                speedMbPerSec = "0.0",
                                status = DownloadStatus.SUCCESS,
                                estimatedTimeRemaining = ""
                            )
                            val file = fileStorage.getDownloadedFile()
                            if (file != null) {
                                viewModelScope.launch {
                                    try {
                                        gemmaInferenceHelper.initialize(file.absolutePath)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            userFactory.saveIsModelDownloaded(false)
                            _downloadState.value = _downloadState.value.copy(
                                status = DownloadStatus.ERROR
                            )
                        }
                        else -> {
                            // Other states like ENQUEUED, CANCELLED, BLOCKED
                        }
                    }
                }
            }
        }
    }
}
