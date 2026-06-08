package com.dutchman.resumeiq.presentation.features.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ScanScreen() {
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ResumeIQ", fontWeight = FontWeight.Bold, color = Color(0xFF104AAB), fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF104AAB))
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
            
            OptionCard(
                onClick = { showPreview = true },
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
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OptionCard(
                onClick = { showPreview = true },
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
                subtitle = "Point camera at document"
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            if (showPreview) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFF1661D7), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Resume Preview:", fontSize = 16.sp, color = Color(0xFF1A202C))
                    Text("resume_final_v2.pdf", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A202C))
                }
                Surface(
                    color = Color(0xFFE2E8F0),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("4", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Text("Pages", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PagePreviewCard(
                    modifier = Modifier.weight(1f),
                    pageNumber = 1,
                    isSelected = true,
                    placeholderColor = Color(0xFFD6BCA8)
                )
                PagePreviewCard(
                    modifier = Modifier.weight(1f),
                    pageNumber = 2,
                    isSelected = false,
                    placeholderColor = Color(0xFF5A5E61)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PagePreviewCard(
                    modifier = Modifier.weight(1f),
                    pageNumber = 3,
                    isSelected = false,
                    placeholderColor = Color(0xFF717D82)
                )
                PagePreviewCard(
                    modifier = Modifier.weight(1f),
                    pageNumber = 4,
                    isSelected = false,
                    placeholderColor = Color(0xFF4A4E52)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = Color(0xFFEBF3FF),
                border = BorderStroke(1.dp, Color(0xFFB0D0FF)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFF1661D7), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("AI RECOMMENDATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A202C))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("We recommend selecting Page 1 and 2 as they contain your primary work experience and key technical achievements.", fontSize = 13.sp, color = Color(0xFF2D3748), lineHeight = 18.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F62FE))
            ) {
                Icon(Icons.Outlined.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Questions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
}

@Composable
fun OptionCard(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Surface(
        color = Color(0xFFF4F6F9),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A202C))
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun PagePreviewCard(
    modifier: Modifier = Modifier,
    pageNumber: Int,
    isSelected: Boolean,
    placeholderColor: Color
) {
    val borderColor = if (isSelected) Color(0xFF1661D7) else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(placeholderColor)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
    ) {
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
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        
        // Page text at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Page $pageNumber", color = Color.Black.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
