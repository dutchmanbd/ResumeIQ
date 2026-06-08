package com.dutchman.resumeiq.presentation.activities

sealed interface MainEvent {

    data object Skip: MainEvent

    data object RefreshModel: MainEvent


}