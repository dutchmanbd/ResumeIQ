package com.dutchman.resumeiq.presentation.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ModelDownloadScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MoreScreenDestination
import com.ramcosta.composedestinations.generated.destinations.QuestionScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ScanScreenDestination
import com.ramcosta.composedestinations.navigation.dependency

@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute == QuestionScreenDestination.route ||
            currentRoute == MoreScreenDestination.route

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        bottomBar = {
            if (showBottomBar) {
                BottomAppBar(
                    cutoutShape = CircleShape,
                    backgroundColor = MaterialTheme.colorScheme.primary, // Using M3 primary color for the bar
                    elevation = 8.dp
                ) {
                    BottomNavigation(
                        backgroundColor = Color.Transparent,
                        elevation = 0.dp
                    ) {
                        BottomNavigationItem(
                            selected = currentRoute == QuestionScreenDestination.route,
                            onClick = {
                                if (currentRoute != QuestionScreenDestination.route) {
                                    navController.navigate(QuestionScreenDestination.route) {
                                        popUpTo(QuestionScreenDestination.route) {
                                            inclusive = true
                                            saveState = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    Icons.Default.QuestionAnswer,
                                    contentDescription = "Questions",
                                    tint = Color.White
                                )
                            }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        BottomNavigationItem(
                            selected = currentRoute == MoreScreenDestination.route,
                            onClick = {
                                if (currentRoute != MoreScreenDestination.route) {
                                    navController.navigate(MoreScreenDestination.route) {
                                        popUpTo(QuestionScreenDestination.route) {
                                            inclusive = true
                                            saveState = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.White
                                )
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = {
                        if (currentRoute != ScanScreenDestination.route) {
                            navController.navigate(ScanScreenDestination.route) {
                                popUpTo(QuestionScreenDestination.route) {
                                    inclusive = true
                                    saveState = true
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        isFloatingActionButtonDocked = true,
        backgroundColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val start = if (uiState.isLoggedIn) {
            if (uiState.isModelDownloaded) {
                QuestionScreenDestination
            } else {
                ModelDownloadScreenDestination
            }
        } else {
            LoginScreenDestination
        }
        Box(modifier = Modifier.padding(paddingValues)) {
            DestinationsNavHost(
                navGraph = NavGraphs.root,
                start = start,
                navController = navController,
                dependenciesContainerBuilder = {
                    dependency(viewModel)
                }
            )
        }
    }
}