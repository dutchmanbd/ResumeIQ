package com.dutchman.resumeiq.di

import android.content.ClipboardManager
import android.content.Context
import com.dutchman.resumeiq.domain.ai.LlmInterface
import com.dutchman.resumeiq.domain.ai.LiteRtInferenceHelper
import com.dutchman.resumeiq.domain.util.SharedPref
import com.dutchman.resumeiq.domain.util.UserFactory
import androidx.room.Room
import com.dutchman.resumeiq.data.local.AppDatabase
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.domain.speech.LiveSpeechRecognizer
import com.dutchman.resumeiq.domain.util.ExternalAppManager
import com.dutchman.resumeiq.domain.util.GoogleTranslatorManager
import com.dutchman.resumeiq.domain.util.TranslatorManager
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideLiveSpeechRecognizer(
        @ApplicationContext context: Context
    ): LiveSpeechRecognizer = LiveSpeechRecognizer(context)

    @Provides
    @Singleton
    fun provideSharedPref(
        @ApplicationContext context: Context
    ): SharedPref = SharedPref(context)

    @Provides
    @Singleton
    fun provideUserFactory(
        sharedPref: SharedPref
    ): UserFactory = UserFactory(sharedPref)


    @Provides
    @Singleton
    fun provideLlmInterface(
        @ApplicationContext context: Context
    ): LlmInterface = LiteRtInferenceHelper(context, useGpuForText = false, supportsVision = true)

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideQuestionDao(
        db: AppDatabase
    ): QuestionDao {
        return db.questionDao
    }

    @Provides
    @Singleton
    fun provideClipboardManager(
        @ApplicationContext context: Context
    ): ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    @Provides
    @Singleton
    fun provideGoogleTranslatorManager(
        @ApplicationContext context: Context
    ): TranslatorManager = GoogleTranslatorManager(context)

    @Provides
    @Singleton
    fun provideExternalAppManager(
        @ApplicationContext context: Context
    ): ExternalAppManager = ExternalAppManager(context)

    @Provides
    @Singleton
    fun provideDocumentScanner(): GmsDocumentScanner {
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(10)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        return GmsDocumentScanning.getClient(options)
    }

}