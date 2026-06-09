package com.dutchman.resumeiq.di

import android.content.Context
import com.dutchman.resumeiq.domain.ai.GemmaLiteRTHelper
import com.dutchman.resumeiq.domain.util.SharedPref
import com.dutchman.resumeiq.domain.util.UserFactory
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
    fun provideGemmaLiteRTHelper(
        @ApplicationContext context: Context
    ) = GemmaLiteRTHelper(context)
}