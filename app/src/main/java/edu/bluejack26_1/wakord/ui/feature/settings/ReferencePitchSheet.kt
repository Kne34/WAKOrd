package edu.bluejack26_1.wakord.ui.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.SheetHeader
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakButtonVariant
import edu.bluejack26_1.wakord.ui.components.WakChip
import edu.bluejack26_1.wakord.ui.components.WakSheet
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private val PitchPresets = listOf(432, 438, 440, 442, 444)

@Composable
fun ReferencePitchSheet(pitch: Int, onChange: (Int) -> Unit, onClose: () -> Unit) {
    WakSheet(onDismiss = onClose) {
        ReferencePitchContent(pitch, onChange, onClose)
    }
}

@Composable
private fun ReferencePitchContent(pitch: Int, onChange: (Int) -> Unit, onClose: () -> Unit) {
    val c = WakTheme.colors
    Column {
        SheetHeader(
            title = stringResource(R.string.settings_reference_pitch),
            subtitle = stringResource(R.string.settings_pitch_subtitle),
            onClose = onClose,
        )
        Spacer(Modifier.height(26.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepButton(
                icon = painterResource(R.drawable.ic_minus),
                contentDescription = stringResource(R.string.settings_pitch_lower),
                enabled = pitch > SettingsViewModel.MIN_PITCH,
                onClick = { onChange(pitch - 1) },
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(pitch.toString(), style = spaceGrotesk(48.sp), color = c.text)
                Text(stringResource(R.string.settings_pitch_value, pitch), style = jakarta(13.sp), color = c.text3)
            }
            StepButton(
                icon = painterResource(R.drawable.ic_plus),
                contentDescription = stringResource(R.string.settings_pitch_higher),
                enabled = pitch < SettingsViewModel.MAX_PITCH,
                onClick = { onChange(pitch + 1) },
            )
        }
        Spacer(Modifier.height(26.dp))
        Text(
            stringResource(R.string.settings_pitch_presets).uppercase(),
            style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp, lineHeight = 14.sp),
            color = c.text3,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PitchPresets.forEach { preset ->
                WakChip(label = preset.toString(), selected = preset == pitch, onClick = { onChange(preset) })
            }
        }
        Spacer(Modifier.height(24.dp))
        WakButton(
            text = stringResource(R.string.settings_pitch_reset),
            onClick = { onChange(SettingsViewModel.DEFAULT_PITCH) },
            modifier = Modifier.fillMaxWidth(),
            variant = WakButtonVariant.Secondary,
            enabled = pitch != SettingsViewModel.DEFAULT_PITCH,
        )
        Spacer(Modifier.height(10.dp))
        WakButton(
            text = stringResource(R.string.settings_done),
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StepButton(icon: Painter, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = c.s3,
        border = BorderStroke(1.dp, c.line),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = if (enabled) c.text else c.text3, modifier = Modifier.size(22.dp))
        }
    }
}

@Preview(name = "Reference pitch sheet", widthDp = 390)
@Composable
private fun ReferencePitchPreview() {
    WAKordTheme(darkTheme = true) {
        Surface(color = WakTheme.colors.s2) {
            Box(Modifier.padding(20.dp)) {
                ReferencePitchContent(pitch = 442, onChange = {}, onClose = {})
            }
        }
    }
}
