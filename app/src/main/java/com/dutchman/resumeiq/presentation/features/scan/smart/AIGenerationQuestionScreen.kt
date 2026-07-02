package com.dutchman.resumeiq.presentation.features.scan.smart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dutchman.resumeiq.domain.util.rememberSharedBackStackEntry
import com.dutchman.resumeiq.presentation.features.scan.ScanEffect
import com.dutchman.resumeiq.presentation.features.scan.ScanEvent
import com.dutchman.resumeiq.presentation.features.scan.ScanViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.QuestionPreviewScreenDestination
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun AIGenerationQuestionScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
) {
    val viewModel: ScanViewModel = hiltViewModel(navController.rememberSharedBackStackEntry())
    val uiState by viewModel.uiState.collectAsState()
    
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.onEvent(ScanEvent.OnMicrophonePermissionResult(isGranted))
            if (isGranted) {
                viewModel.onEvent(ScanEvent.OnSpeechMicToggle)
            }
        }
    )
    
    // Check initial permission state
    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onEvent(ScanEvent.OnMicrophonePermissionResult(isGranted))
    }
    
    // Sync speech text to prompt
    LaunchedEffect(uiState.speechPartialText) {
        if (uiState.isSpeechRecording && uiState.speechPartialText.isNotEmpty()) {
            viewModel.onEvent(ScanEvent.OnPromptTextChanged(uiState.speechPartialText))
        }
    }

    LaunchedEffect(viewModel.event) {
        viewModel.event.collectLatest { effect ->
            when (effect) {
                is ScanEffect.NavigateToQuestionPreview -> {
                    if (uiState.parsedQuestions.isNotEmpty()) {
                        navigator.navigate(QuestionPreviewScreenDestination)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate with AI") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(Color(0xFFF8F9FA))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = if (uiState.generatedQuestions.isNotEmpty()) Alignment.TopStart else Alignment.Center
            ) {
                if (uiState.generatedQuestions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = uiState.generatedQuestions,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                } else if (uiState.isGenerating) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        text = "What kind of questions would you like to generate?",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // MiniDoctor-style Input Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Plus Button
//                    IconButton(
//                        onClick = { /* TODO attach file */ },
//                        modifier = Modifier.size(36.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(28.dp)
//                                .background(
//                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
//                                    CircleShape
//                                ),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Icon(
//                                imageVector = Icons.Default.Add,
//                                contentDescription = "Add",
//                                tint = MaterialTheme.colorScheme.onSurface,
//                                modifier = Modifier.size(20.dp)
//                            )
//                        }
//                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.promptText.isEmpty() && !uiState.isSpeechRecording) {
                            Text(
                                text = "Ask anything...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        } else if (uiState.isSpeechRecording && uiState.promptText.isEmpty()) {
                             Text(
                                text = "Listening...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        BasicTextField(
                            value = uiState.promptText,
                            onValueChange = { viewModel.onEvent(ScanEvent.OnPromptTextChanged(it)) },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 5
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (uiState.isSpeechRecording) {
                        IconButton(
                            onClick = { viewModel.onEvent(ScanEvent.OnSpeechMicToggle) },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    } else if (uiState.promptText.isEmpty()) {
                        IconButton(
                            onClick = {
                                if (uiState.isMicrophonePermissionGranted) {
                                    viewModel.onEvent(ScanEvent.OnSpeechMicToggle)
                                } else {
                                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { 
                                focusManager.clearFocus()
                                if (!uiState.isGenerating) {
                                    viewModel.onEvent(ScanEvent.OnGenerateQuestionsFromPrompt)
                                } else {
                                    // Optionally handle cancellation here if viewmodel supports it
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(if (uiState.isGenerating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isGenerating) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Send",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
