package com.dutchman.resumeiq.presentation.features.question.detail

sealed interface QuestionDetailEvent {
    data object GenerateAiAnswer : QuestionDetailEvent
    data object UpdateAnswer : QuestionDetailEvent
    data object CopyQuestion : QuestionDetailEvent
    data object TranslateText : QuestionDetailEvent
    data object OpenExternalApp : QuestionDetailEvent
}
