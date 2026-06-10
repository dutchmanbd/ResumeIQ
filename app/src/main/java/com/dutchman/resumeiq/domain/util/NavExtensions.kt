package com.dutchman.resumeiq.domain.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.ramcosta.composedestinations.generated.NavGraphs

@Composable
fun NavController.rememberSharedBackStackEntry(): NavBackStackEntry {
    return remember(this.currentBackStackEntry) {
        this.getBackStackEntry(NavGraphs.root.route)
    }
}