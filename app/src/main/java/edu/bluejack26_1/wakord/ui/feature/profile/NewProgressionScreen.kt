package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.core.music.DiatonicChords
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.ui.components.DialogTone
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.SegmentedControl
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakDialog
import edu.bluejack26_1.wakord.ui.components.WakSheetFrame
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.components.keyLabel
import edu.bluejack26_1.wakord.ui.components.levelLabel
import edu.bluejack26_1.wakord.ui.components.prettyChord
import edu.bluejack26_1.wakord.ui.feature.auth.DefaultGenres
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

const val MAX_PROGRESSION_CHORDS = 16
private const val MIN_CHORDS = 2
private val Roots = listOf("C", "C#", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B")
private val Qualities = listOf("maj" to "", "m" to "m", "7" to "7", "maj7" to "maj7", "m7" to "m7", "9" to "9", "13" to "13", "sus4" to "sus4")

private enum class PickerSheet { Key, Genre }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewProgressionScreen(
    state: NewProgressionUiState,
    onAction: (NewProgressionAction) -> Unit,
    onClose: () -> Unit,
    onSaveDraft: () -> Unit,
    onViewProgression: (String) -> Unit,
    onBackToProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = WakTheme.colors
    var root by rememberSaveable { mutableStateOf("C") }
    var quality by rememberSaveable { mutableStateOf("") }
    var sheet by rememberSaveable { mutableStateOf<PickerSheet?>(null) }
    var uploadedTitle by rememberSaveable { mutableStateOf("") }
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
            GlassIconButton(painterResource(R.drawable.ic_close), stringResource(R.string.close), onClose, Modifier.align(Alignment.CenterStart))
            Text(stringResource(R.string.profile_new_progression), style = spaceGrotesk(16.sp), color = c.text)
            Text(
                stringResource(R.string.new_prog_draft),
                style = jakarta(14.sp),
                color = c.primaryText,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onSaveDraft)
                    .padding(horizontal = 4.dp, vertical = 11.dp),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            WakTextField(
                value = state.title,
                onValueChange = { onAction(NewProgressionAction.TitleChanged(it)) },
                label = stringResource(R.string.new_prog_title),
                placeholder = stringResource(R.string.new_prog_title_placeholder),
                error = if (DraftError.TITLE_REQUIRED in state.errors) stringResource(R.string.error_title_required) else null,
            )
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DropdownField(stringResource(R.string.new_prog_key), keyLabel(state.key), { sheet = PickerSheet.Key }, Modifier.weight(1f))
                DropdownField(stringResource(R.string.new_prog_genre), state.genre, { sheet = PickerSheet.Genre }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            Label(stringResource(R.string.new_prog_difficulty))
            Spacer(Modifier.height(10.dp))
            SegmentedControl(
                labels = Difficulty.entries.map { levelLabel(it) },
                selectedIndex = state.difficulty.ordinal,
                onSelect = { onAction(NewProgressionAction.DifficultyChanged(Difficulty.entries[it])) },
                description = stringResource(R.string.new_prog_difficulty),
            )
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label(stringResource(R.string.new_prog_yours), Modifier.weight(1f))
                Text(
                    stringResource(R.string.new_prog_count, state.chords.size, MAX_PROGRESSION_CHORDS),
                    style = jakarta(12.sp),
                    color = c.text3,
                )
            }
            Spacer(Modifier.height(12.dp))
            ChordBuilder(
                state = state,
                root = root,
                quality = quality,
                onRoot = { root = it },
                onQuality = { quality = it },
                onAdd = { onAction(NewProgressionAction.AddChord(root + quality)) },
                onRemove = { onAction(NewProgressionAction.RemoveChord(it)) },
            )
            if (DraftError.TOO_FEW_CHORDS in state.errors) {
                Spacer(Modifier.height(6.dp))
                Text(
                    pluralStringResource(R.plurals.error_too_few_chords, MIN_CHORDS, MIN_CHORDS),
                    style = jakarta(12.sp),
                    color = c.error,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            WakTextField(
                value = state.description,
                onValueChange = { onAction(NewProgressionAction.DescriptionChanged(it)) },
                label = stringResource(R.string.new_prog_description),
                placeholder = stringResource(R.string.new_prog_description_placeholder),
                singleLine = false,
                imeAction = ImeAction.Default,
            )
            Spacer(Modifier.height(38.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PreviewButton(
                    playing = state.isPreviewing,
                    enabled = state.chords.isNotEmpty(),
                    onClick = { onAction(NewProgressionAction.TogglePreview) },
                )
                WakButton(
                    text = stringResource(R.string.new_prog_upload),
                    onClick = {
                        uploadedTitle = state.title.trim()
                        onAction(NewProgressionAction.Upload)
                    },
                    modifier = Modifier.weight(1f),
                    loading = state.isUploading,
                    leadingIcon = painterResource(R.drawable.ic_upload),
                )
            }
        }
    }

    when (sheet) {
        PickerSheet.Key -> OptionSheet(
            title = stringResource(R.string.new_prog_pick_key),
            options = DiatonicChords.KEYS.map { it to keyLabel(it) },
            selected = state.key,
            onSelect = {
                onAction(NewProgressionAction.KeyChanged(it))
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        PickerSheet.Genre -> OptionSheet(
            title = stringResource(R.string.new_prog_pick_genre),
            options = (DefaultGenres + state.genre).distinct().map { it to it },
            selected = state.genre,
            onSelect = {
                onAction(NewProgressionAction.GenreChanged(it))
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }

    val uploadedId = state.uploadedId
    if (uploadedId != null) {
        WakDialog(
            title = stringResource(R.string.uploaded_title),
            message = stringResource(R.string.uploaded_message, uploadedTitle),
            primaryText = stringResource(R.string.uploaded_view),
            onPrimary = { onViewProgression(uploadedId) },
            onDismissRequest = onBackToProfile,
            secondaryText = stringResource(R.string.uploaded_back),
            icon = painterResource(R.drawable.ic_check_circle),
            tone = DialogTone.Default,
        )
    }
}

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
    Text(text, style = jakarta(13.sp), color = WakTheme.colors.text2, modifier = modifier)
}

@Composable
private fun DropdownField(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Column(modifier) {
        Label(label)
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = onClick,
            modifier = Modifier
                .height(54.dp)
                .semantics { contentDescription = "$label, $value" },
            shape = RoundedCornerShape(16.dp),
            color = c.s2,
            border = BorderStroke(1.dp, c.outline),
        ) {
            Row(Modifier.padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(value, style = jakarta(16.sp), color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = c.text3, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChordBuilder(
    state: NewProgressionUiState,
    root: String,
    quality: String,
    onRoot: (String) -> Unit,
    onQuality: (String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val c = WakTheme.colors
    val canAdd = state.chords.size < MAX_PROGRESSION_CHORDS
    val next = root + quality
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = c.s2,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(15.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.chords.forEachIndexed { index, chord ->
                    ProgressionChip(chord, now = index == state.previewIndex, onRemove = { onRemove(index) })
                }
                val addLabel = stringResource(R.string.new_prog_add_chord_cd, prettyChord(next))
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 44.dp)
                        .alpha(if (canAdd) 1f else 0.4f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, c.line, RoundedCornerShape(14.dp))
                        .clickable(enabled = canAdd, role = Role.Button, onClick = onAdd)
                        .semantics { contentDescription = addLabel },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_plus), contentDescription = null, tint = c.text3, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.line),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.new_prog_picker).uppercase(),
                style = spaceGrotesk(11.sp).copy(letterSpacing = 1.1.sp, lineHeight = 14.sp),
                color = c.text3,
            )
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.new_prog_root), style = jakarta(12.sp), color = c.text3)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Roots.forEach { note -> PickerChip(prettyChord(note), note == root, filled = true) { onRoot(note) } }
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.new_prog_quality), style = jakarta(12.sp), color = c.text3)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Qualities.forEach { (label, suffix) -> PickerChip(label, suffix == quality, filled = false) { onQuality(suffix) } }
            }
        }
    }
}

