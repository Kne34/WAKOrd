package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(c.s3, CircleShape)
                .border(1.dp, c.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_alert_circle), contentDescription = null, tint = c.text3, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.error_load_title), style = spaceGrotesk(20.sp), color = c.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(message, style = jakarta(14.sp), color = c.text2, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Surface(
            onClick = onRetry,
            modifier = Modifier.height(40.dp),
            shape = RoundedCornerShape(14.dp),
            color = c.primary,
        ) {
            Box(Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.try_again), style = spaceGrotesk(14.sp), color = c.onPrimary, maxLines = 1)
            }
        }
    }
}

@Preview(name = "Error state")
@Composable
private fun ErrorStatePreview() {
    WAKordTheme(darkTheme = true) {
        Box(Modifier.background(WakTheme.colors.bg)) {
            ErrorState(stringResource(R.string.error_network), onRetry = {})
        }
    }
}
