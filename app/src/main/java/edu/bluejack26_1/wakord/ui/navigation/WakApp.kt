package edu.bluejack26_1.wakord.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import edu.bluejack26_1.wakord.ui.components.TopLevelDestinations
import edu.bluejack26_1.wakord.ui.components.WakBottomBar
import edu.bluejack26_1.wakord.ui.feature.auth.LoginRoute
import edu.bluejack26_1.wakord.ui.feature.auth.PreferencesRoute
import edu.bluejack26_1.wakord.ui.feature.auth.RegisterRoute
import edu.bluejack26_1.wakord.ui.feature.auth.ResetPasswordRoute
import edu.bluejack26_1.wakord.ui.feature.auth.SplashScreen
import edu.bluejack26_1.wakord.ui.feature.auth.SplashViewModel
import edu.bluejack26_1.wakord.ui.feature.tuner.TunerRoute
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun WakApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = TopLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        containerColor = WakTheme.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                WakBottomBar(currentRoute = currentRoute, onNavigate = navController::navigateTopLevel)
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.AUTH_GRAPH,
            modifier = Modifier.padding(padding),
        ) {
            authGraph(navController)
            mainGraph(navController)
        }
    }
}

private fun NavGraphBuilder.authGraph(nav: NavController) {
    navigation(startDestination = Routes.SPLASH, route = Routes.AUTH_GRAPH) {
        composable(Routes.SPLASH) {
            val splashViewModel: SplashViewModel = viewModel(factory = SplashViewModel.Factory)
            SplashScreen(onFinished = {
                if (splashViewModel.isLoggedIn) {
                    nav.navigateToMain()
                } else {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.SPLASH) { inclusive = true } }
                }
            })
        }
        composable(Routes.LOGIN) {
            LoginRoute(
                onLoggedIn = nav::navigateToMain,
                onRegister = { nav.navigate(Routes.REGISTER) },
                onForgotPassword = { nav.navigate(Routes.RESET_PASSWORD) },
            )
        }
        composable(Routes.REGISTER) {
            RegisterRoute(
                onRegistered = {
                    nav.navigate(Routes.PREFERENCES) { popUpTo(Routes.REGISTER) { inclusive = true } }
                },
                onBackToLogin = { nav.popBackStack() },
            )
        }
        composable(Routes.PREFERENCES) {
            PreferencesRoute(onDone = nav::navigateToMain)
        }
        composable(Routes.RESET_PASSWORD) {
            ResetPasswordRoute(onBack = { nav.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.mainGraph(nav: NavController) {
    navigation(startDestination = Routes.HOME, route = Routes.MAIN_GRAPH) {
        composable(Routes.HOME) { PlaceholderScreen("Home") }
        composable(Routes.EXPLORE) { PlaceholderScreen("Explore") }
        composable(Routes.TUNER) {
            TunerRoute(onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) })
        }
        composable(Routes.ANALYZE) { PlaceholderScreen("Analyze") }
        composable(Routes.PROFILE) { PlaceholderScreen("Profile") }
        composable(Routes.NOTIFICATIONS) { PlaceholderScreen("Notifications") }
    }
}

fun NavController.navigateToMain() {
    navigate(Routes.MAIN_GRAPH) { popUpTo(Routes.AUTH_GRAPH) { inclusive = true } }
}

fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name, style = MaterialTheme.typography.headlineMedium, color = WakTheme.colors.text)
    }
}
