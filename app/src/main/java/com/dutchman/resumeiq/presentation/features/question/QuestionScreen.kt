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
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun QuestionScreen() {
    Scaffold(
        containerColor = Color(0xFFF4F6F9)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                QuestionCard(
                    tag = "Behavioral",
                    tagColor = Color(0xFF86F286),
                    tagTextColor = Color(0xFF1A5A1A),
                    question = "Tell me about a time you handled conflict with a team member.",
                    bottomContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFFFF0D4),
                                shape = CircleShape,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF2A900), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("High Frequency", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                )
            }
            item {
                QuestionCard(
                    tag = "Experience",
                    tagColor = Color(0xFFB58B00),
                    tagTextColor = Color.White,
                    question = "Describe the most complex architectural challenge you've solved.",
                    bottomContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Advanced Level", fontSize = 12.sp, color = Color.DarkGray)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF1061E3), modifier = Modifier.size(18.dp))
                        }
                    }
                )
            }
            item {
                QuestionCard(
                    tag = "Self-Awareness",
                    tagColor = Color(0xFF86F286),
                    tagTextColor = Color(0xFF1A5A1A),
                    question = "What is your greatest professional weakness and how are you addressing it?",
                    bottomContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E8B57), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("3 practices completed", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                )
            }
            item {
                QuestionCard(
                    tag = "Leadership",
                    tagColor = Color(0xFFB58B00),
                    tagTextColor = Color.White,
                    question = "Tell me about a time you led a team through a period of ambiguity.",
                    bottomContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleTag("L")
                            Spacer(modifier = Modifier.width(6.dp))
                            CircleTag("6")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun QuestionCard(
    tag: String,
    tagColor: Color,
    tagTextColor: Color = Color.Black,
    question: String,
    bottomContent: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
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
                    Text(tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 10.sp, color = tagTextColor, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(question, fontSize = 17.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp, color = Color(0xFF1A202C))
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEDF2F7), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))
            bottomContent()
        }
    }
}

@Composable
fun AITailoredCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1661D7)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI Tailored", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text("How do you align your engineering decisions with long-term business goals?", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Based on your recent resume scan for the Google Staff Engineer role.", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(18.dp))
            Row {
                Surface(color = Color(0xFF4B8DF8), shape = RoundedCornerShape(16.dp)) {
                    Text("Strategic", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(color = Color(0xFF4B8DF8), shape = RoundedCornerShape(16.dp)) {
                    Text("Leadership", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CircleTag(text: String) {
    Surface(
        color = Color(0xFFE2E8F0),
        shape = CircleShape,
        modifier = Modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A202C))
        }
    }
}