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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userFactory: UserFactory,
    private val fileStorage: FileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(
        isModelDownloaded = userFactory.isModelDownloaded,
        isSkip = userFactory.isSkip
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

            MainEvent.Skip -> {
                userFactory.saveIsSkip(true)
                _uiState.update { state ->
                    state.copy(
                        isSkip = true
                    )
                }
            }
        }
    }
}