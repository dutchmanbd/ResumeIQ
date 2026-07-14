package com.dutchman.resumeiq.domain.util

import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

interface TranslatorManager {

    val translatorApp: ResolveInfo?

    val iconBitmap: Bitmap?

    val translatedText: StateFlow<String>

    fun isTranslatorInstalled(): Boolean

    fun launchPlayStoreForInstall()

    fun translate(text: String)


}