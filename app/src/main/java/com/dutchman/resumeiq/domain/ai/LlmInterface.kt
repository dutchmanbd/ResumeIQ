package com.dutchman.resumeiq.domain.ai

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow

interface LlmInterface {
    suspend fun initialize(modelPath: String)
    suspend fun generateResponse(prompt: String, images: List<Bitmap> = emptyList()): String
    fun generateResponseStreaming(prompt: String, images: List<Bitmap> = emptyList()): Flow<String>
    fun closeSession()
    fun release()
}
