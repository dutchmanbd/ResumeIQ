package com.dutchman.resumeiq.presentation.features.scan

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.dutchman.resumeiq.domain.util.rememberSharedBackStackEntry
import com.dutchman.resumeiq.presentation.features.scan.preview.QuestionPreviewScreen
import com.ramcosta.composedestinations.generated.destinations.AIGenerationQuestionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuestionPreviewScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuickQuestionScreenDestination
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ScanScreen(
    navigator: DestinationsNavigator,
    navController: NavController,
) {
    val viewModel: ScanViewModel = hiltViewModel(navController.rememberSharedBackStackEntry())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.onEvent(ScanEvent.OnFileSelected(it, context))
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.onEvent(ScanEvent.OnJsonFileSelected(it, context))
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

    if (uiState.selectedImageBitmap != null) {
        Dialog(onDismissRequest = { viewModel.onEvent(ScanEvent.OnSelectedImageBitmapChanged(null)) }) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = maxOf(1f, minOf(scale * zoom, 5f))
                            val maxX = (size.width * (scale - 1)) / 2
                            val maxY = (size.height * (scale - 1)) / 2
                            offset = Offset(
                                x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = uiState.selectedImageBitmap!!.asImageBitmap(),
                    contentDescription = "Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Generate Question",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navigator.navigateUp()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Extract Questions from Resume",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A202C),
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select how you want to provide your professional experience for analysis.",
                fontSize = 14.sp,
                color = Color(0xFF4A5568),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { launcher.launch(arrayOf("application/pdf", "image/*")) },
                    icon = {
                        Surface(
                            color = Color(0xFF1661D7),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Upload, contentDescription = null, tint = Color.White)
                            }
                        }
                    },
                    title = "Upload Resume",
                    subtitle = "PDF or Image"
                )
                
                OptionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { /* Handle camera scan */ },
                    icon = {
                        Surface(
                            color = Color(0xFF86F286),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF1A5A1A))
                            }
                        }
                    },
                    title = "Scan Resume",
                    subtitle = "Point camera"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { jsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                    icon = {
                        Surface(
                            color = Color(0xFFFCD34D),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFB45309))
                            }
                        }
                    },
                    title = "Upload JSON",
                    subtitle = "Select JSON file"
                )

                OptionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onEvent(ScanEvent.OnShowPasteDialogChanged(true)) },
                    icon = {
                        Surface(
                            color = Color(0xFFE2E8F0),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF4A5568))
                            }
                        }
                    },
                    title = "Paste JSON",
                    subtitle = "Paste raw JSON"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { navigator.navigate(AIGenerationQuestionScreenDestination) },
                    icon = {
                        Surface(
                            color = Color(0xFFE0E7FF),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF4F46E5))
                            }
                        }
                    },
                    title = "AI Smart Gen",
                    subtitle = "Full questions set"
                )

                OptionCard(
                    modifier = Modifier.weight(1f),
                    onClick = { navigator.navigate(QuickQuestionScreenDestination) },
                    icon = {
                        Surface(
                            color = Color(0xFFFCE7F3),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.QuestionMark, contentDescription = null, tint = Color(0xFFDB2777))
                            }
                        }
                    },
                    title = "Quick Question",
                    subtitle = "Single targeted"
                )
            }

            if (uiState.showPreview) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = Color(0xFF1661D7),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Resume Preview:", fontSize = 16.sp, color = Color(0xFF1A202C))
                        Text(
                            uiState.fileName.ifEmpty { "resume_final_v2.pdf" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A202C)
                        )
                    }
                    Surface(
                        color = Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                uiState.pageCount.toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Text("Pages", fontSize = 10.sp, color = Color.DarkGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1661D7))
                    }
                } else if (uiState.previewImages.isNotEmpty()) {
                    val images = uiState.previewImages
                    images.chunked(2).forEachIndexed { chunkIndex, chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (i in 0 until 2) {
                                val imageIndex = chunkIndex * 2 + i
                                val bitmap = chunk.getOrNull(i)
                                val isSelected = uiState.selectedPages.contains(imageIndex)

                                if (bitmap != null) {
                                    PagePreviewCard(
                                        modifier = Modifier.weight(1f),
                                        pageNumber = imageIndex + 1,
                                        isSelected = isSelected,
                                        bitmap = bitmap,
                                        onClick = { viewModel.onEvent(ScanEvent.OnSelectedImageBitmapChanged(bitmap)) },
                                        onLongClick = {
                                            viewModel.onEvent(ScanEvent.OnPageSelectionToggled(imageIndex))
                                        }
                                    )
                                } else if (imageIndex < uiState.pageCount.coerceAtMost(10)) {
                                    // Placeholder if image is missing but it's within count
                                    PagePreviewCard(
                                        modifier = Modifier.weight(1f),
                                        pageNumber = imageIndex + 1,
                                        isSelected = false,
                                        bitmap = null,
                                        onClick = {},
                                        onLongClick = {}
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = Color(0xFFEBF3FF),
                    border = BorderStroke(1.dp, Color(0xFFB0D0FF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF1661D7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "AI RECOMMENDATION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A202C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "We recommend selecting Page 1 and 2 as they contain your primary work experience and key technical achievements.",
                                fontSize = 13.sp,
                                color = Color(0xFF2D3748),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val selectedBitmaps = uiState.previewImages.filterIndexed { index, _ ->
                            uiState.selectedPages.contains(index)
                        }
                        viewModel.onEvent(ScanEvent.OnGenerateQuestionsClicked(selectedBitmaps))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE)),
                    enabled = !uiState.isGenerating
                ) {
                    if (uiState.isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Psychology,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (uiState.isGenerating) "Generating..." else "Generate Questions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.generatedQuestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Generated Questions:",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A202C)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                uiState.generatedQuestions,
                                fontSize = 14.sp,
                                color = Color(0xFF2D3748)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Estimated analysis time: 15 seconds",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (uiState.showPasteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(ScanEvent.OnShowPasteDialogChanged(false)) },
            title = { Text("Paste JSON", fontWeight = FontWeight.Bold) },
            text = {
                Text("Do you want to paste JSON text from your clipboard and parse it?", fontSize = 16.sp)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(ScanEvent.OnShowPasteDialogChanged(false))
                        coroutineScope.launch {
                            val clipEntry = clipboard.getClipEntry()
                            val clipData = clipEntry?.clipData
                            val clipText = if (clipData != null && clipData.itemCount > 0) {
                                clipData.getItemAt(0).text?.toString() ?: ""
                            } else ""
                            viewModel.onEvent(ScanEvent.OnJsonTextPasted(clipText))
                        }
                    }
                ) {
                    Text("Okay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(ScanEvent.OnShowPasteDialogChanged(false)) }) {
                    Text("Cancel")
                }
            }
        )
    }


}

@Composable
fun OptionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Surface(
        color = Color(0xFFF4F6F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A202C)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 10.sp,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagePreviewCard(
    modifier: Modifier = Modifier,
    pageNumber: Int,
    isSelected: Boolean,
    bitmap: Bitmap? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Only apply border if selected AND we have a valid image for this slot, or we just keep it simple.
    // The design shows the border regardless, assuming pages exist.
    // However, if there's no bitmap, we shouldn't show the checkmark or border since it's an empty slot.
    val hasContent = bitmap != null
    val borderColor = if (isSelected && hasContent) Color(0xFF1661D7) else Color.Transparent
    val borderWidth = if (isSelected && hasContent) 2.dp else 0.dp

    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE2E8F0))
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .combinedClickable(
                enabled = hasContent,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page $pageNumber",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (hasContent) {
            // Selection circle
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFF1661D7) else Color.LightGray.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Page text at bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Page $pageNumber",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            // Empty state placeholder text
            Text(
                "No Page",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

