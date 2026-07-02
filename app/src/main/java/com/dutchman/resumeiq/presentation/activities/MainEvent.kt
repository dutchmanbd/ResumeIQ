package com.dutchman.resumeiq.presentation.activities

import android.content.Context

sealed interface MainEvent {

    data object RefreshModel: MainEvent
    data object SignInAnonymously: MainEvent
    data class SignInWithGoogle(val context: Context): MainEvent
    data object Logout: MainEvent
    data class ChangeTheme(val theme: String): MainEvent
    data class ChangeLanguage(val language: String): MainEvent
}