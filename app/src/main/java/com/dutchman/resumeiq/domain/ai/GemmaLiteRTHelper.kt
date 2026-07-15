package com.dutchman.resumeiq.domain.ai

//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import com.google.ai.edge.litertlm.*
//import java.io.ByteArrayOutputStream
//import java.util.concurrent.CancellationException
//
///**
// * Helper class to run Gemma 4 (LiteRT-LM) multimodal (text and image) inference.
// */
//class GemmaLiteRTHelper(
//    private val context: Context,
//
//) {
//
//    private var engine: Engine? = null
//    private var conversation: Conversation? = null
//
//    val isInitialized: Boolean
//        get() = engine != null && conversation != null
//
//    companion object {
//        private const val TAG = "GemmaLiteRTHelper"
//    }
//
//    /**
//     * Initializes the LiteRT engine and creates a conversation instance.
//     * Ensure this is called before generating responses.
//     *
//     * @param modelPath The absolute path to the `.bin` or `.tflite` model file on the device.
//     * @param useGpuForText Whether to use GPU for text backend (NPU/CPU are other options depending on device).
//     */
//    fun initialize(modelPath: String, useGpuForText: Boolean = true) {
//        try {
//            // Vision backend MUST be GPU for multimodal models like Gemma 4
//            val visionBackend = Backend.GPU()
//
//            val textBackend = if (useGpuForText) Backend.GPU() else Backend.CPU()
//
//            val engineConfig = EngineConfig(
//                modelPath = modelPath,
//                backend = textBackend,
//                visionBackend = visionBackend,
//                maxNumTokens = 32000,
//                maxNumImages = 10,
//                cacheDir = context.getExternalFilesDir(null)?.absolutePath
//            )
//
//            engine = Engine(engineConfig).apply {
//                initialize()
//
//                // You can customize sampler configurations such as topK, topP, and temperature
//                conversation = createConversation(
//                    ConversationConfig(
//                        samplerConfig = SamplerConfig(
//                            topK = 64,
//                            topP = 0.95,
//                            temperature = 1.0,
//                        )
//                    )
//                )
//            }
//            Log.d(TAG, "Gemma LiteRT engine initialized successfully.")
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to initialize Gemma LiteRT engine", e)
//            throw e
//        }
//    }
//
//    /**
//     * Generates a response asynchronously using the initialized conversation.
//     *
//     * @param prompt Text input to the model.
//     * @param images List of bitmaps to process alongside text (optional).
//     * @param onPartialResult Callback for streaming text generation (token by token).
//     * @param onDone Callback invoked when generation completes successfully.
//     * @param onError Callback invoked when an error occurs during inference.
//     */
//    fun generateResponse(
//        prompt: String,
//        images: List<Bitmap> = emptyList(),
//        onPartialResult: (String) -> Unit,
//        onDone: () -> Unit,
//        onError: (String) -> Unit
//    ) {
//        val currentConversation = conversation
//        if (currentConversation == null) {
//            onError("Conversation is not initialized. Call initialize() first.")
//            return
//        }
//
//        val contents = mutableListOf<Content>()
//
//        // For multimodal models, pass images first
//        images.forEach { bitmap ->
//            contents.add(Content.ImageBytes(bitmap.toPngByteArray()))
//        }
//
//        // Then add the text prompt
//        if (prompt.isNotBlank()) {
//            contents.add(Content.Text(prompt))
//        }
//
//        currentConversation.sendMessageAsync(
//            Contents.of(contents),
//            object : MessageCallback {
//                override fun onMessage(message: Message) {
//                    onPartialResult(message.toString())
//                }
//
//                override fun onDone() {
//                    onDone()
//                }
//
//                override fun onError(throwable: Throwable) {
//                    if (throwable is CancellationException) {
//                        Log.i(TAG, "Generation cancelled.")
//                        onDone()
//                    } else {
//                        Log.e(TAG, "Inference error", throwable)
//                        onError(throwable.message ?: "Unknown inference error")
//                    }
//                }
//            }
//        )
//    }
//
//    /**
//     * Stops any ongoing response generation.
//     */
//    fun stopGeneration() {
//        conversation?.cancelProcess()
//    }
//
//    /**
//     * Cleans up the conversation and engine resources.
//     */
//    fun close() {
//        try {
//            conversation?.close()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error closing conversation", e)
//        } finally {
//            conversation = null
//        }
//
//        try {
//            engine?.close()
//        } catch (e: Exception) {
//            Log.e(TAG, "Error closing engine", e)
//        } finally {
//            engine = null
//        }
//    }
//
//    private fun Bitmap.toPngByteArray(): ByteArray {
//        val stream = ByteArrayOutputStream()
//        this.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        return stream.toByteArray()
//    }
//}
