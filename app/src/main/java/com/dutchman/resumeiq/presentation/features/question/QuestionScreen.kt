package com.dutchman.resumeiq.presentation.features.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.generated.destinations.QuestionDetailScreenDestination
import androidx.compose.foundation.clickable
import com.ramcosta.composedestinations.generated.destinations.ScanScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MoreScreenDestination
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.ArrowBack
import com.dutchman.resumeiq.presentation.activities.MainEvent
import com.dutchman.resumeiq.presentation.activities.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuestionScreen(
    navigator: DestinationsNavigator,
    mainViewModel: MainViewModel,
    viewModel: QuestionViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val lastSavedIndex by viewModel.lastQuestionIndex.collectAsStateWithLifecycle()
    val interviewer by viewModel.interviewer.collectAsStateWithLifecycle()
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedQuestionIds by remember { mutableStateOf(setOf<Long>()) }

    val isDownloaded by viewModel.isModelDownloaded.collectAsStateWithLifecycle()
    val isInitialized by mainViewModel.isModelInitialized.collectAsStateWithLifecycle()
    val isFabEnabled = !isDownloaded || isInitialized

    LaunchedEffect(key1 = Unit) {
        mainViewModel.onEvent(MainEvent.InitializeModel)
    }

    LaunchedEffect(questions.size) {
        if (questions.isNotEmpty() && listState.firstVisibleItemIndex == 0) {
            if (lastSavedIndex > 0 && lastSavedIndex < questions.size) {
                listState.scrollToItem(lastSavedIndex)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedQuestionIds.size == questions.size && questions.isNotEmpty(),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedQuestionIds = questions.map { it.id }.toSet()
                                    } else {
                                        selectedQuestionIds = emptySet()
                                    }
                                }
                            )
                            Text("Select All", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("${selectedQuestionIds.size} Selected", fontSize = 16.sp)
                        }
                    } else {
                        Text("Questions") 
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            isSelectionMode = false
                            selectedQuestionIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            if (selectedQuestionIds.isNotEmpty()) {
                                viewModel.deleteQuestions(selectedQuestionIds.toList())
                                isSelectionMode = false
                                selectedQuestionIds = emptySet()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete selected")
                        }
                    } else {
                        IconButton(onClick = { navigator.navigate(MoreScreenDestination) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            if (interviewer != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = interviewer?.name ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (interviewer?.designation.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = interviewer?.designation ?: "",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (questions.isEmpty()) {
                item {
                    Text(
                        "No questions saved yet. Scan a resume to generate and save questions.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            } else {
                itemsIndexed(questions.reversed()) { index, question ->
                    QuestionCard(
                        tag = question.category.ifEmpty { "General" },
                        tagColor = MaterialTheme.colorScheme.primaryContainer,
                        tagTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        question = question.question,
                        isLastRead = index == lastSavedIndex && index >= 0,
                        isSelected = selectedQuestionIds.contains(question.id),
                        onClick = {
                            if (isSelectionMode) {
                                if (selectedQuestionIds.contains(question.id)) {
                                    selectedQuestionIds -= question.id
                                    if (selectedQuestionIds.isEmpty()) {
                                        isSelectionMode = false
                                    }
                                } else {
                                    selectedQuestionIds += question.id
                                }
                            } else {
                                viewModel.saveLastQuestionIndex(index)
                                navigator.navigate(QuestionDetailScreenDestination(id = question.id))
                            }
                        },
                        onLongClick = {
                            if (!isSelectionMode) {
                                isSelectionMode = true
                                selectedQuestionIds += question.id
                            }
                        },
                        bottomContent = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    question.difficulty.ifEmpty { "Medium" },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
                }
            }
            
            AnimatedVisibility(
                visible = !listState.isScrollInProgress,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {

                FloatingActionButton(
                    onClick = { 
                        if (isFabEnabled) {
                            navigator.navigate(ScanScreenDestination) 
                        }
                    },
                    containerColor = if (isFabEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isFabEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = CircleShape
                ) {
                    if (isDownloaded && !isInitialized) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Scan")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionCard(
    tag: String,
    tagColor: Color,
    tagTextColor: Color = Color.Black,
    question: String,
    isLastRead: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    bottomContent: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else if (isLastRead) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = tagColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = tagTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                question,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))
            bottomContent()
//            if (isLastRead) {
//                Spacer(modifier = Modifier.height(16.dp))
//                HorizontalDivider(
//                    modifier = Modifier.fillMaxWidth(0.5f),
//                    color = Color(0xFF1661D7),
//                    thickness = 3.dp
//                )
//            }
        }
    }
}

@Composable
fun AITailoredCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "AI Tailored",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "How do you align your engineering decisions with long-term business goals?",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Based on your recent resume scan for the Google Staff Engineer role.",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Strategic",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        "Leadership",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CircleTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape,
        modifier = Modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}