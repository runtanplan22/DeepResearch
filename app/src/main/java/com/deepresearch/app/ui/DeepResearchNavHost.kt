package com.deepresearch.app.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.deepresearch.app.ui.navigation.Routes
import com.deepresearch.app.ui.screens.*
import com.deepresearch.app.viewmodel.ResearchViewModel

/**
 * Top-level navigation host with bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepResearchNavHost() {
    val navController = rememberNavController()
    val viewModel: ResearchViewModel = viewModel()
    val researchState by viewModel.researchState.collectAsState()

    // Determine which bottom nav items to show based on current state
    val navItems = remember(researchState.status) {
        buildList {
            add(BottomNavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, Routes.HOME))
            if (researchState.plan != null || researchState.status.ordinal >= 2) {
                add(BottomNavItem("Plan", Icons.Filled.Assignment, Icons.Outlined.Assignment, Routes.PLAN))
            }
            if (researchState.iterations.isNotEmpty() || researchState.isRunning) {
                add(BottomNavItem("Research", Icons.Filled.Science, Icons.Outlined.Science, Routes.RESEARCH))
            }
            if (researchState.report.isNotBlank()) {
                add(BottomNavItem("Bericht", Icons.Filled.Description, Icons.Outlined.Description, Routes.REPORT))
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (navItems.isNotEmpty()) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    onPlanCreated = {
                        if (researchState.plan != null) {
                            navController.navigate(Routes.PLAN) {
                                popUpTo(Routes.HOME)
                            }
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }
            composable(Routes.PLAN) {
                PlanScreen(
                    viewModel = viewModel,
                    onResearchStarted = {
                        navController.navigate(Routes.RESEARCH) {
                            popUpTo(Routes.HOME)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.RESEARCH) {
                ResearchScreen(
                    viewModel = viewModel,
                    onComplete = {
                        navController.navigate(Routes.REPORT) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }
            composable(Routes.REPORT) {
                ReportScreen(
                    viewModel = viewModel,
                    onNewResearch = {
                        viewModel.resetResearch()
                        navController.navigate(Routes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: String
)
