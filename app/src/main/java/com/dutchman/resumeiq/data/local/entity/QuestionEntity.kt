package com.dutchman.resumeiq.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dutchman.resumeiq.domain.models.Question

import androidx.room.Index

@Entity(
    tableName = "questions",
    indices = [Index(value = ["question"], unique = true)]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val question: String,
    val answer: String = "",
    val difficulty: String = "",
    val category: String = ""
)

fun QuestionEntity.toDomain(): Question {
    return Question(
        id = id,
        question = question,
        answer = answer,
        difficulty = difficulty,
        category = category,
        isSelected = true // typically when fetched they are displayed, selection might not matter for display
    )
}

fun Question.toEntity(): QuestionEntity {
    return QuestionEntity(
        // we omit id mapping if it's auto-generated, but domain id is currently a timestamp
        // if we map it, Room might just use it, or we can let Room generate it by omitting it or using 0 if the default id behavior allows it.
        // Let's use 0 to ensure Room auto-generates a clean sequential ID.
        id = 0, 
        question = question,
        answer = answer,
        difficulty = difficulty,
        category = category
    )
}
