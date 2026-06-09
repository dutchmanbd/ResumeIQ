package com.dutchman.resumeiq

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.dutchman.resumeiq.domain.ai.GemmaLiteRTHelper
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
    lateinit var gemmaLiteRTHelper: GemmaLiteRTHelper

    @Inject
    lateinit var fileStorage: FileStorage

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            val file = fileStorage.getDownloadedFile()
            if (file != null && !gemmaLiteRTHelper.isInitialized) {
                try {
                    gemmaLiteRTHelper.initialize(file.absolutePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}