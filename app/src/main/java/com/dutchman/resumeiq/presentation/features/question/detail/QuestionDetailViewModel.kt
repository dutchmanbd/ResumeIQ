package com.dutchman.resumeiq.presentation.features.question.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.QuestionEntity
import com.dutchman.resumeiq.data.local.entity.toDomain
import com.dutchman.resumeiq.domain.ai.GemmaInferenceHelper
import com.dutchman.resumeiq.domain.models.Question
import com.dutchman.resumeiq.domain.util.FileStorage
import com.dutchman.resumeiq.domain.util.UserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionDao: QuestionDao,
    private val gemmaInferenceHelper: GemmaInferenceHelper,
    private val fileStorage: FileStorage,
    private val userFactory: UserFactory
) : ViewModel() {

    private val questionId: Long = checkNotNull(savedStateHandle["id"])

    val question: StateFlow<Question?> = questionDao.getQuestionById(questionId)
        .map { entity -> entity?.toDomain() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generatedAnswer = MutableStateFlow("")
    val generatedAnswer: StateFlow<String> = _generatedAnswer.asStateFlow()

    val externalApp: String
        get() = userFactory.externalApp

    fun generateAiAnswer() {
        val currentQuestion = question.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            _generatedAnswer.value = ""

            try {
                val file = fileStorage.getDownloadedFile()
                if (file != null) {
                    gemmaInferenceHelper.initialize(file.absolutePath)
                } else {
                    _isGenerating.value = false
                    return@launch
                }

                val prompt = """
                    You are a highly qualified job candidate interviewing for a position relevant to this resume. Provide a tailored, real-world answer with realistic example.
                    Interview Question: "${currentQuestion.question}"
                    RESPONSE RULES:
                    1. Speak in the FIRST PERSON ("I", "my team") as the candidate answering the question.
                    2. Keep the answer professional, punchy, and concise (under 750 words total). 
                    3. Output ONLY the raw response text. Do not wrap the output in markdown code blocks.
                """.trimIndent()

                gemmaInferenceHelper.generateResponse(prompt).collect { partialResult ->
                    _generatedAnswer.value += partialResult
                }

                // Generation complete, save to DB
                val finalAnswer = _generatedAnswer.value.trim()
                if (finalAnswer.isNotEmpty()) {
                    val updatedEntity = QuestionEntity(
                        id = currentQuestion.id,
                        question = currentQuestion.question,
                        answer = finalAnswer,
                        difficulty = currentQuestion.difficulty,
                        category = currentQuestion.category
                    )
                    questionDao.updateQuestion(updatedEntity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun updateAnswer(newAnswer: String) {
        val currentQuestion = question.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedEntity = QuestionEntity(
                id = currentQuestion.id,
                question = currentQuestion.question,
                answer = newAnswer,
                difficulty = currentQuestion.difficulty,
                category = currentQuestion.category
            )
            questionDao.updateQuestion(updatedEntity)
        }
    }
}
