package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.validation.DefaultAuthValidator
import edu.bluejack26_1.wakord.domain.validation.FieldError
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.theme.SpaceGrotesk
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme

private fun Instrument.nameRes() = when (this) {
    Instrument.GUITAR -> R.string.instrument_guitar
    Instrument.UKULELE -> R.string.instrument_ukulele
    Instrument.BASS -> R.string.instrument_bass
    Instrument.PIANO -> R.string.instrument_piano
    Instrument.VOCALS -> R.string.instrument_vocals
    Instrument.OTHER -> R.string.instrument_other
}

private fun Instrument.detailRes() = when (this) {
    Instrument.GUITAR -> R.string.instrument_guitar_detail
    Instrument.UKULELE -> R.string.instrument_ukulele_detail
    Instrument.BASS -> R.string.instrument_bass_detail
    Instrument.PIANO -> R.string.instrument_piano_detail
    Instrument.VOCALS -> R.string.instrument_vocals_detail
    Instrument.OTHER -> R.string.instrument_other_detail
}

private fun Instrument.iconRes() = when (this) {
    Instrument.GUITAR, Instrument.UKULELE, Instrument.BASS -> R.drawable.ic_guitar
    Instrument.PIANO -> R.drawable.ic_piano
    Instrument.VOCALS -> R.drawable.ic_vocals
    Instrument.OTHER -> R.drawable.ic_other
}

@Composable
fun PreferencesScreen(
    state: PreferencesUiState,
    onAction: (PreferencesAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    AuthScaffold(
        modifier = modifier,
        background = null,
        bottom = {
            WakButton(
                text = stringResource(R.string.continue_label),
                onClick = { onAction(PreferencesAction.Continue) },
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
        },
    ) {
        AuthTopBar(
            onBack = null,
            center = { StepLabel(step = 2, total = 2) },
            end = { AuthLinkText(stringResource(R.string.skip), { onAction(PreferencesAction.Skip) }) },
        )
        Spacer(Modifier.height(16.dp))
        AuthHeadline(stringResource(R.string.preferences_title), stringResource(R.string.preferences_subtitle))

        Spacer(Modifier.height(30.dp))
        SectionHeader(
            title = stringResource(R.string.instruments),
            trailing = stringResource(R.string.selected_count, state.selectedInstruments.size),
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Instrument.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { instrument ->
                        InstrumentCard(
                            instrument = instrument,
                            selected = instrument in state.selectedInstruments,
                            onClick = { onAction(PreferencesAction.ToggleInstrument(instrument)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
        SectionHeader(
            title = stringResource(R.string.favorite_genres),
            trailing = stringResource(R.string.pick_at_least, DefaultAuthValidator.MIN_GENRES),
        )
        Spacer(Modifier.height(14.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.availableGenres.forEach { genre ->
                GenreChip(
                    label = genre,
                    selected = genre in state.selectedGenres,
                    onClick = { onAction(PreferencesAction.ToggleGenre(genre)) },
                )
            }
        }
        fieldErrorText(state.genreError, R.string.error_too_few_genres)?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.error)
        }
        if (state.errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(stringResource(state.errorMessage), style = MaterialTheme.typography.bodySmall, color = c.error)
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String) {
    val c = WakTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = c.text,
            modifier = Modifier.weight(1f),
        )
        Text(trailing, style = MaterialTheme.typography.bodySmall, color = c.text3)
    }
}

@Composable
private fun InstrumentCard(
    instrument: Instrument,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    val content = if (selected) c.onPrimary else c.text
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) c.primary else c.s3,
        border = BorderStroke(1.dp, if (selected) c.primary else c.text2.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(instrument.iconRes()), contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(instrument.nameRes()),
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = content,
                )
                Text(
                    text = stringResource(instrument.detailRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.75f),
                )
            }
            if (selected) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.height(38.dp),
        shape = CircleShape,
        color = if (selected) c.primary else c.bg.copy(alpha = 0f),
        border = BorderStroke(1.dp, if (selected) c.primary else c.outline),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = if (selected) c.onPrimary else c.text,
            )
        }
    }
}

@Preview
@Composable
private fun PreferencesPreview() {
    WAKordTheme {
        PreferencesScreen(
            state = PreferencesUiState(
                selectedInstruments = setOf(Instrument.GUITAR, Instrument.UKULELE),
                selectedGenres = setOf("Acoustic", "Neo-Soul", "Jazz", "Indie Folk"),
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Preferences - too few genres")
@Composable
private fun PreferencesErrorPreview() {
    WAKordTheme {
        PreferencesScreen(
            state = PreferencesUiState(
                selectedInstruments = setOf(Instrument.PIANO),
                selectedGenres = setOf("Pop"),
                genreError = FieldError.TOO_FEW_GENRES,
            ),
            onAction = {},
        )
    }
}
