package com.dutchman.resumeiq.domain.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleTranslatorManager(
    private val context: Context
) : TranslatorManager {

    private var _translatorApp: ResolveInfo? = updateTranslatorApp()
    override val translatorApp: ResolveInfo?
        get() = _translatorApp

    private var _iconBitmap: Bitmap? = null
    override val iconBitmap: Bitmap?
        get() = _iconBitmap

    private var _translatedText = MutableStateFlow("")
    override val translatedText: StateFlow<String>
        get() = _translatedText.asStateFlow()


    override fun isTranslatorInstalled(): Boolean {
        return _translatorApp != null
    }

    override fun launchPlayStoreForInstall() {
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "market://details?id=$GOOGLE_TRANSLATOR_PACKAGE".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: ActivityNotFoundException) {
            // Fallback if Play Store is not installed
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$GOOGLE_TRANSLATOR_PACKAGE".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    override fun translate(text: String) {
        val processTextIntent = Intent()
        processTextIntent.action = Intent.ACTION_PROCESS_TEXT
        processTextIntent.type = "text/plain"
        val activityInfo = translatorApp?.activityInfo ?: return
        val name =
            ComponentName(activityInfo.packageName, activityInfo.name)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            processTextIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        }

        processTextIntent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            component = name
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
            putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, text)
        }
        Log.e("GoogleTranslatorManager", "translate: $text")
        try {
            context.startActivity(processTextIntent)
        } catch (e: Exception) {
            Log.e("GoogleTranslatorManager", "translate: error ${e.message}")
        }
    }
    

    private fun updateTranslatorApp(): ResolveInfo? {
        val processTextIntent = Intent()
        processTextIntent.action = Intent.ACTION_PROCESS_TEXT
        processTextIntent.type = "text/plain"

        val customIntent = Intent("colordict.intent.action.SEARCH")

        val searchIntent = Intent()
        searchIntent.action = Intent.ACTION_SEARCH


        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.type = "text/plain"

        val processApps = resolveInfosList(
            processTextIntent, context.packageManager
        )

        val customApps = resolveInfosList(
            customIntent,
            context.packageManager
        )

        val searchApps = resolveInfosList(
            searchIntent,
            context.packageManager
        )

        val sendApps = resolveInfosList(
            sendIntent,
            context.packageManager
        )

        val translatorApps = processApps + customApps + searchApps + sendApps

        Log.e("TranslatorManager", "updateTranslatorApp: $searchApps, $sendApps")

        _translatorApp = translatorApps.firstOrNull { app ->
            app.activityInfo.name.contains(GOOGLE_TRANSLATOR_ACTIVITY) ||
                    app.activityInfo.packageName.contains(
                        other = GOOGLE_TRANSLATOR_PACKAGE,
                        ignoreCase = true
                    )
        }

        _iconBitmap = _translatorApp?.loadIcon(context.packageManager)?.toBitmap()

        return _translatorApp
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun resolveInfosList(intent: Intent, pm: PackageManager): List<ResolveInfo> {
        try {
            return pm.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            try {
                return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            } catch (e1: Exception) {
                return emptyList()
            }
        }
    }


    companion object {
        private const val GOOGLE_TRANSLATOR_PACKAGE = "com.google.android.apps.translate"
        private const val GOOGLE_TRANSLATOR_ACTIVITY = "TapToTranslateActivity"
    }
}
