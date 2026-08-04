package com.dutchman.resumeiq.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.ai.edge.litertlm.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class LiteRtInferenceHelper(
    private val context: Context,
    private val useGpuForText: Boolean = false,
    private val supportsVision: Boolean = false
) : LlmInterface {

    @Volatile
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    companion object {
        private const val TAG = "LiteRtInferenceHelper"
        private const val INIT_TIMEOUT_MS = 180_000L

        /**
         * A dedicated single-thread [CoroutineDispatcher] for all model operations.
         *
         * Unlike [kotlinx.coroutines.Dispatchers.IO] (a shared pool), this dispatcher
         * is fully isolated — blocking native JNI calls like [Engine.initialize] will
         * never starve other IO work or cause indirect main-thread contention.
         *
         * A single thread also prevents GPU resource contention from concurrent
         * model operations.
         */
        private val modelDispatcher: CoroutineDispatcher =
            Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "litert-model-thread").apply {
                    isDaemon = true
                    priority = Thread.MIN_PRIORITY
                }
            }.asCoroutineDispatcher()
    }

    @Volatile
    private var isInitializing = false

    override suspend fun initialize(modelPath: String) {
        if (engine != null || isInitializing) return
        isInitializing = true

        withContext(modelDispatcher) {
            Log.e(TAG, "initialize: execute")
            // Lowest possible OS-level scheduling priority
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST)
            try {
                ensureActive() // Check for cancellation before heavy work

                val cacheDirectory =
                    context.applicationContext.getExternalFilesDir(null)?.absolutePath

                Log.e(TAG, "initialize: cache: $cacheDirectory")
                val visionBackend = if (supportsVision) Backend.GPU() else null

                val backendsToTry = if (useGpuForText) {
                    listOf(Backend.GPU(), Backend.CPU())
                } else {
                    listOf(Backend.CPU(), Backend.GPU())
                }

                var newEngine: Engine? = null
                for (backend in backendsToTry) {
                    ensureActive() // Check for cancellation between backend attempts

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
                        candidateEngine = Engine(engineConfig)
                        Log.e(TAG, "initialize: before call init")
                        candidateEngine.initialize()
                        newEngine = candidateEngine
                        Log.d(TAG, "LiteRT engine initialized with backend: ${backend.name}")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "Backend $backend failed", e)
                        runCatching { candidateEngine?.close() }
                    }
                }

                if (newEngine != null) {
                    engine = newEngine
                    _isInitialized.value = true
                    Log.d(TAG, "LiteRT engine initialization complete.")
                } else {
                    Log.e(TAG, "All backends failed to initialize engine.")
                    _isInitialized.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LiteRT engine", e)
                _isInitialized.value = false
            } finally {
                isInitializing = false
            }
        }
    }

    private suspend fun awaitEngine(): Engine {
        if (!_isInitialized.value) {
            withTimeoutOrNull(INIT_TIMEOUT_MS) { _isInitialized.first { it } }
                ?: throw IllegalStateException("Model initialization timed out or failed")
        }
        return engine ?: throw IllegalStateException("Model not initialized")
    }

    override suspend fun generateResponse(
        prompt: String,
        images: List<Bitmap>
    ): String = withContext(modelDispatcher) {
        val currentEngine = awaitEngine()

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
        val currentEngine = awaitEngine()

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
    }.flowOn(modelDispatcher)

    override fun closeSession() {
        try {
            conversation?.cancelProcess()
        } catch (_: Exception) {}
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
            _isInitialized.value = false
            isInitializing = false
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
