package com.dutchman.resumeiq

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.dutchman.resumeiq.domain.ai.LlmInterface
import com.dutchman.resumeiq.domain.util.FileStorage
import com.dutchman.resumeiq.domain.util.SharedPref
import javax.inject.Inject

@HiltAndroidApp
class ResumeIQ : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sharedPref: SharedPref

    @Inject
    lateinit var llmInterface: LlmInterface

    @Inject
    lateinit var fileStorage: FileStorage

    override fun onCreate() {
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}