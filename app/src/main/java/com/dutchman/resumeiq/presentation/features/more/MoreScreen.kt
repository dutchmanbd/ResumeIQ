package com.dutchman.resumeiq.presentation.features.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.dutchman.resumeiq.presentation.activities.MainEvent
import com.dutchman.resumeiq.presentation.activities.MainViewModel
import com.ramcosta.composedestinations.generated.destinations.ImportScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModelDownloadScreenDestination
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun MoreScreen(
    navigator: DestinationsNavigator,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedSetting by remember { mutableStateOf<String?>(null) }

    val currentLanguage = uiState.language
    val currentTheme = uiState.theme
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to logout from your account?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onEvent(MainEvent.Logout)
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = currentTheme,
                isExpanded = expandedSetting == "Theme",
                onClick = { 
                    expandedSetting = if (expandedSetting == "Theme") null else "Theme" 
                }
            ) {
                Column {
                    ThemeOption("System Default", currentTheme) { 
                        viewModel.onEvent(MainEvent.ChangeTheme("System Default"))
                        expandedSetting = null
                    }
                    ThemeOption("Light", currentTheme) { 
                        viewModel.onEvent(MainEvent.ChangeTheme("Light"))
                        expandedSetting = null
                    }
                    ThemeOption("Dark", currentTheme) { 
                        viewModel.onEvent(MainEvent.ChangeTheme("Dark"))
                        expandedSetting = null
                    }
                }
            }

            SettingItem(
                icon = Icons.Default.Language,
                title = "Language",
                subtitle = currentLanguage,
                isExpanded = expandedSetting == "Language",
                onClick = { 
                    expandedSetting = if (expandedSetting == "Language") null else "Language" 
                }
            ) {
                Column {
                    LanguageOption("English", currentLanguage) { 
                        viewModel.onEvent(MainEvent.ChangeLanguage("English"))
                        expandedSetting = null
                    }
                    LanguageOption("Bengali", currentLanguage) { 
                        viewModel.onEvent(MainEvent.ChangeLanguage("Bengali"))
                        expandedSetting = null
                    }
                }
            }

            ActionItem(
                icon = Icons.Default.CloudDownload,
                title = "Model Download",
                subtitle = "Manage AI model for offline use",
                onClick = { navigator.navigate(ModelDownloadScreenDestination(showBackButton = true)) }
            )

            SettingItem(
                icon = Icons.Default.Apps,
                title = "External App",
                subtitle = uiState.externalApp,
                isExpanded = expandedSetting == "ExternalApp",
                onClick = { 
                    expandedSetting = if (expandedSetting == "ExternalApp") null else "ExternalApp" 
                }
            ) {
                Column {
                    AppOption("ChatGPT", uiState.externalApp) { 
                        viewModel.onEvent(MainEvent.ChangeExternalApp("ChatGPT"))
                        expandedSetting = null
                    }
                    AppOption("Gemini", uiState.externalApp) { 
                        viewModel.onEvent(MainEvent.ChangeExternalApp("Gemini"))
                        expandedSetting = null
                    }
                }
            }

            ActionItem(
                icon = Icons.Default.Download,
                title = "Import",
                subtitle = "Import data from a file",
                onClick = { navigator.navigate(ImportScreenDestination) }
            )

            ActionItem(
                icon = Icons.Default.Upload,
                title = "Export",
                subtitle = "Export data to a file",
                onClick = { viewModel.onEvent(MainEvent.ExportData(context, share = false)) }
            )

            ActionItem(
                icon = Icons.Default.Share,
                title = "Export and Share",
                subtitle = "Export data and share it",
                onClick = { viewModel.onEvent(MainEvent.ExportData(context, share = true)) }
            )

            ActionItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Logout",
                subtitle = "Sign out of your account",
                onClick = { showLogoutDialog = true }
            )
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    expandedContent: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(modifier = Modifier.padding(16.dp)) {
                        expandedContent()
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageOption(language: String, selectedLanguage: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = language == selectedLanguage,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(language, fontSize = 16.sp)
    }
}

@Composable
fun ThemeOption(theme: String, selectedTheme: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = theme == selectedTheme,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(theme, fontSize = 16.sp)
    }
}

@Composable
fun AppOption(app: String, selectedApp: String, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = app == selectedApp,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(app, fontSize = 16.sp)
    }
}

@Composable
fun ActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
