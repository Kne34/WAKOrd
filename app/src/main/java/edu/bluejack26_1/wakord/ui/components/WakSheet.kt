package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WakSheetFrame(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = WakTheme.colors
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = shape,
        containerColor = Color.Transparent,
        scrimColor = c.scrim,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = c.s2,
            border = BorderStroke(1.dp, c.line),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .padding(top = 11.dp, bottom = 18.dp)
                        .size(width = 40.dp, height = 5.dp)
                        .background(c.s5, CircleShape),
                )
                content()
            }
        }
    }
}

@Composable
fun WakSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    WakSheetFrame(onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            content = content,
        )
    }
}

@Composable
fun SheetHeader(title: String, subtitle: String, onClose: () -> Unit) {
    val c = WakTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = spaceGrotesk(20.sp), color = c.text)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = jakarta(12.sp), color = c.text3)
        }
        Surface(
            onClick = onClose,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = c.s3,
            border = BorderStroke(1.dp, c.line),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close), tint = c.text, modifier = Modifier.size(20.dp))
            }
        }
    }
}
