package edu.bluejack26_1.wakord.ui.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.components.ProfileAvatar
import edu.bluejack26_1.wakord.ui.components.WakButton
import edu.bluejack26_1.wakord.ui.components.WakTextField
import edu.bluejack26_1.wakord.ui.feature.auth.GenreChip
import edu.bluejack26_1.wakord.ui.feature.auth.nameRes
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

private const val MAX_BIO = 160
private const val MIN_GENRES = 3
private val EditableInstruments = Instrument.entries - Instrument.OTHER

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onAction: (EditProfileAction) -> Unit,
    onPickPhoto: () -> Unit,
    onBack: () -> Unit,
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
            GlassIconButton(painterResource(R.drawable.ic_back), stringResource(R.string.back), onBack, Modifier.align(Alignment.CenterStart))
            Text(stringResource(R.string.profile_edit), style = spaceGrotesk(16.sp), color = c.text)
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp),
        ) {
            PhotoPicker(state, onPickPhoto, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(22.dp))
            WakTextField(
                value = state.fullName,
                onValueChange = { onAction(EditProfileAction.FullNameChanged(it)) },
                label = stringResource(R.string.edit_name),
                error = if (state.fullNameError != null) stringResource(R.string.error_name_required) else null,
            )
            Spacer(Modifier.height(22.dp))
            WakTextField(
                value = state.username,
                onValueChange = { onAction(EditProfileAction.UsernameChanged(it)) },
                label = stringResource(R.string.edit_username),
                prefix = "@",
                error = when (state.usernameError) {
                    ProfileFieldError.REQUIRED -> stringResource(R.string.error_username_required)
                    ProfileFieldError.USERNAME_INVALID -> stringResource(R.string.error_username_invalid)
                    else -> null
                },
            )
            Spacer(Modifier.height(22.dp))
            WakTextField(
                value = state.bio,
                onValueChange = { onAction(EditProfileAction.BioChanged(it)) },
                label = stringResource(R.string.edit_bio),
                placeholder = stringResource(R.string.edit_bio_placeholder),
                supportingText = stringResource(R.string.edit_bio_count, state.bio.length, MAX_BIO),
                singleLine = false,
                imeAction = ImeAction.Default,
            )
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.instruments))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EditableInstruments.forEach { instrument ->
                    GenreChip(stringResource(instrument.nameRes()), instrument in state.instruments) {
                        onAction(EditProfileAction.ToggleInstrument(instrument))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SectionLabel(stringResource(R.string.edit_genres))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.availableGenres.forEach { genre ->
                    GenreChip(genre, genre in state.genres) { onAction(EditProfileAction.ToggleGenre(genre)) }
                }
            }
            if (state.genresError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.error_too_few_genres, MIN_GENRES),
                    style = jakarta(12.sp),
                    color = c.error,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Spacer(Modifier.height(38.dp))
            WakButton(
                text = stringResource(R.string.edit_save),
                onClick = { onAction(EditProfileAction.Save) },
                modifier = Modifier.fillMaxWidth(),
                loading = state.isSaving,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = jakarta(13.sp), color = WakTheme.colors.text2)
}

@Composable
private fun PhotoPicker(state: EditProfileUiState, onPickPhoto: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val ring = c.bg
    val description = stringResource(R.string.edit_change_photo)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(104.dp)) {
            ProfileAvatar(state.fullName, state.uid, state.photoUrl, 104.dp, fontSize = 34.sp)
            if (state.isUploadingPhoto) {
                Box(
                    Modifier
                        .size(104.dp)
                        .background(c.bg.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = c.primary, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
            Surface(
                onClick = onPickPhoto,
                enabled = !state.isUploadingPhoto,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(40.dp)
                    .drawBehind { drawCircle(ring, radius = size.minDimension / 2 + 3.dp.toPx()) },
                shape = CircleShape,
                color = c.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = description, tint = c.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            description,
            style = jakarta(14.sp),
            color = c.primaryText,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !state.isUploadingPhoto, onClick = onPickPhoto)
                .padding(horizontal = 16.dp, vertical = 11.dp),
        )
    }
}

@Preview(name = "Edit profile", heightDp = 900)
@Composable
private fun EditProfilePreview() {
    WAKordTheme(darkTheme = true) {
        EditProfileScreen(
            state = EditProfileUiState(
                isLoading = false,
                fullName = "Keane Wilson",
                username = "keane.wilson",
                bio = "Indie producer",
                instruments = setOf(Instrument.GUITAR, Instrument.UKULELE),
                genres = setOf("Acoustic", "Neo-Soul", "Indie Folk", "Jazz"),
            ),
            onAction = {},
            onPickPhoto = {},
            onBack = {},
        )
    }
}

@Preview(name = "Edit profile errors", heightDp = 900)
@Composable
private fun EditProfileErrorPreview() {
    WAKordTheme(darkTheme = false) {
        EditProfileScreen(
            state = EditProfileUiState(
                isLoading = false,
                fullName = "",
                username = "Keane W",
                fullNameError = ProfileFieldError.REQUIRED,
                usernameError = ProfileFieldError.USERNAME_INVALID,
                genresError = ProfileFieldError.TOO_FEW_GENRES,
            ),
            onAction = {},
            onPickPhoto = {},
            onBack = {},
        )
    }
}
