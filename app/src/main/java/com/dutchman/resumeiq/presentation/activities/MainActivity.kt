package com.dutchman.resumeiq.presentation.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dutchman.resumeiq.presentation.theme.ResumeIQTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            
            // Determine dark theme
            val isDarkTheme = when (uiState.theme) {
                "Dark" -> true
                "Light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            // Apply Language
            val context = LocalContext.current
            val locale = if (uiState.language == "Bengali") Locale("bn") else Locale("en")
            val configuration = Configuration(context.resources.configuration)
            configuration.setLocale(locale)
            val localizedContext = context.createConfigurationContext(configuration)

            // Wrap the localized context to preserve the Activity context for Hilt
            val hiltSafeContext = remember(localizedContext, context) {
                object : ContextWrapper(localizedContext) {
                    override fun getBaseContext(): Context = context
                }
            }
            
            CompositionLocalProvider(
                LocalContext provides hiltSafeContext
            ) {
                ResumeIQTheme(darkTheme = isDarkTheme) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}