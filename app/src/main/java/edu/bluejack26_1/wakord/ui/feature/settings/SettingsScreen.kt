package edu.bluejack26_1.wakord.ui.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.SyncStatus
import edu.bluejack26_1.wakord.domain.model.ThemeMode
import edu.bluejack26_1.wakord.ui.components.DialogTone
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakButtonVariant
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.relativeTime
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    languageCaption: String,
    appVersion: String,
    pushOn: Boolean,
    onBack: () -> Unit,
    onOpenLanguage: () -> Unit,
    onPushChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassIconButton(
                painterResource(R.drawable.ic_back),
                stringResource(R.string.back),
                onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(stringResource(R.string.settings_title), style = spaceGrotesk(16.sp), color = c.text)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            SectionLabel(stringResource(R.string.settings_appearance))
            ThemeSelector(
                selected = when (state.themeMode) {
                    ThemeMode.SYSTEM -> if (c.isDark) ThemeMode.DARK else ThemeMode.LIGHT
                    else -> state.themeMode
                },
                onSelect = { onAction(SettingsAction.SelectTheme(it)) },
            )
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_general))
            SettingsCard {
                SettingsRow(
                    icon = painterResource(R.drawable.ic_globe),
                    title = stringResource(R.string.settings_language),
                    onClick = onOpenLanguage,
                ) {
                    Caption(languageCaption)
                    Chevron()
                }
                CardDivider()
                SettingsRow(
                    icon = painterResource(R.drawable.ic_tuning_fork),
                    title = stringResource(R.string.settings_reference_pitch),
                    onClick = { onAction(SettingsAction.OpenReferencePitch) },
                ) {
                    Text(
                        stringResource(R.string.settings_pitch_value, state.referencePitch.roundToInt()),
                        style = spaceGrotesk(12.sp),
                        color = c.text3,
                    )
                    Chevron()
                }
                CardDivider()
                SwitchRow(
                    icon = painterResource(R.drawable.ic_bell),
                    title = stringResource(R.string.settings_push),
                    checked = pushOn,
                    onCheckedChange = onPushChange,
                )
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel(stringResource(R.string.settings_offline))
            SettingsCard {
                SyncRow(state.sync)
                CardDivider()
                SwitchRow(
                    icon = painterResource(R.drawable.ic_bookmark),
                    title = stringResource(R.string.settings_keep_offline),
                    checked = state.keepOffline,
                    onCheckedChange = { onAction(SettingsAction.SetKeepOffline(it)) },
                )
            }
            Spacer(Modifier.height(32.dp))
            WakButton(
                text = stringResource(R.string.settings_log_out),
                onClick = { onAction(SettingsAction.RequestLogout) },
                modifier = Modifier.fillMaxWidth(),
                variant = WakButtonVariant.Destructive,
                leadingIcon = painterResource(R.drawable.ic_logout),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.settings_footer, appVersion),
                style = jakarta(12.sp),
                color = c.text3,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.isPitchSheetOpen) {
        ReferencePitchSheet(
            pitch = state.referencePitch.roundToInt(),
            onChange = { onAction(SettingsAction.SetReferencePitch(it.toFloat())) },
            onClose = { onAction(SettingsAction.CloseReferencePitch) },
        )
    }

    if (state.isLogoutDialogOpen) {
        WakDialog(
            title = stringResource(R.string.logout_title),
            message = stringResource(R.string.logout_message),
            primaryText = stringResource(R.string.settings_log_out),
            onPrimary = { onAction(SettingsAction.ConfirmLogout) },
            onDismissRequest = { onAction(SettingsAction.DismissLogout) },
            secondaryText = stringResource(R.string.cancel),
            icon = painterResource(R.drawable.ic_logout),
            tone = DialogTone.Destructive,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp, lineHeight = 14.sp),
        color = WakTheme.colors.text3,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(c.s3, RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ThemeOption(
            icon = painterResource(R.drawable.ic_moon),
            label = stringResource(R.string.settings_theme_dark),
            selected = selected == ThemeMode.DARK,
            onClick = { onSelect(ThemeMode.DARK) },
            modifier = Modifier.weight(1f),
        )
        ThemeOption(
            icon = painterResource(R.drawable.ic_sun),
            label = stringResource(R.string.settings_theme_light),
            selected = selected == ThemeMode.LIGHT,
            onClick = { onSelect(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ThemeOption(icon: Painter, label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val container by animateColorAsState(if (selected) c.s1 else Color.Transparent, label = "themeOption")
    val content = if (selected) c.text else c.text3
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(container, RoundedCornerShape(12.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = jakarta(13.sp), color = content)
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val c = WakTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(horizontal = 17.dp, vertical = 1.dp), content = content)
    }
}

@Composable
private fun CardDivider() {
    HorizontalDivider(thickness = 1.dp, color = WakTheme.colors.line)
}

@Composable
private fun SettingsRow(
    icon: Painter,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.text2, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = jakarta(13.sp), color = c.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { trailing() }
    }
}

@Composable
private fun Caption(text: String) {
    Text(text, style = jakarta(12.sp), color = WakTheme.colors.text3, maxLines = 1)
}

@Composable
private fun Chevron() {
    Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = WakTheme.colors.text3, modifier = Modifier.size(16.dp))
}

@Composable
private fun SwitchRow(icon: Painter, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SettingsRow(
        icon = icon,
        title = title,
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        WakSwitch(checked)
    }
}

@Composable
private fun WakSwitch(checked: Boolean) {
    val c = WakTheme.colors
    val track by animateColorAsState(if (checked) c.primary else c.s5, label = "switchTrack")
    val thumb by animateColorAsState(if (checked) c.onPrimary else c.text3, label = "switchThumb")
    val offset by animateDpAsState(if (checked) 23.dp else 3.dp, label = "switchThumbOffset")
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 30.dp)
            .background(track, CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset(offset.roundToPx(), 0) }
                .size(24.dp)
                .background(thumb, CircleShape),
        )
    }
}

