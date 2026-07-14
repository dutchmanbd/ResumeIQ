package com.dutchman.resumeiq.presentation.features.question.detail

import android.app.Application
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.QuestionEntity
import com.dutchman.resumeiq.data.local.entity.toDomain
import com.dutchman.resumeiq.domain.ai.GemmaInferenceHelper
import com.dutchman.resumeiq.domain.models.Question
import com.dutchman.resumeiq.domain.util.FileStorage
import com.dutchman.resumeiq.domain.util.TranslatorManager
import com.dutchman.resumeiq.domain.util.UserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val application: Application,
    private val questionDao: QuestionDao,
    private val gemmaInferenceHelper: GemmaInferenceHelper,
    private val translatorManager: TranslatorManager,
    private val clipboardManager: ClipboardManager,
    private val fileStorage: FileStorage,
    private val userFactory: UserFactory
) : AndroidViewModel(application) {

    private val questionId: Long = checkNotNull(savedStateHandle["id"])

    private val _isGenerating = MutableStateFlow(false)
    private val _generatedAnswer = MutableStateFlow("")
    private val _copyText = MutableStateFlow("")

    val state = combine(
        questionDao.getQuestionById(questionId).map { it?.toDomain() },
        _isGenerating,
        _generatedAnswer,
        _copyText
    ) { question, isGenerating, generatedAnswer, copyText ->
        Log.e("QuestionDetail", "copyText: $copyText")
        QuestionDetailState(
            question = question,
            isGenerating = isGenerating,
            generatedAnswer = generatedAnswer,
            copyText = copyText,
            externalApp = userFactory.externalApp,
            isModelDownloaded = userFactory.isModelDownloaded,
            translateIcon = translatorManager.iconBitmap
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuestionDetailState(
            externalApp = userFactory.externalApp,
            isModelDownloaded = userFactory.isModelDownloaded,
            translateIcon = translatorManager.iconBitmap
        )
    )


    val listener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            val text = item.text?.toString() ?: ""
            _copyText.update { text }
        }
    }

    init {
        clipboardManager.addPrimaryClipChangedListener(listener)
    }


    override fun onCleared() {
        clipboardManager.removePrimaryClipChangedListener(listener)
        super.onCleared()
    }


    fun onEvent(event: QuestionDetailEvent) {
        when (event) {
            is QuestionDetailEvent.GenerateAiAnswer -> generateAiAnswer()
            QuestionDetailEvent.UpdateAnswer -> {
                val clipData = clipboardManager.primaryClip
                val answerText =
                    if (clipboardManager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true) {
                        val item = clipData?.getItemAt(0)
                        item?.htmlText ?: ""
                    } else {
                        val item = clipData?.getItemAt(0)
                        item?.text?.toString() ?: ""
                    }
                updateAnswer(answerText)
            }

            QuestionDetailEvent.CopyQuestion -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        "copied_text",
                        state.value.question?.question ?: ""
                    )
                )
            }

            is QuestionDetailEvent.TranslateText -> {
                translatorManager.translate(state.value.copyText)
                _copyText.update {
                    ""
                }
            }
        }
    }


    private fun generateAiAnswer() {
        val currentQuestion = state.value.question ?: return

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

    private fun updateAnswer(newAnswer: String) {
        val currentQuestion = state.value.question ?: return
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
