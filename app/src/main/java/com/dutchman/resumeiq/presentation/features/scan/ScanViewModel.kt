package com.dutchman.resumeiq.presentation.features.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner

import com.dutchman.resumeiq.domain.ai.LlmInterface
import com.dutchman.resumeiq.domain.models.Interviewer
import com.dutchman.resumeiq.domain.models.Question
import com.dutchman.resumeiq.domain.util.UserFactory
import com.dutchman.resumeiq.domain.speech.LiveSpeechRecognizer
import com.dutchman.resumeiq.domain.speech.SpeechEvent

import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.toEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.Job

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val llmInterface: LlmInterface,
    private val questionDao: QuestionDao,
    private val userFactory: UserFactory,
    private val liveSpeechRecognizer: LiveSpeechRecognizer,
    val documentScanner: GmsDocumentScanner
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = kotlinx.coroutines.channels.Channel<ScanEffect>()
    val event: kotlinx.coroutines.flow.Flow<ScanEffect>
        get() = _event.receiveAsFlow()
        
    private var speechJob: Job? = null
    private var generateJob: Job? = null

    init {
        _uiState.update { it.copy(isModelDownloaded = userFactory.isModelDownloaded) }
    }

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
            is ScanEvent.OnSaveSelectedQuestions -> saveSelectedQuestions(event.onSaved)
            is ScanEvent.OnJsonFileSelected -> processJsonFile(event.uri, event.context)
            is ScanEvent.OnJsonTextPasted -> parseGeneratedQuestions(event.json)
            is ScanEvent.OnScannedImageReady -> {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        showPreview = true,
                        fileName = "scanned_image.jpg",
                        pageCount = 1,
                        previewImages = listOf(event.bitmap)
                    )
                }
            }
            is ScanEvent.OnSelectedImageBitmapChanged -> {
                _uiState.update { it.copy(selectedImageBitmap = event.bitmap) }
            }
            is ScanEvent.OnPageSelectionToggled -> {
                _uiState.update { state ->
                    val newSelected = state.selectedPages.toMutableList()
                    if (newSelected.contains(event.pageIndex)) {
                        newSelected.remove(event.pageIndex)
                    } else {
                        newSelected.add(event.pageIndex)
                    }
                    state.copy(selectedPages = newSelected)
                }
            }
            is ScanEvent.OnShowPasteDialogChanged -> {
                _uiState.update { it.copy(showPasteDialog = event.show) }
            }
            is ScanEvent.OnSpeechMicToggle -> toggleSpeechRecording()
            is ScanEvent.OnMicrophonePermissionResult -> {
                _uiState.update { it.copy(isMicrophonePermissionGranted = event.granted) }
            }

            is ScanEvent.OnPromptTextChanged -> {
                _uiState.update { state ->
                    state.copy(
                        promptText = event.text
                    )
                }
            }
            is ScanEvent.OnGenerateQuestionsFromPrompt -> generateQuestions()
            is ScanEvent.OnSaveQuickQuestion -> saveQuickQuestion(event.question, event.onSaved)
            is ScanEvent.OnClearPreview -> {
                _uiState.update { 
                    it.copy(
                        showPreview = false, 
                        previewImages = emptyList(), 
                        selectedPages = listOf(0), 
                        fileName = "", 
                        pageCount = 0,
                        generatedQuestions = ""
                    ) 
                }
            }
            is ScanEvent.OnCancelGenerationClicked -> {
                generateJob?.cancel()
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun saveQuickQuestion(questionText: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            if (questionText.isNotBlank()) {
                val entity = com.dutchman.resumeiq.data.local.entity.QuestionEntity(
                    question = questionText.trim(),
                    difficulty = "Unknown",
                    category = "Unknown"
                )
                questionDao.insertQuestion(entity)
            }
            withContext(Dispatchers.Main) {
                onSaved()
            }
        }
    }

    private fun toggleSpeechRecording() {
        if (_uiState.value.isSpeechRecording) {
            stopLiveRecognition()
        } else {
            startLiveRecognition()
        }
    }

    private fun startLiveRecognition() {
        if (!_uiState.value.isMicrophonePermissionGranted) return
        speechJob?.cancel()
        _uiState.update { it.copy(isSpeechRecording = true, speechPartialText = "") }

        speechJob = viewModelScope.launch {
            liveSpeechRecognizer.startListening().collect { event ->
                when (event) {
                    is SpeechEvent.Partial -> {
                        _uiState.update { it.copy(speechPartialText = event.text) }
                    }
                    is SpeechEvent.Final -> {
                        val currentText = _uiState.value.speechPartialText
                        val newText = if (currentText.isEmpty()) event.text else "$currentText ${event.text}"
                        _uiState.update { it.copy(speechPartialText = newText) }
                    }
                    is SpeechEvent.Error -> {
                        stopLiveRecognition()
                    }
                    SpeechEvent.Done -> {
                        _uiState.update { it.copy(isSpeechRecording = false) }
                    }
                }
            }
        }
    }

    private fun stopLiveRecognition() {
        speechJob?.cancel()
        speechJob = null
        _uiState.update { it.copy(isSpeechRecording = false) }
    }

    private fun saveSelectedQuestions(onSaved: () -> Unit) {
        viewModelScope.launch {
            val selectedQuestions = _uiState.value.parsedQuestions.filter { it.isSelected }
            if (selectedQuestions.isNotEmpty()) {
                questionDao.insertQuestions(selectedQuestions.map { it.toEntity() })
            }
            withContext(Dispatchers.Main) {
                onSaved()
            }
        }
    }

    private fun processJsonFile(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: ""
                withContext(Dispatchers.Main) {
                    parseGeneratedQuestions(jsonString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun processFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            // Perform file IO on background thread
            val (fileName, bitmaps) = withContext(Dispatchers.IO) {
                val name = getFileName(uri, context)
                val extracted = extractImageOrPdfPages(uri, context)
                Pair(name, extracted)
            }

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


    private fun generateQuestions() {
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generatedQuestions = "") }

            try {

                val message = """
                    You are an expert technical interviewer and recruiter analyzing the attached resume image.

                    TASK:
                    Generate a highly professional, real-world interview question bank based ONLY ${uiState.value.promptText}.

                    QUESTION STYLE:
                    Create realistic, scenario-based, and technical questions tailored to their specific industry and seniority. Questions MUST be extremely concise (1 short sentence maximum) to ensure fast generation.

                    JSON OUTPUT FORMAT:
                    Output EXCLUSIVELY a raw JSON object. Do not include markdown tags like ```json, do not write code blocks, and do not write closing/opening chat greetings. Use this exact compact schema:

                    {"questions": [{"c": "Skill | Lead | Behav", "l": "Basic | Adv", "q": "The professional interview question."}], "info":{ "name":"Jewel Rana", "designation":"Senior Mobile Application Developer", "Mobile":"+8801812386609"}}
                    
                    FIELD DESCRIPTIONS:
                    - questions: The array containing the generated questions.
                    - c (Category): Must be exactly one of: "Skill", "Lead", "Behav".
                    - l (Level): Must be exactly one of: "Basic" or "Adv".
                    - q (Question): The actual question text.
                    
                    GENERATION REQUIREMENTS:
                    Ensure exactly 5-6 questions are output in the "questions" array. DO NOT generate more questions to keep generation time short. Keep responses extremely brief.
                """.trimIndent()

                var bufferedText = ""
                var lastUpdateTime = System.currentTimeMillis()

                llmInterface.generateResponseStreaming(
                    prompt = message,
                ).collect { chunk ->
                    bufferedText += chunk
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > 50) {
                        val newText = bufferedText
                        _uiState.update { it.copy(generatedQuestions = newText) }
                        lastUpdateTime = currentTime
                    }
                }
                
                // Final update to ensure no chunk is missed
                _uiState.update { it.copy(generatedQuestions = bufferedText) }

                _uiState.update { it.copy(isGenerating = false) }
                val jsonString = _uiState.value.generatedQuestions
                Log.d("ScanViewModel", "generateQuestions: $jsonString")
                parseGeneratedQuestions(jsonString)
            } catch (e: Throwable) {
                e.printStackTrace()
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }


    private fun generateQuestions(images: List<Bitmap>) {
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, generatedQuestions = "") }

            try {

                val message = """
                    You are an expert technical interviewer and recruiter analyzing the attached resume image.
                    TASK:
                    Generate a highly professional, real-world interview question bank based ONLY resume image.

                    QUESTION STYLE:
                    Create realistic, scenario-based, and technical questions tailored to their specific industry and seniority. Questions should be concise (1-2 sentences maximum) but challenging, reflecting actual interviews for their target role.

                    JSON OUTPUT FORMAT:
                    Output EXCLUSIVELY a raw JSON object. Do not include markdown tags like ```json, do not write code blocks, and do not write closing/opening chat greetings. Use this exact compact schema:

                    [{"c": "Skill | Lead | Behav", "l": "Basic | Adv", "q": "The professional interview question."}]
                    
                    FIELD DESCRIPTIONS:
                    - c (Category): Must be exactly one of: "Skill" (Technical/Core Skills), "Lead" (Leadership/Mentoring), "Behav" (Behavioral/Scenario).
                    - l (Level): Must be exactly one of: "Basic" or "Adv".
                    - q (Question): The actual question text.
                """.trimIndent()

//                val message = "Generate interview questions from above images and return json"
                var bufferedText = ""
                var lastUpdateTime = System.currentTimeMillis()

                llmInterface.generateResponseStreaming(
                    prompt = message,
                    images = images
                ).collect { chunk ->
                    bufferedText += chunk
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > 50) {
                        val newText = bufferedText
                        _uiState.update { it.copy(generatedQuestions = newText) }
                        lastUpdateTime = currentTime
                    }
                }

                // Final update to ensure no chunk is missed
                _uiState.update { it.copy(generatedQuestions = bufferedText) }

                _uiState.update { it.copy(isGenerating = false) }
                val jsonString = _uiState.value.generatedQuestions
                Log.d("ScanViewModel", "generateQuestions: $jsonString")
                parseGeneratedQuestions(jsonString)
            } catch (e: Throwable) {
                e.printStackTrace()
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    private fun parseGeneratedQuestions(jsonString: String) {
        try {
            Log.e("ScanViewModel", "parseGeneratedQuestions: $jsonString", )
            val parsedList = mutableListOf<Question>()

            // Try to find a JSON object
            val objStart = jsonString.indexOf("{")
            val objEnd = jsonString.lastIndexOf("}")

            // Try to find a JSON array
            val arrStart = jsonString.indexOf("[")
            val arrEnd = jsonString.lastIndexOf("]")

            val isArrayOuter = arrStart != -1 && arrEnd != -1 && (objStart == -1 || arrStart < objStart) && (objEnd == -1 || arrEnd > objEnd)

            if (isArrayOuter) {
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
                        val question = if (qObj.has("q")) qObj.optString("q", "").trim()
                            .removeSurrounding("\"").trim() else qObj.optString("question", "")
                            .trim().removeSurrounding("\"").trim()
                        val difficulty = if (qObj.has("l")) qObj.optString("l", "") else if (qObj.has("d")) qObj.optString(
                            "d",
                            ""
                        ) else qObj.optString("difficulty", "")
                        val category = if (qObj.has("c")) qObj.optString(
                            "c",
                            ""
                        ) else qObj.optString("category", "")
                        if (question.isNotEmpty()) {
                            parsedList.add(
                                Question(
                                    question = question,
                                    difficulty = difficulty,
                                    category = category
                                )
                            )
                        }
                    }
                }
            } else if (objStart != -1 && objEnd != -1 && objStart < objEnd) {
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

                if (jsonObject != null && (jsonObject.has("q_list") || jsonObject.has("questions"))) {
                    if (jsonObject.has("info")) {
                        try {
                            val infoObj = jsonObject.getJSONObject("info")
                            val name = infoObj.optString("name", "")
                            val designation = infoObj.optString("designation", "")
                            var mobile = infoObj.optString("mobile", "")
                            if (mobile.isEmpty()) {
                                mobile = infoObj.optString("Mobile", "")
                            }
                            if (name.isNotEmpty()) {
                                userFactory.saveInterviewer(Interviewer(name, designation, mobile))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val questionsArray =
                        if (jsonObject.has("q_list")) jsonObject.getJSONArray("q_list") else jsonObject.getJSONArray(
                            "questions"
                        )
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        val question = if (qObj.has("q")) qObj.optString("q", "").trim()
                            .removeSurrounding("\"").trim() else qObj.optString("question", "")
                            .trim().removeSurrounding("\"").trim()
                        val difficulty = if (qObj.has("l")) qObj.optString("l", "") else if (qObj.has("d")) qObj.optString(
                            "d",
                            ""
                        ) else qObj.optString("difficulty", "")
                        val category = if (qObj.has("c")) qObj.optString(
                            "c",
                            ""
                        ) else qObj.optString("category", "")
                        if (question.isNotEmpty()) {
                            parsedList.add(
                                Question(
                                    question = question,
                                    difficulty = difficulty,
                                    category = category
                                )
                            )
                        }
                    }
                }
            }

            if (parsedList.isNotEmpty()) {
                _uiState.update { it.copy(parsedQuestions = parsedList) }
                viewModelScope.launch {
                    _event.send(ScanEffect.NavigateToQuestionPreview)
                }
            } else {
                // Fallback: If parsing totally fails, at least show the raw text as one big question so user sees something happened
                parsedList.add(
                    Question(
                        question = jsonString,
                        difficulty = "Unknown",
                        category = "Unknown"
                    )
                )
                _uiState.update { it.copy(parsedQuestions = parsedList) }
                viewModelScope.launch {
                    _event.send(ScanEffect.NavigateToQuestionPreview)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback on exception
            val fallbackList = listOf(
                Question(
                    question = jsonString,
                    difficulty = "Unknown",
                    category = "Unknown"
                )
            )
            _uiState.update { it.copy(parsedQuestions = fallbackList) }
            viewModelScope.launch {
                _event.send(ScanEffect.NavigateToQuestionPreview)
            }
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

    companion object {
        const val MINIMUM_QUESTIONS = 10
        const val ESTIMATED_ANALYSIS_TIME_SECONDS = 25
    }
}
