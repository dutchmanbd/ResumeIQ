package com.dutchman.resumeiq.domain.models

data class Question(
    val question: String,
    var answer: String = ""
)