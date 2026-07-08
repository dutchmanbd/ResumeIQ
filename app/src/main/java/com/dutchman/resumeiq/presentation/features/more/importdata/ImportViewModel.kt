package com.dutchman.resumeiq.presentation.features.more.importdata

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dutchman.resumeiq.data.local.AppDatabase
import com.dutchman.resumeiq.data.local.dao.QuestionDao
import com.dutchman.resumeiq.data.local.entity.QuestionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val questionDao: QuestionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportState())
    val uiState: StateFlow<ImportState> = _uiState.asStateFlow()

    fun onEvent(event: ImportEvent) {
        when (event) {
            is ImportEvent.OnFileSelected -> {
                val uri = event.uri
                val fileName = uri?.let { getFileName(it) }
                _uiState.update { 
                    it.copy(selectedFileUri = uri, selectedFileName = fileName, errorMessage = null, importSuccess = null)
                }
            }
            is ImportEvent.OnImportClicked -> {
                val uri = _uiState.value.selectedFileUri
                if (uri != null) {
                    importDatabase(uri)
                } else {
                    _uiState.update { it.copy(errorMessage = "Please select a file first") }
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "selected_db_file"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, errorMessage = null, importSuccess = null) }
            
            try {
                withContext(Dispatchers.IO) {
                    // Copy the selected file to a temporary file in cache
                    val tempFile = File(context.cacheDir, "temp_import.db")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Open the temporary SQLite database
                    val db = SQLiteDatabase.openDatabase(
                        tempFile.absolutePath, 
                        null, 
                        SQLiteDatabase.OPEN_READONLY
                    )

                    // Read questions
                    val cursor = db.query(
                        "questions", 
                        arrayOf("question", "answer", "difficulty", "category"), 
                        null, null, null, null, null
                    )

                    val questionsToInsert = mutableListOf<QuestionEntity>()
                    
                    if (cursor.moveToFirst()) {
                        do {
                            val q = cursor.getString(0) ?: ""
                            val a = cursor.getString(1) ?: ""
                            val d = cursor.getString(2) ?: ""
                            val c = cursor.getString(3) ?: ""
                            
                            questionsToInsert.add(QuestionEntity(
                                id = 0,
                                question = q,
                                answer = a,
                                difficulty = d,
                                category = c
                            ))
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                    db.close()
                    tempFile.delete()

                    // Insert into our Room database
                    if (questionsToInsert.isNotEmpty()) {
                        questionDao.insertQuestions(questionsToInsert)
                    }
                    
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(isImporting = false, importSuccess = true) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isImporting = false, importSuccess = false, errorMessage = e.message) }
                }
            }
        }
    }
}
