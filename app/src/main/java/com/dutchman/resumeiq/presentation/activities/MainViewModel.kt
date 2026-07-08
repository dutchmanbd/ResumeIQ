package com.dutchman.resumeiq.presentation.activities

import androidx.lifecycle.ViewModel
import com.dutchman.resumeiq.domain.util.FileStorage
import com.dutchman.resumeiq.domain.util.UserFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.dutchman.resumeiq.domain.util.FirebaseAuthHelper
import com.dutchman.resumeiq.data.local.AppDatabase
import androidx.core.content.FileProvider
import android.os.Environment
import android.widget.Toast
import android.content.Intent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userFactory: UserFactory,
    private val fileStorage: FileStorage,
    private val firebaseAuthHelper: FirebaseAuthHelper,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(
        isModelDownloaded = userFactory.isModelDownloaded,
        isSkip = userFactory.isSkip,
        isLoggedIn = firebaseAuthHelper.isUserLoggedIn(),
        theme = userFactory.theme,
        language = userFactory.language,
        externalApp = userFactory.externalApp
    ))
    val uiState: StateFlow<MainUiState>
        get() = _uiState.asStateFlow()



    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.RefreshModel -> {
                _uiState.update { state ->
                    state.copy(
                        isModelDownloaded = true
                    )
                }
            }

            MainEvent.SignInAnonymously -> {
                viewModelScope.launch {
                    val success = firebaseAuthHelper.signInAnonymously()
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                isLoggedIn = true
                            )
                        }
                    }
                }
            }

            is MainEvent.SignInWithGoogle -> {
                // Now handled by signInWithGoogle(context) method below
                viewModelScope.launch {
                    val success = firebaseAuthHelper.signInWithGoogle(event.context)
                    if (success) {
                        _uiState.update { state ->
                            state.copy(
                                isLoggedIn = true
                            )
                        }
                    }
                }
            }
            
            MainEvent.Logout -> {
                firebaseAuthHelper.logout()
                userFactory.saveIsSkip(false)
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = false,
                        isSkip = false
                    )
                }
            }

            is MainEvent.ChangeTheme -> {
                userFactory.saveTheme(event.theme)
                _uiState.update { state ->
                    state.copy(
                        theme = event.theme
                    )
                }
            }

            is MainEvent.ChangeLanguage -> {
                userFactory.saveLanguage(event.language)
                _uiState.update { state ->
                    state.copy(
                        language = event.language
                    )
                }
            }

            is MainEvent.ChangeExternalApp -> {
                userFactory.saveExternalApp(event.appName)
                _uiState.update { state ->
                    state.copy(
                        externalApp = event.appName
                    )
                }
            }

            is MainEvent.ExportData -> {
                val context = event.context
                viewModelScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            appDatabase.checkpoint()
                            val dbFile = context.getDatabasePath(AppDatabase.DATABASE_NAME)
                            
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            val fileName = "ResumeIQ_$timestamp.db"
                            
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val exportFile = File(downloadsDir, fileName)
                            
                            dbFile.copyTo(exportFile, overwrite = true)
                            
                            withContext(Dispatchers.Main) {
                                if (event.share) {
                                    // For sharing, we need to copy it to a cache path compatible with FileProvider
                                    val cacheFile = File(File(context.cacheDir, "shared_db").apply { mkdirs() }, fileName)
                                    exportFile.copyTo(cacheFile, overwrite = true)
                                    
                                    val uri = FileProvider.getUriForFile(
                                        context, 
                                        "${context.packageName}.provider", 
                                        cacheFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = Intent.createChooser(intent, "Share Database")
                                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(chooser)
                                } else {
                                    Toast.makeText(context, "Exported to Downloads: $fileName", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}