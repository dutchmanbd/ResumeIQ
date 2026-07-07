package com.dutchman.resumeiq.presentation.features.question.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Intent
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.dutchman.resumeiq.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuestionDetailScreen(
    id: Long,
    navigator: DestinationsNavigator,
    viewModel: QuestionDetailViewModel = hiltViewModel()
) {
    val question by viewModel.question.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generatedAnswer by viewModel.generatedAnswer.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showPasteDialog by remember { mutableStateOf(false) }

    val externalApp = viewModel.externalApp
    val externalAppPackage = if (externalApp == "Gemini") "com.google.android.apps.bard" else "com.openai.chatgpt"
    val externalAppIcon = if (externalApp == "Gemini") R.drawable.ic_gemini else R.drawable.ic_chatgpt

    val openExternalApp = {
        val qText = question?.question ?: ""
        try {
            val processIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_PROCESS_TEXT, qText)
                putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                setPackage(externalAppPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(processIntent)
        } catch (e: Exception) {
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, qText)
                    setPackage(externalAppPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(sendIntent)
            } catch (e2: Exception) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$externalAppPackage")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                } catch (e3: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$externalAppPackage")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        }
    }

    if (showPasteDialog) {
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Paste Answer") },
            text = { Text("Are you sure you want to paste the answer from your clipboard? This will overwrite your current answer.") },
            confirmButton = {
                TextButton(onClick = {
                    val clipboardText = clipboardManager.getText()?.text
                    if (!clipboardText.isNullOrEmpty()) {
                        viewModel.updateAnswer(clipboardText)
                    }
                    showPasteDialog = false
                }) {
                    Text("Okay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Answer", fontWeight = FontWeight.Medium, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // User Question Bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = question?.question ?: "Loading...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = {
                                question?.question?.let {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Question",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (question?.answer.isNullOrEmpty() && !isGenerating) {
                // Empty state with dashed border
                val outlineVariant = MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawRoundRect(
                                color = outlineVariant,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                                )
                            )
                        }
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Generate a tailored response based on your resume and the STAR method (Situation, Task, Action, Result).",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.generateAiAnswer() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Generate AI Answer", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add Paste button below empty state
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showPasteDialog = true }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste Answer from Clipboard")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = openExternalApp,
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = externalAppIcon),
                            contentDescription = "Open in $externalApp",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (isGenerating) {
                // Generating state
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Generating...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    dev.jeziellago.compose.markdowntext.MarkdownText(
                        markdown = generatedAnswer,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = androidx.compose.material3.LocalTextStyle.current.copy(lineHeight = 26.sp)
                    )
                }
            } else {
                // Answer filled state
                Column(modifier = Modifier.fillMaxWidth()) {
                    dev.jeziellago.compose.markdowntext.MarkdownText(
                        markdown = question?.answer ?: "",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = androidx.compose.material3.LocalTextStyle.current.copy(lineHeight = 26.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showPasteDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = "Paste Answer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = openExternalApp,
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = externalAppIcon),
                                contentDescription = "Open in $externalApp",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
