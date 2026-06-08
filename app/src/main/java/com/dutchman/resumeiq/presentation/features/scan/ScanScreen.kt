package com.dutchman.resumeiq.presentation.features.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ramcosta.composedestinations.annotation.Destination

import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun ScanScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Scan Screen")
    }
}
