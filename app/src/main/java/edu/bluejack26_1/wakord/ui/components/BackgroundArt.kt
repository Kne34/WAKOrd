package edu.bluejack26_1.wakord.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.ui.theme.WakTheme

@Composable
fun BackgroundArt(@DrawableRes image: Int, height: Dp, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Box(
        modifier = modifier
            .offset(x = (-60).dp, y = (-80).dp)
            .wrapContentSize(align = Alignment.TopStart, unbounded = true)
            .size(width = 510.dp, height = height),
    ) {
        Crossfade(targetState = image, label = "background") { res ->
            Image(
                painter = painterResource(res),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = if (c.isDark) 0.55f else 0.35f,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