@Composable
private fun ProgressionChip(chord: String, now: Boolean, onRemove: () -> Unit) {
    val c = WakTheme.colors
    val content = if (now) c.onPrimary else c.text
    val removeLabel = stringResource(R.string.new_prog_remove_chord_cd, prettyChord(chord))
    Row(
        modifier = Modifier
            .height(44.dp)
            .background(if (now) c.primary else c.s3, RoundedCornerShape(14.dp))
            .border(1.dp, if (now) c.primary else c.line, RoundedCornerShape(14.dp))
            .padding(start = 15.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prettyChord(chord), style = spaceGrotesk(16.sp), color = content, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removeLabel },
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = if (now) c.onPrimary else c.text3, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun PickerChip(label: String, selected: Boolean, filled: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 44.dp),
        shape = CircleShape,
        color = when {
            selected -> c.primary
            filled -> c.s3
            else -> c.s2
        },
        border = BorderStroke(1.dp, if (selected) c.primary else if (filled) c.line else c.outline),
    ) {
        Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(label, style = jakarta(16.sp), color = if (selected) c.onPrimary else c.text, maxLines = 1)
        }
    }
}

@Composable
private fun PreviewButton(playing: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .height(54.dp)
            .widthIn(min = 120.dp)
            .alpha(if (enabled) 1f else 0.5f),
        shape = RoundedCornerShape(18.dp),
        color = c.s3,
        border = BorderStroke(1.dp, c.outline),
    ) {
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(if (playing) R.drawable.ic_stop else R.drawable.ic_play), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(if (playing) R.string.new_prog_stop else R.string.new_prog_preview), style = jakarta(16.sp), color = c.text, maxLines = 1)
        }
    }
}

@Composable
private fun OptionSheet(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = WakTheme.colors
    WakSheetFrame(onDismiss = onDismiss) {
        Text(
            title,
            style = spaceGrotesk(20.sp),
            color = c.text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
        ) {
            options.forEach { (value, label) ->
                val active = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.RadioButton) { onSelect(value) }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = jakarta(15.sp), color = if (active) c.primaryText else c.text, modifier = Modifier.weight(1f))
                    if (active) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Preview(name = "New progression", heightDp = 1200)
@Composable
private fun NewProgressionPreview() {
    WAKordTheme(darkTheme = true) {
        NewProgressionScreen(
            state = NewProgressionUiState(
                title = "Rainy window loop",
                genre = "Lo-fi",
                difficulty = Difficulty.INTERMEDIATE,
                chords = listOf("Cmaj7", "Am9", "Dm9", "G13"),
                previewIndex = 3,
            ),
            onAction = {},
            onClose = {},
            onSaveDraft = {},
            onViewProgression = {},
            onBackToProfile = {},
        )
    }
}

@Preview(name = "New progression errors", heightDp = 1200)
@Composable
private fun NewProgressionErrorPreview() {
    WAKordTheme(darkTheme = false) {
        NewProgressionScreen(
            state = NewProgressionUiState(errors = setOf(DraftError.TITLE_REQUIRED, DraftError.TOO_FEW_CHORDS)),
            onAction = {},
            onClose = {},
            onSaveDraft = {},
            onViewProgression = {},
            onBackToProfile = {},
        )
    }
}
