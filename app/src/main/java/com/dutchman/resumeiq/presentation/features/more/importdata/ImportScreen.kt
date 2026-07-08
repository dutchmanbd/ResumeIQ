package com.dutchman.resumeiq.presentation.features.more.importdata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ImportScreen(
    navigator: DestinationsNavigator,
    viewModel: ImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onEvent(ImportEvent.OnFileSelected(uri))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Data", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Import Database",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Select a previously exported .db file to merge its questions into your current database.",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select DB File", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (uiState.selectedFileName != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Selected: ${uiState.selectedFileName}",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.onEvent(ImportEvent.OnImportClicked) },
                    modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
                    enabled = !uiState.isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Import Data", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            if (uiState.importSuccess == true) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Import successful!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else if (uiState.importSuccess == false && uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Import failed: ${uiState.errorMessage}", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}
