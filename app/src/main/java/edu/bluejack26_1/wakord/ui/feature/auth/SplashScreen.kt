package edu.bluejack26_1.wakord.ui.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.components.windowWidth
import edu.bluejack26_1.wakord.ui.theme.SpaceGrotesk
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val latestOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        delay(1500)
        latestOnFinished()
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        Image(
            painter = painterResource(R.drawable.bg_splash),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (c.isDark) 0.55f else 0.35f,
            modifier = Modifier
                .offset(x = (windowWidth() - 630.dp) / 2, y = 80.dp)
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .size(width = 630.dp, height = 640.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to Color.Black,
                            0.35f to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-24).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.logo_wordmark),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(width = 236.dp, height = 91.dp),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = stringResource(R.string.splash_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = c.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(240.dp),
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = c.primary,
                trackColor = c.s5,
                strokeWidth = 2.dp,
            )
            Text(
                text = stringResource(R.string.splash_version),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = SpaceGrotesk),
                color = c.text3,
            )
        }
    }
}

@Preview
@Composable
private fun SplashPreview() {
    WAKordTheme { SplashScreen(onFinished = {}) }
}
