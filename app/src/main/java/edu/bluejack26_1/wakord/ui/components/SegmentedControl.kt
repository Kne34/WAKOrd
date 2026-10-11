package edu.bluejack26_1.wakord.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.jakarta

@Composable
fun SegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    val c = WakTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(c.s3, RoundedCornerShape(16.dp))
            .border(1.dp, c.text2.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
            .padding(5.dp)
            .semantics { if (description != null) contentDescription = description }
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val active = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (active) c.s1 else Color.Transparent, RoundedCornerShape(12.dp))
                    .selectable(selected = active, onClick = { onSelect(index) }, role = Role.Tab),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = jakarta(13.sp), color = if (active) c.text else c.text3, maxLines = 1)
            }
        }
    }
}
