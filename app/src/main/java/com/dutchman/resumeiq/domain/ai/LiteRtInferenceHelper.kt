package com.dutchman.resumeiq.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
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

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    companion object {
        private const val TAG = "LiteRtInferenceHelper"
    }

    private val initMutex = Mutex()

    override suspend fun initialize(modelPath: String) = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (engine != null) return@withLock

            try {
                // Safely fetch path on IO thread
                val cacheDirectory = context.applicationContext.getExternalFilesDir(null)?.absolutePath

                val visionBackend = if (supportsVision) Backend.GPU() else null
                var newEngine: Engine? = null

                val backendsToTry = if (useGpuForText) {
                    listOf(Backend.GPU(), Backend.CPU())
                } else {
                    listOf(Backend.CPU(), Backend.GPU())
                }

                for (backend in backendsToTry) {
                    var candidateEngine: Engine? = null
                    try {
                        val engineConfig = EngineConfig(
                            modelPath = modelPath,
                            backend = backend,
                            visionBackend = visionBackend,
                            maxNumTokens = 4000,
                            maxNumImages = 4,
                            cacheDir = cacheDirectory
                        )

                        candidateEngine = setupEngine(engineConfig)
                        newEngine = candidateEngine
                        Log.d(TAG, "LiteRT engine initialized with backend: $backend")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Backend $backend failed, cleaning native state...", e)
                        runCatching { candidateEngine?.close() }
                    }
                }

                val finalEngine = newEngine ?: throw IllegalStateException("Failed to initialize engine")
                engine = finalEngine

                // Allow the GPU driver lock to clear before triggering UI recomposition
                yield()

                // Safely post state update on the Main thread to eliminate frame dropping
                withContext(Dispatchers.Main.immediate) {
                    _isInitialized.value = true
                }

                Log.d(TAG, "Gemma LiteRT engine initialization complete.")

            } catch (e: Exception) {
                withContext(Dispatchers.Main.immediate) {
                    _isInitialized.value = false
                }
                Log.e(TAG, "Failed to initialize Gemma LiteRT engine", e)
                throw e
            }
        }
    }
    private suspend fun setupEngine(engineConfig: EngineConfig): Engine {
        return withContext(Dispatchers.IO) {
            val candidateEngine = Engine(engineConfig)
            // This now executes safely on an I/O thread pool
            candidateEngine.initialize()
            candidateEngine
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

        // For multimodal models, pass images first (up to maxNumImages=4)
        images.take(4).forEach { bitmap ->
            contents.add(Content.ImageBytes(bitmap.toJpegByteArray()))
        }

        // Then add the text prompt
        if (prompt.isNotBlank()) {
            contents.add(Content.Text(prompt))
        }

        val response = java.lang.StringBuilder()
        currentConversation.sendMessageAsync(Contents.of(contents)).collect { message ->
            response.append(message.toString())
        }
        response.toString()
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

        images.take(4).forEach { bitmap ->
            contents.add(Content.ImageBytes(bitmap.toJpegByteArray()))
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

    private fun Bitmap.toJpegByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        val resizedBitmap = this.resize(1024)
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        if (resizedBitmap != this) {
            resizedBitmap.recycle()
        }
        return stream.toByteArray()
    }

    private fun Bitmap.resize(maxSize: Int): Bitmap {
        val originalWidth = this.width
        val originalHeight = this.height

        if (originalWidth <= maxSize && originalHeight <= maxSize) {
            return this
        }

        val aspectRatio: Float = originalWidth.toFloat() / originalHeight.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (aspectRatio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / aspectRatio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxSize
            newWidth = (maxSize * aspectRatio).toInt().coerceAtLeast(1)
        }

        return Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
    }
}
