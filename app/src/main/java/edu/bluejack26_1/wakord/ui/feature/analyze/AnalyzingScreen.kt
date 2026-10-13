package edu.bluejack26_1.wakord.ui.feature.analyze

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.model.AnalysisStage
import edu.bluejack26_1.wakord.ui.components.BackgroundArt
import edu.bluejack26_1.wakord.ui.components.GlassIconButton
import edu.bluejack26_1.wakord.ui.theme.WAKordTheme
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk

@Composable
fun AnalyzingScreen(stage: AnalysisStage?, recordedMs: Long, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val c = WakTheme.colors
    val stages = AnalysisStage.entries
    val current = stage?.ordinal ?: 0
    val progress by animateFloatAsState((current + 1f) / stages.size, label = "analyzeProgress")
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        BackgroundArt(R.drawable.bg_analyze, height = 700.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassIconButton(
                    painterResource(R.drawable.ic_close),
                    stringResource(R.string.analyzing_cancel_cd),
                    onCancel,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(stringResource(R.string.analyzing_title), style = spaceGrotesk(14.sp), color = c.text)
            }
            Spacer(Modifier.height(76.dp))
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(c.s5, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    drawArc(c.primary, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.analyzing_step, current + 1, stages.size), style = spaceGrotesk(26.sp), color = c.text)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.analyzing_recording, formatClock(recordedMs)), style = jakarta(12.sp), color = c.text3)
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.analyzing_headline), style = spaceGrotesk(26.sp), color = c.text)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.analyzing_body),
                style = jakarta(14.sp),
                color = c.text2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 44.dp),
            )
            Spacer(Modifier.height(28.dp))
            Surface(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = c.s2,
                border = BorderStroke(1.dp, c.line),
            ) {
                Column(Modifier.padding(horizontal = 17.dp, vertical = 4.dp)) {
                    stages.forEachIndexed { i, s ->
                        if (i > 0) HorizontalDivider(thickness = 1.dp, color = c.line)
                        StageRow(
                            label = stringResource(
                                when (s) {
                                    AnalysisStage.CLEANING_NOISE -> R.string.analyzing_stage_noise
                                    AnalysisStage.DETECTING_CHORDS -> R.string.analyzing_stage_chords
                                    AnalysisStage.MATCHING_SONGS -> R.string.analyzing_stage_songs
                                },
                            ),
                            done = i < current,
                            active = i == current,
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StageRow(label: String, done: Boolean, active: Boolean) {
    val c = WakTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            when {
                done -> Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = c.primaryText, modifier = Modifier.size(18.dp))
                active -> CircularProgressIndicator(color = c.primary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else -> Icon(painterResource(R.drawable.ic_clock), contentDescription = null, tint = c.text3, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = jakarta(13.sp), color = if (done || active) c.text else c.text3)
    }
}

@Preview(name = "Analyzing", heightDp = 844)
@Composable
private fun AnalyzingPreview() {
    WAKordTheme(darkTheme = true) {
        AnalyzingScreen(AnalysisStage.DETECTING_CHORDS, 24_000L, {})
    }
}
