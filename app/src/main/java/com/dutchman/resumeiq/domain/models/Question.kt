package com.dutchman.resumeiq.domain.models

data class Question(
    val id: Long = System.currentTimeMillis(),
    val question: String,
    val answer: String = "",
    val difficulty: String = "",
    val category: String = "",
    val isSelected: Boolean = true
)