package com.dutchman.resumeiq.presentation.features.question

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.toDomain
import com.dutchman.resumeiq.domain.models.Interviewer
import com.dutchman.resumeiq.domain.models.Question
import com.dutchman.resumeiq.domain.util.UserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val userFactory: UserFactory
) : ViewModel() {

    val questions: StateFlow<List<Question>> = questionDao.getAllQuestions()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _lastQuestionIndex = MutableStateFlow(userFactory.lastQuestionIndex)
    val lastQuestionIndex: StateFlow<Int>
        get() = _lastQuestionIndex.asStateFlow()


    private val _interviewer = MutableStateFlow(userFactory.interviewer)
    val interviewer: StateFlow<Interviewer?>
        get() = _interviewer.asStateFlow()


    fun saveLastQuestionIndex(index: Int) {
        Log.e("QuestionViewModel", "saveLastQuestionIndex: $index")
        userFactory.saveLastQuestionIndex(index)
        _lastQuestionIndex.update { index }
    }

    fun deleteQuestions(ids: List<Long>) {
        viewModelScope.launch {
            questionDao.deleteQuestionsByIds(ids)
        }
    }
}
