package com.dutchman.resumeiq.presentation.features.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.toDomain
import com.dutchman.resumeiq.domain.models.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val questionDao: QuestionDao
) : ViewModel() {

    val questions: StateFlow<List<Question>> = questionDao.getAllQuestions()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
