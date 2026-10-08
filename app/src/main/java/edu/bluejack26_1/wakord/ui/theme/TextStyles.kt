package edu.bluejack26_1.wakord.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

fun spaceGrotesk(size: TextUnit, weight: FontWeight = FontWeight.Bold) =
    TextStyle(fontFamily = SpaceGrotesk, fontWeight = weight, fontSize = size, lineHeight = size * 1.25f)

fun jakarta(size: TextUnit, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = PlusJakartaSans, fontWeight = weight, fontSize = size, lineHeight = size * 1.3f)