@Composable
private fun SyncRow(sync: SyncStatus) {
    val c = WakTheme.colors
    val songs = pluralStringResource(R.plurals.settings_saved_songs, sync.savedSongs, sync.savedSongs)
    val chords = pluralStringResource(R.plurals.settings_saved_chords, sync.savedChords, sync.savedChords)
    val synced = sync.lastSyncedAt
    val recent = synced != null && System.currentTimeMillis() - synced < 60_000
    val time = when {
        synced == null -> stringResource(R.string.settings_saved_on_phone)
        sync.isOnline && recent -> stringResource(R.string.settings_just_now)
        recent -> stringResource(R.string.settings_last_synced, stringResource(R.string.settings_just_now))
        else -> stringResource(R.string.settings_last_synced, stringResource(R.string.time_ago, relativeTime(synced)))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (sync.isOnline) R.drawable.ic_check_circle else R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = if (sync.isOnline) c.primaryText else c.peachText,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (sync.isOnline) R.string.settings_synced else R.string.settings_waiting_sync),
                style = jakarta(13.sp),
                color = c.text,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.settings_sync_detail, songs, chords, time),
                style = jakarta(12.sp),
                color = c.text3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "Settings", heightDp = 900)
@Composable
private fun SettingsPreview() {
    WAKordTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(
                themeMode = ThemeMode.DARK,
                sync = SyncStatus(savedSongs = 12, savedChords = 24, isOnline = true, lastSyncedAt = System.currentTimeMillis()),
            ),
            onAction = {},
            languageCaption = "Follow system · English",
            appVersion = "1.0",
            pushOn = true,
            onBack = {},
            onOpenLanguage = {},
            onPushChange = {},
        )
    }
}

@Preview(name = "Settings - light, offline", heightDp = 900)
@Composable
private fun SettingsLightPreview() {
    WAKordTheme(darkTheme = false) {
        SettingsScreen(
            state = SettingsUiState(
                themeMode = ThemeMode.LIGHT,
                keepOffline = false,
                sync = SyncStatus(savedSongs = 1, savedChords = 3, isOnline = false, lastSyncedAt = System.currentTimeMillis() - 3_600_000),
            ),
            onAction = {},
            languageCaption = "Follow system · English",
            appVersion = "1.0",
            pushOn = false,
            onBack = {},
            onOpenLanguage = {},
            onPushChange = {},
        )
    }
}

@Preview(name = "Settings - log out")
@Composable
private fun SettingsLogoutPreview() {
    WAKordTheme(darkTheme = true) {
        SettingsScreen(
            state = SettingsUiState(isLogoutDialogOpen = true),
            onAction = {},
            languageCaption = "Follow system · English",
            appVersion = "1.0",
            pushOn = true,
            onBack = {},
            onOpenLanguage = {},
            onPushChange = {},
        )
    }
}
