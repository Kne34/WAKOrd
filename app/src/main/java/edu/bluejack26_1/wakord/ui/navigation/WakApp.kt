package edu.bluejack26_1.wakord.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.TopLevelDestinations
import edu.bluejack26_1.wakord.ui.components.WakBottomBar
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakButtonVariant
import edu.bluejack26_1.wakord.ui.feature.analyze.AnalysisResultRoute
import edu.bluejack26_1.wakord.ui.feature.analyze.AnalyzeRoute
import edu.bluejack26_1.wakord.ui.feature.analyze.AnalyzeViewModel
import edu.bluejack26_1.wakord.ui.feature.analyze.AnalyzingRoute
import edu.bluejack26_1.wakord.ui.feature.auth.LoginRoute
import edu.bluejack26_1.wakord.ui.feature.auth.PreferencesRoute
import edu.bluejack26_1.wakord.ui.feature.auth.RegisterRoute
import edu.bluejack26_1.wakord.ui.feature.auth.ResetPasswordRoute
import edu.bluejack26_1.wakord.ui.feature.auth.SplashScreen
import edu.bluejack26_1.wakord.ui.feature.auth.SplashViewModel
import edu.bluejack26_1.wakord.ui.feature.explore.ExploreRoute
import edu.bluejack26_1.wakord.ui.feature.explore.ExploreSection
import edu.bluejack26_1.wakord.ui.feature.explore.ExploreSectionRoute
import edu.bluejack26_1.wakord.ui.feature.explore.ExploreViewModel
import edu.bluejack26_1.wakord.ui.feature.home.HomeRoute
import edu.bluejack26_1.wakord.ui.feature.player.MiniPlayerHost
import edu.bluejack26_1.wakord.ui.feature.progression.ProgressionCommentsRoute
import edu.bluejack26_1.wakord.ui.feature.progression.ProgressionDetailRoute
import edu.bluejack26_1.wakord.ui.feature.progression.ProgressionDetailViewModel
import edu.bluejack26_1.wakord.ui.feature.settings.SettingsRoute
import edu.bluejack26_1.wakord.ui.feature.song.SongCommentsRoute
import edu.bluejack26_1.wakord.ui.feature.song.SongDetailRoute
import edu.bluejack26_1.wakord.ui.feature.song.SongDetailViewModel
import edu.bluejack26_1.wakord.ui.feature.training.ExerciseRoute
import edu.bluejack26_1.wakord.ui.feature.training.ExerciseViewModel
import edu.bluejack26_1.wakord.ui.feature.training.TrainingRoute
import edu.bluejack26_1.wakord.ui.feature.training.TrainingStatsRoute
import edu.bluejack26_1.wakord.ui.feature.training.TrainingViewModel
import edu.bluejack26_1.wakord.ui.feature.tuner.TunerRoute
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun WakApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val barRoute = if (currentRoute == Routes.TRAINING) Routes.HOME else currentRoute
    val showBottomBar = TopLevelDestinations.any { it.route == barRoute }

    Scaffold(
        containerColor = WakTheme.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                Column {
                    MiniPlayerHost(
                        visible = currentRoute == Routes.HOME || currentRoute == Routes.PROFILE,
                        onOpenProgression = { navController.navigate(Routes.progression(it)) },
                    )
                    WakBottomBar(currentRoute = barRoute, onNavigate = navController::navigateTopLevel)
                }
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
        composable(Routes.HOME) {
            HomeRoute(
                onOpenSong = { nav.navigate(Routes.song(it)) },
                onOpenProgression = { nav.navigate(Routes.progression(it)) },
                onOpenSearch = { nav.navigateTopLevel(Routes.EXPLORE) },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onOpenProfile = { nav.navigateTopLevel(Routes.PROFILE) },
                onOpenTraining = { nav.navigate(Routes.TRAINING) },
            )
        }
        composable(Routes.EXPLORE) { entry ->
            val search by entry.savedStateHandle.getStateFlow<String?>(EXPLORE_SEARCH, null).collectAsStateWithLifecycle()
            ExploreRoute(
                onOpenSong = { nav.navigate(Routes.song(it)) },
                onOpenProgression = { nav.navigate(Routes.progression(it)) },
                onOpenSection = { nav.navigate(Routes.exploreSection(it.name)) },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                searchQuery = search,
                onSearchConsumed = { entry.savedStateHandle[EXPLORE_SEARCH] = null },
            )
        }
        composable(Routes.EXPLORE_SECTION) { entry ->
            val section = entry.arguments?.getString("section")
                ?.let { name -> ExploreSection.entries.firstOrNull { it.name == name } }
                ?: ExploreSection.TRENDING_SONGS
            val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.EXPLORE) }.getOrDefault(entry) }
            ExploreSectionRoute(
                section = section,
                onBack = { nav.popBackStack() },
                onOpenSong = { nav.navigate(Routes.song(it)) },
                onOpenProgression = { nav.navigate(Routes.progression(it)) },
                viewModel = viewModel(viewModelStoreOwner = owner, factory = ExploreViewModel.Factory),
            )
        }
        composable(Routes.TUNER) {
            TunerRoute(onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) })
        }
        composable(Routes.ANALYZE) {
            AnalyzeRoute(
                onOpenAnalyzing = { nav.navigate(Routes.ANALYZING) { launchSingleTop = true } },
                onOpenResult = { id -> nav.navigate(Routes.analysisResult(id)) { launchSingleTop = true } },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onOpenProgression = { nav.navigate(Routes.progression(it)) },
            )
        }
        composable(Routes.ANALYZING) { entry ->
            val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.ANALYZE) }.getOrDefault(entry) }
            AnalyzingRoute(
                onShowResult = { id ->
                    nav.navigate(Routes.analysisResult(id)) {
                        popUpTo(Routes.ANALYZING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { nav.popBackStack(Routes.ANALYZE, inclusive = false) },
                viewModel = viewModel(viewModelStoreOwner = owner, factory = AnalyzeViewModel.Factory),
            )
        }
        composable(Routes.ANALYSIS_RESULT) { entry ->
            val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.ANALYZE) }.getOrDefault(entry) }
            AnalysisResultRoute(
                onBack = { nav.popBackStack(Routes.ANALYZE, inclusive = false) },
                onOpenSong = { nav.navigate(Routes.song(it)) },
                onOpenProgression = { nav.navigate(Routes.progression(it)) },
                onOpenArtist = nav::openExploreSearch,
                viewModel = viewModel(viewModelStoreOwner = owner, factory = AnalyzeViewModel.Factory),
            )
        }
        composable(Routes.PROFILE) {
            PlaceholderScreen("Profile") {
                WakButton(
                    text = stringResource(R.string.settings_title),
                    onClick = { nav.navigate(Routes.SETTINGS) },
                    variant = WakButtonVariant.Secondary,
                )
            }
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(
                onBack = { nav.popBackStack() },
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.MAIN_GRAPH) { inclusive = true } }
                },
            )
        }
        composable(Routes.NOTIFICATIONS) { PlaceholderScreen("Notifications") }
        composable(Routes.TRAINING) {
            TrainingRoute(
                onBack = { nav.popBackStack() },
                onStartExercise = { nav.navigate(Routes.trainingExercise(it)) },
                onOpenStats = { nav.navigate(Routes.TRAINING_STATS) },
                onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
            )
        }
        composable(
            Routes.TRAINING_EXERCISE,
            arguments = listOf(navArgument(ExerciseViewModel.LEVEL) { type = NavType.IntType }),
        ) {
            ExerciseRoute(
                onBack = { nav.popBackStack() },
                onOpenLevel = { level ->
                    nav.navigate(Routes.trainingExercise(level)) { popUpTo(Routes.TRAINING_EXERCISE) { inclusive = true } }
                },
            )
        }
        composable(Routes.TRAINING_STATS) { entry ->
            val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.TRAINING) }.getOrDefault(entry) }
            TrainingStatsRoute(
                onBack = { nav.popBackStack() },
                viewModel = viewModel(viewModelStoreOwner = owner, factory = TrainingViewModel.Factory),
            )
        }
        composable(Routes.SONG) { entry ->
            val songId = entry.arguments?.getString(SongDetailViewModel.SONG_ID).orEmpty()
            SongDetailRoute(
                onBack = { nav.popBackStack() },
                onOpenComments = { nav.navigate(Routes.songComments(songId)) },
            )
        }
        composable(Routes.SONG_COMMENTS) { entry ->
            val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.SONG) }.getOrDefault(entry) }
            SongCommentsRoute(
                onBack = { nav.popBackStack() },
                viewModel = viewModel(viewModelStoreOwner = owner, factory = SongDetailViewModel.Factory),
            )
        }
        composable(Routes.PROGRESSION) { entry ->
            val progressionId = entry.arguments?.getString(ProgressionDetailViewModel.PROGRESSION_ID).orEmpty()
            ProgressionDetailRoute(
                onBack = { nav.popBackStack() },
                onOpenComments = { nav.navigate(Routes.progressionComments(progressionId)) },
            )
        }
        composable(Routes.PROGRESSION_COMMENTS) { entry ->
            val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.PROGRESSION) }.getOrDefault(entry) }
            ProgressionCommentsRoute(
                onBack = { nav.popBackStack() },
                viewModel = viewModel(viewModelStoreOwner = owner, factory = ProgressionDetailViewModel.Factory),
            )
        }
    }
}

fun NavController.navigateToMain() {
    navigate(Routes.MAIN_GRAPH) { popUpTo(Routes.AUTH_GRAPH) { inclusive = true } }
}

private const val EXPLORE_SEARCH = "exploreSearch"

fun NavController.openExploreSearch(query: String) {
    navigateTopLevel(Routes.EXPLORE)
    currentBackStackEntry?.savedStateHandle?.set(EXPLORE_SEARCH, query)
}

fun NavController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun PlaceholderScreen(name: String, action: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, style = MaterialTheme.typography.headlineMedium, color = WakTheme.colors.text)
        action()
    }
}
