package com.himma.app.features.shell

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.himma.app.R
import com.himma.app.features.onboarding.OnboardingScreen
import com.himma.app.features.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun HimmaRootNavHost(
    onboardingCompleted: Boolean,
) {
    Crossfade(
        targetState = onboardingCompleted,
        label = "RootOnboardingTransition",
    ) { isCompleted ->
        if (!isCompleted) {
            OnboardingScreen()
        } else {
            HimmaAppShell()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HimmaAppShell() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val activeTab = HimmaDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    } ?: HimmaDestination.TODAY

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = activeTab.titleRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                HimmaDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.route == destination.route
                    } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(id = destination.labelRes),
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(id = destination.labelRes),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HimmaDestination.TODAY.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(HimmaDestination.TODAY.route) {
                TodayFoundationScreen()
            }
            composable(HimmaDestination.ROUTINES.route) {
                PlaceholderFeatureScreen(
                    title = stringResource(id = R.string.routines_title),
                    description = stringResource(id = R.string.routines_foundation_desc),
                    icon = HimmaDestination.ROUTINES.icon,
                )
            }
            composable(HimmaDestination.EXERCISES.route) {
                PlaceholderFeatureScreen(
                    title = stringResource(id = R.string.exercises_title),
                    description = stringResource(id = R.string.exercises_foundation_desc),
                    icon = HimmaDestination.EXERCISES.icon,
                )
            }
            composable(HimmaDestination.ANALYTICS.route) {
                PlaceholderFeatureScreen(
                    title = stringResource(id = R.string.analytics_title),
                    description = stringResource(id = R.string.analytics_foundation_desc),
                    icon = HimmaDestination.ANALYTICS.icon,
                )
            }
            composable(HimmaDestination.SETTINGS.route) {
                SettingsScreen(
                    onShowMessage = { msg ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                )
            }
        }
    }
}
