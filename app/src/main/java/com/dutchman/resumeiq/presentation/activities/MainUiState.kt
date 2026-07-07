package com.dutchman.resumeiq.presentation.activities

data class MainUiState(
    val isLoggedIn: Boolean = false,
    val isSkip: Boolean = false,
    val isModelDownloaded: Boolean = false,
    val theme: String = "System Default",
    val language: String = "English",
    val externalApp: String = "ChatGPT",
)
