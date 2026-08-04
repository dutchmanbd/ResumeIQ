package com.dutchman.resumeiq.domain.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.genai.llminference.GraphOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class GemmaInferenceHelper(
    private val context: Context,
    /**
     * When true, the MediaPipe LLM engine is initialized with vision support
     * (`setMaxNumImages` + per-session `GraphOptions.setEnableVisionModality(true)`).
     * Set this only for `.task` models that actually ship a vision encoder
     * (e.g. Gemma 3n E2B/E4B). Setting it for text-only models will cause
     * MediaPipe to throw "Vision modality is not enabled" at `addImage` time.
     */
    private val supportsVision: Boolean = false,
) : LlmInterface {
    private var llmInference: LlmInference? = null
    private var session: LlmInferenceSession? = null

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    companion object {
        private const val TAG = "GemmaInferenceHelper"
        private const val MAX_TOKENS = 32000
        private const val MAX_NUM_IMAGES = 4
        // Maximum time (ms) to wait for model loading before giving up
        private const val INIT_TIMEOUT_MS = 180_000L
    }

    @Volatile private var isInitializing = false

    /**
     * Kicks off model initialization on a dedicated background Thread and returns
     * immediately — never blocks the calling thread or any coroutine dispatcher.
     *
     * Completion is signalled via [isInitialized]. The generate methods internally
     * await this flag, so callers just need to invoke [initialize] then call a
     * generate method.
     */
    override fun initialize(modelPath: String) {
        if (llmInference != null || isInitializing) return
        isInitializing = true

        Thread({
            // Lower this thread's OS-level priority so the Android scheduler
            // always favours the UI/render thread during model loading.
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            // Give the UI one frame to settle before starting heavy native work.
            Thread.sleep(16)
            val modelFile = File(modelPath)
            try {
                require(modelFile.exists()) { "Model file does not exist: $modelPath" }
                require(modelFile.length() >= 1024L * 1024L) { "Model file is too small or invalid." }
                require(
                    modelFile.extension.equals("task", ignoreCase = true) ||
                        modelFile.extension.equals("litertlm", ignoreCase = true)
                ) {
                    "Unsupported model format '${modelFile.extension}'. Expected .task or .litertlm"
                }

                val builder = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .setPreferredBackend(LlmInference.Backend.DEFAULT)
                //  .setMaxTopK(64)

                if (supportsVision) {
                    builder.setMaxNumImages(MAX_NUM_IMAGES)
                }

                llmInference = LlmInference.createFromOptions(context, builder.build())
                _isInitialized.value = true
                Log.d(TAG, "Gemma MediaPipe engine initialized successfully.")
            } catch (e: Exception) {
                _isInitialized.value = false
                Log.e(TAG, "Failed to initialize Gemma MediaPipe engine", e)
            } finally {
                isInitializing = false
            }
        }, "gemma-init-thread").start()
    }

    /**
     * Suspends (without blocking any thread) until [isInitialized] is true, then
     * returns the engine. Throws if initialization timed out or failed.
     */
    private suspend fun awaitInference(): LlmInference {
        if (!_isInitialized.value) {
            withTimeoutOrNull(INIT_TIMEOUT_MS) { _isInitialized.first { it } }
                ?: throw IllegalStateException("Model initialization timed out or failed")
        }
        return llmInference ?: throw IllegalStateException("Model not initialized")
    }

    override suspend fun generateResponse(
        prompt: String,
        images: List<Bitmap>
    ): String = withContext(Dispatchers.IO) {
        val inference = awaitInference()

        if (images.isNotEmpty() && !supportsVision) {
            throw IllegalStateException(
                "This model does not support image input. Re-initialize with supportsVision=true and a vision-capable .task model."
            )
        }

        val currentSession = try {
            val sessionBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            if (supportsVision) {
                sessionBuilder.setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(true)
                        .build()
                )
            }
            LlmInferenceSession.createFromOptions(inference, sessionBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        session = currentSession

        try {
            for (bitmap in images) {
                val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
                currentSession.addImage(mpImage)
            }

            currentSession.addQueryChunk(prompt)

            val builder = StringBuilder()
            val future = currentSession.generateResponseAsync { partialResult, _ ->
                builder.append(partialResult)
            }

            future.await()
            builder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        } finally {
            if (session === currentSession) {
                session = null
            }
            disposeInferenceSession(currentSession)
        }
    }

    override fun generateResponseStreaming(
        prompt: String,
        images: List<Bitmap>
    ): Flow<String> = callbackFlow {
        // Suspend here (non-blocking) until the background init thread completes
        val inference = try {
            awaitInference()
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        if (images.isNotEmpty() && !supportsVision) {
            close(
                IllegalStateException(
                    "This model does not support image input. Re-initialize with supportsVision=true and a vision-capable .task model."
                )
            )
            return@callbackFlow
        }

        val currentSession = try {
            val sessionBuilder = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            if (supportsVision) {
                sessionBuilder.setGraphOptions(
                    GraphOptions.builder()
                        .setEnableVisionModality(true)
                        .build()
                )
            }
            LlmInferenceSession.createFromOptions(inference, sessionBuilder.build())
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        session = currentSession

        try {
            for (bitmap in images) {
                val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
                currentSession.addImage(mpImage)
            }

            currentSession.addQueryChunk(prompt)

            currentSession.generateResponseAsync { partialResult, done ->
                trySend(partialResult)
                if (done) {
                    close()
                }
            }
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            if (session === currentSession) {
                session = null
            }
            disposeInferenceSession(currentSession)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Stops an in-flight [generateResponseAsync] stream; [close] alone does not cancel generation.
     */
    private fun disposeInferenceSession(s: LlmInferenceSession?) {
        if (s == null) return
        runCatching { s.cancelGenerateResponseAsync() }
        runCatching { s.close() }
    }

    override fun closeSession() {
        val s = session
        session = null
        disposeInferenceSession(s)
    }

    override fun release() {
        closeSession()
        llmInference?.close()
        llmInference = null
        _isInitialized.value = false
        isInitializing = false
    }
}