package edu.bluejack26_1.wakord.ui.feature.settings

import android.Manifest
import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.bluejack26_1.wakord.R

private fun Context.hasNotificationPermission() =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun Context.followsSystemLanguage() =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        getSystemService(LocaleManager::class.java).applicationLocales.isEmpty

private fun Context.openSafely(vararg intents: Intent) {
    for (intent in intents) {
        try {
            startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
        }
    }
}

private fun Context.openLanguageSettings() {
    val appLocales = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", packageName, null))
    } else {
        null
    }
    openSafely(*listOfNotNull(appLocales, Intent(Settings.ACTION_LOCALE_SETTINGS)).toTypedArray())
}

private fun Context.openNotificationSettings() {
    openSafely(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(context.hasNotificationPermission()) }
    var followsSystem by remember { mutableStateOf(context.followsSystemLanguage()) }
    LifecycleResumeEffect(Unit) {
        hasPermission = context.hasNotificationPermission()
        followsSystem = context.followsSystemLanguage()
        onPauseOrDispose {}
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) viewModel.onAction(SettingsAction.SetPushEnabled(true)) else context.openNotificationSettings()
    }
    LaunchedEffect(state.isLoggedOut) {
        if (state.isLoggedOut) onLoggedOut()
    }

    val locale = LocalConfiguration.current.locales.get(0)
    val language = locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
    val appVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    SettingsScreen(
        state = state,
        onAction = viewModel::onAction,
        languageCaption = if (followsSystem) stringResource(R.string.settings_language_follow, language) else language,
        appVersion = appVersion,
        pushOn = state.pushEnabled && hasPermission,
        onBack = onBack,
        onOpenLanguage = context::openLanguageSettings,
        onPushChange = { on ->
            if (on && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onAction(SettingsAction.SetPushEnabled(on))
            }
        },
    )
}
