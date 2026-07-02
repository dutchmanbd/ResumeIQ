package com.dutchman.resumeiq.presentation.activities

import androidx.lifecycle.ViewModel
import com.dutchman.resumeiq.domain.util.FileStorage
import com.dutchman.resumeiq.domain.util.UserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.dutchman.resumeiq.domain.util.FirebaseAuthHelper

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userFactory: UserFactory,
    private val fileStorage: FileStorage,
    private val firebaseAuthHelper: FirebaseAuthHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(
        isModelDownloaded = userFactory.isModelDownloaded,
        isSkip = userFactory.isSkip,
        isLoggedIn = firebaseAuthHelper.isUserLoggedIn(),
        theme = userFactory.theme,
        language = userFactory.language
    ))
    val uiState: StateFlow<MainUiState>
        get() = _uiState.asStateFlow()



    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.RefreshModel -> {
                _uiState.update { state ->
                    state.copy(
                        isModelDownloaded = true
                    )
                }
            }

            MainEvent.SignInAnonymously -> {
                viewModelScope.launch {
                    val success = firebaseAuthHelper.signInAnonymously()
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                isLoggedIn = true
                            )
                        }
                    }
                }
            }

            is MainEvent.SignInWithGoogle -> {
                // Now handled by signInWithGoogle(context) method below
                viewModelScope.launch {
                    val success = firebaseAuthHelper.signInWithGoogle(event.context)
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                isLoggedIn = true
                            )
                        }
                    }
                }
            }
            
            MainEvent.Logout -> {
                firebaseAuthHelper.logout()
                userFactory.saveIsSkip(false)
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = false,
                        isSkip = false
                    )
                }
            }

            is MainEvent.ChangeTheme -> {
                userFactory.saveTheme(event.theme)
                _uiState.update { state ->
                    state.copy(
                        theme = event.theme
                    )
                }
            }

            is MainEvent.ChangeLanguage -> {
                userFactory.saveLanguage(event.language)
                _uiState.update { state ->
                    state.copy(
                        language = event.language
                    )
                }
            }
        }
    }
}