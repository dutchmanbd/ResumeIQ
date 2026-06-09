package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import com.dutchman.resumeiq.domain.ai.GemmaLiteRTHelper
import com.dutchman.resumeiq.domain.util.FileStorage

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val gemmaLiteRTHelper: GemmaLiteRTHelper,
    private val fileStorage: FileStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.OnFileSelected -> processFile(event.uri, event.context)
            is ScanEvent.OnGenerateQuestionsClicked -> generateQuestions(event.images)
            is ScanEvent.OnQuestionSelected -> {
                val currentQuestions = _uiState.value.parsedQuestions
                val updatedQuestions = currentQuestions.map {
                    if (it.id == event.id) it.copy(isSelected = !it.isSelected) else it
                }
                _uiState.update { it.copy(parsedQuestions = updatedQuestions) }
            }
        }
    }

    private fun processFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            val fileName = getFileName(uri, context)

            // Extract pages for PDF or Image
            val bitmaps = extractImageOrPdfPages(uri, context)
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    showPreview = true,
                    fileName = fileName,
                    pageCount = if (bitmaps.isEmpty()) 1 else bitmaps.size,
                    previewImages = bitmaps
                )
            }
        }
    }

    private fun generateQuestions(images: List<Bitmap>) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isGenerating = true, generatedQuestions = "") }
            
            try {
                if (!gemmaLiteRTHelper.isInitialized) {
                    val file = fileStorage.getDownloadedFile()
                    if (file != null) {
                        gemmaLiteRTHelper.initialize(file.absolutePath)
                    } else {
                        _uiState.update { it.copy(isGenerating = false) }
                        return@launch
                    }
                }

                val message = """
                    You are an expert technical interviewer and executive recruiter. Your task is to analyze the text inside the provided resume image and generate a robust question bank for the interviewer.

                    OUTPUT REQUIREMENT:
                    You must generate a MINIMUM of 10 and a MAXIMUM of 20 distinct interview questions. 
                    
                    For each question, you must assign a difficulty level ("Basic" or "Advanced") and map it to one of these primary categories: "Technical Skill", "Leadership", "Behavioral", or "Project-Specific".
                    
                    Output your response EXCLUSIVELY as a valid JSON object. Do not include introductory text, markdown code blocks (like ```json), or explanatory notes. Follow this JSON schema exactly:
                    
                    {
                      "questions": [
                        {
                          "question": "The actual question text here",
                          "difficulty": "Basic or Advanced",
                          "category": "Technical Skill, Leadership, Behavioral, or Project-Specific"
                        }
                      ]
                    }
                   
                    CRITICAL RULES FOR GENERATION:
                    1. "Basic" questions should verify core competencies, standard tools, and fundamental behaviors mentioned.
                    2. "Advanced" questions should test edge cases, architectural decisions, conflict management, or scale limits based on their senior-level claims.
                    3. Keep generating items sequentially until you have populated at least 10-20 distinct objects in the array. Do not truncate the list.
                """.trimIndent()

                gemmaLiteRTHelper.generateResponse(
                    prompt = message,
                    images = images,
                    onPartialResult = { result ->
                        _uiState.update { it.copy(generatedQuestions = it.generatedQuestions + result) }
                    },
                    onDone = {
                        _uiState.update { it.copy(isGenerating = false) }
                        parseGeneratedQuestions(_uiState.value.generatedQuestions)
                    },
                    onError = { error ->
                        _uiState.update { it.copy(isGenerating = false) }
                    }
                )
            } catch (e: Throwable) {
                e.printStackTrace()
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun parseGeneratedQuestions(jsonString: String) {
        try {
            val parsedList = mutableListOf<ParsedQuestion>()

            // Try to find a JSON object
            val objStart = jsonString.indexOf("{")
            val objEnd = jsonString.lastIndexOf("}")

            // Try to find a JSON array
            val arrStart = jsonString.indexOf("[")
            val arrEnd = jsonString.lastIndexOf("]")

            if (objStart != -1 && objEnd != -1 && objStart < objEnd) {
                var cleanJson = jsonString.substring(objStart, objEnd + 1)

                // Attempt to parse, if it fails due to truncation, append ]} and try again
                var jsonObject: org.json.JSONObject? = null
                try {
                    jsonObject = org.json.JSONObject(cleanJson)
                } catch (e: Exception) {
                    try {
                        jsonObject = org.json.JSONObject("$cleanJson]}")
                    } catch (e2: Exception) {
                        try {
                            jsonObject = org.json.JSONObject("$cleanJson}")
                        } catch (e3: Exception) {
                            e3.printStackTrace()
                        }
                    }
                }

                if (jsonObject != null && jsonObject.has("questions")) {
                    val questionsArray = jsonObject.getJSONArray("questions")
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        val question = qObj.optString("question", "").trim().removeSurrounding("\"").trim()
                        val difficulty = qObj.optString("difficulty", "")
                        val category = qObj.optString("category", "")
                        if (question.isNotEmpty()) {
                            parsedList.add(ParsedQuestion(question = question, difficulty = difficulty, category = category))
                        }
                    }
                }
            } else if (arrStart != -1 && arrEnd != -1 && arrStart < arrEnd) {
                var cleanJson = jsonString.substring(arrStart, arrEnd + 1)
                var questionsArray: org.json.JSONArray? = null
                try {
                    questionsArray = org.json.JSONArray(cleanJson)
                } catch (e: Exception) {
                    try {
                        questionsArray = org.json.JSONArray("$cleanJson]")
                    } catch (e2: Exception) {
                        e2.printStackTrace()
                    }
                }

                if (questionsArray != null) {
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        val question = qObj.optString("question", "").trim().removeSurrounding("\"").trim()
                        val difficulty = qObj.optString("difficulty", "")
                        val category = qObj.optString("category", "")
                        if (question.isNotEmpty()) {
                            parsedList.add(ParsedQuestion(question = question, difficulty = difficulty, category = category))
                        }
                    }
                }
            }

            if (parsedList.isNotEmpty()) {
                _uiState.update { it.copy(parsedQuestions = parsedList) }
            } else {
                // Fallback: If parsing totally fails, at least show the raw text as one big question so user sees something happened
                parsedList.add(ParsedQuestion(question = jsonString, difficulty = "Unknown", category = "Unknown"))
                _uiState.update { it.copy(parsedQuestions = parsedList) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback on exception
            val fallbackList = listOf(ParsedQuestion(question = jsonString, difficulty = "Unknown", category = "Unknown"))
            _uiState.update { it.copy(parsedQuestions = fallbackList) }
        }
    }

    private suspend fun extractImageOrPdfPages(uri: Uri, context: Context): List<Bitmap> =
        withContext(Dispatchers.IO) {
            val bitmaps = mutableListOf<Bitmap>()
            try {
                val mimeType = context.contentResolver.getType(uri)
                if (mimeType?.startsWith("image/") == true) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    var bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        val maxWidth = 1024
                        if (bitmap.width > maxWidth) {
                            val ratio = maxWidth.toFloat() / bitmap.width
                            val scaledHeight = (bitmap.height * ratio).toInt()
                            bitmap = Bitmap.createScaledBitmap(bitmap, maxWidth, scaledHeight, true)
                        }
                        bitmaps.add(bitmap)
                    }
                } else {
                    val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                    fileDescriptor?.let { fd ->
                        val renderer = PdfRenderer(fd)
                        val pageCount = renderer.pageCount
                        val pagesToExtract = minOf(pageCount, 10)

                        for (i in 0 until pagesToExtract) {
                            val page = renderer.openPage(i)
                            // Scale bitmap to a reasonable preview size (e.g., 800 width)
                            val width = 800
                            val height = (width.toFloat() / page.width * page.height).toInt()
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                            // Fill background with white because PDF might have transparent background
                            bitmap.eraseColor(android.graphics.Color.WHITE)

                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                            bitmaps.add(bitmap)
                            page.close()
                        }
                        renderer.close()
                        fd.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bitmaps
        }

    private fun getFileName(uri: Uri, context: Context): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "Unknown file"
    }
}
