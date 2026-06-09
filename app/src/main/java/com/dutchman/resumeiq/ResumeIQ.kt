package com.dutchman.resumeiq

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.dutchman.resumeiq.domain.ai.GemmaInferenceHelper
import com.dutchman.resumeiq.domain.util.FileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ResumeIQ : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var gemmaInferenceHelper: GemmaInferenceHelper

    @Inject
    lateinit var fileStorage: FileStorage

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            val file = fileStorage.getDownloadedFile()
            if (file != null) {
                try {
                    gemmaInferenceHelper.initialize(file.absolutePath)
                } catch (e: Exception) {
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}