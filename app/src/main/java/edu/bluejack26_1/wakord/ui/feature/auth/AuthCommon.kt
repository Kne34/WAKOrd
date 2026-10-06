package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.validation.DefaultAuthValidator
import edu.bluejack26_1.wakord.domain.validation.FieldError
import edu.bluejack26_1.wakord.ui.theme.SpaceGrotesk
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun AuthScaffold(
    modifier: Modifier = Modifier,
    @DrawableRes background: Int? = R.drawable.bg_login,
    backgroundHeight: Dp = 460.dp,
    bottom: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = WakTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        if (background != null) {
            AuthBackgroundArt(background, backgroundHeight)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val minHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(min = minHeight)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    content()
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.height(24.dp))
                    bottom()
                }
            }
        }
    }
}

@Composable
private fun AuthBackgroundArt(@DrawableRes image: Int, height: Dp) {
    val c = WakTheme.colors
    Box(
        modifier = Modifier
            .offset(x = (-60).dp, y = (-80).dp)
            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            .size(width = 510.dp, height = height),
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (c.isDark) 0.55f else 0.35f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to c.bg.copy(alpha = 0f),
                        0.35f to c.bg.copy(alpha = 0f),
                        0.96f to c.bg,
                    ),
                ),
        )
    }
}

@Composable
fun AuthHeadline(title: String, subtitle: String) {
    val c = WakTheme.colors
    Text(title, style = MaterialTheme.typography.displayLarge, color = c.text)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = c.text2)
}

@Composable
fun AuthTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
    end: @Composable () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        if (onBack != null) {
            Box(Modifier.align(Alignment.CenterStart)) { BackCircleButton(onBack) }
        }
        Box(Modifier.align(Alignment.Center)) { center() }
        Box(Modifier.align(Alignment.CenterEnd)) { end() }
    }
}

@Composable
fun BackCircleButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        color = c.s2.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, c.text.copy(alpha = 0.09f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.back),
                tint = c.text,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun StepLabel(step: Int, total: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.step_of, step, total),
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceGrotesk),
        color = WakTheme.colors.text3,
        modifier = modifier,
    )
}

@Composable
fun AuthLinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color = WakTheme.colors.text,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@Composable
fun AuthSwitchPrompt(prompt: String, action: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$prompt ", style = MaterialTheme.typography.bodyMedium, color = c.text2)
        Text(
            text = action,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = c.primaryText,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}

@Composable
fun fieldErrorText(error: FieldError?, @StringRes requiredRes: Int): String? = when (error) {
    null -> null
    FieldError.REQUIRED -> stringResource(requiredRes)
    FieldError.INVALID_EMAIL -> stringResource(R.string.error_invalid_email)
    FieldError.PASSWORD_TOO_SHORT -> stringResource(R.string.error_password_too_short, DefaultAuthValidator.MIN_PASSWORD_LENGTH)
    FieldError.PASSWORD_MISMATCH -> stringResource(R.string.error_password_mismatch)
    FieldError.TOO_FEW_GENRES -> stringResource(R.string.error_too_few_genres, DefaultAuthValidator.MIN_GENRES)
}

@Composable
fun AuthLogoMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = stringResource(R.string.app_name),
        modifier = modifier.size(width = 34.dp, height = 60.dp),
    )
}
