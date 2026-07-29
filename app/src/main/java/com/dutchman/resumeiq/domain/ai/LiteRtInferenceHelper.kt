package com.dutchman.resumeiq.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LiteRtInferenceHelper(
    private val context: Context,
    private val useGpuForText: Boolean = false,
    private val supportsVision: Boolean = false
) : LlmInterface {

    private var engine: Engine? = null
    private var conversation: Conversation? = null

    companion object {
        private const val TAG = "LiteRtInferenceHelper"
    }

    override suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        if (engine != null) return@withContext
        try {
            val visionBackend = if (supportsVision) Backend.GPU() else null
            val textBackend = if (useGpuForText) Backend.GPU() else Backend.CPU()
            
            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = textBackend,
                visionBackend = visionBackend,
                maxNumTokens = 32000,
                maxNumImages = 4,
                cacheDir = context.getExternalFilesDir(null)?.absolutePath
            )
            
            val newEngine = Engine(engineConfig)
            newEngine.initialize()
            engine = newEngine
            Log.d(TAG, "Gemma LiteRT engine initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Gemma LiteRT engine", e)
            throw e
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        images: List<Bitmap>
    ): String = withContext(Dispatchers.IO) {
        val currentEngine = engine ?: throw IllegalStateException("Model not initialized")
        
        // Ensure conversation is closed and recreated if it exists
        conversation?.close()
        
        val currentConversation = currentEngine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 64,
                    topP = 0.95,
                    temperature = 1.0,
                )
            )
        )
        conversation = currentConversation

        val contents = mutableListOf<Content>()

        // For multimodal models, pass images first
        images.forEach { bitmap ->
            contents.add(Content.ImageBytes(bitmap.toPngByteArray()))
        }

        // Then add the text prompt
        if (prompt.isNotBlank()) {
            contents.add(Content.Text(prompt))
        }

        val responseMessage = currentConversation.sendMessage(Contents.of(contents))
        responseMessage.toString()
    }

    override fun generateResponseStreaming(
        prompt: String,
        images: List<Bitmap>
    ): Flow<String> = kotlinx.coroutines.flow.flow {
        val currentEngine = engine ?: error("Model not initialized")
        
        conversation?.close()
        val currentConversation = currentEngine.createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = 64,
                    topP = 0.95,
                    temperature = 1.0,
                )
            )
        )
        conversation = currentConversation
        
        val contents = mutableListOf<Content>()

        images.forEach { bitmap ->
            contents.add(Content.ImageBytes(bitmap.toPngByteArray()))
        }

        if (prompt.isNotBlank()) {
            contents.add(Content.Text(prompt))
        }
        
        currentConversation.sendMessageAsync(Contents.of(contents)).collect { message ->
            emit(message.toString())
        }
    }.flowOn(Dispatchers.IO)

    override fun closeSession() {
        try {
            conversation?.cancelProcess()
        } catch (e: Exception) {
            // ignore
        }
        try {
            conversation?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing conversation", e)
        } finally {
            conversation = null
        }
    }

    override fun release() {
        closeSession()
        try {
            engine?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing engine", e)
        } finally {
            engine = null
        }
    }

    private fun Bitmap.toPngByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
