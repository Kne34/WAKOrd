package edu.bluejack26_1.wakord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class WakColors(
    val bg: Color,
    val s1: Color,
    val s2: Color,
    val s3: Color,
    val s4: Color,
    val s5: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val outline: Color,
    val primary: Color,
    val primary2: Color,
    val primaryText: Color,
    val onPrimary: Color,
    val peach: Color,
    val peachText: Color,
    val orange: Color,
    val onOrange: Color,
    val lavender: Color,
    val error: Color,
    val onError: Color,
    val isDark: Boolean,
)

val DarkWakColors = WakColors(
    bg = Color(0xFF080D1D),
    s1 = Color(0xFF0E1323),
    s2 = Color(0xFF161B2B),
    s3 = Color(0xFF1A1F30),
    s4 = Color(0xFF25293A),
    s5 = Color(0xFF2F3446),
    text = Color(0xFFDEE1F9),
    text2 = Color(0xFFBBC9CB),
    text3 = Color(0xFF859395),
    outline = Color(0xFF3C494B),
    primary = Color(0xFF4DE7F6),
    primary2 = Color(0xFF17CBD9),
    primaryText = Color(0xFF4DE7F6),
    onPrimary = Color(0xFF00363B),
    peach = Color(0xFFFFB597),
    peachText = Color(0xFFFFB597),
    orange = Color(0xFFFF8A55),
    onOrange = Color(0xFF2B0C00),
    lavender = Color(0xFFD3D0F9),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    isDark = true,
)

val LightWakColors = WakColors(
    bg = Color(0xFFF1F5F9),
    s1 = Color(0xFFFFFFFF),
    s2 = Color(0xFFFFFFFF),
    s3 = Color(0xFFE9EEF4),
    s4 = Color(0xFFE2E8F0),
    s5 = Color(0xFFCBD5E1),
    text = Color(0xFF0F172A),
    text2 = Color(0xFF475569),
    text3 = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    primary = Color(0xFF4DE7F6),
    primary2 = Color(0xFF17CBD9),
    primaryText = Color(0xFF00747C),
    onPrimary = Color(0xFF00363B),
    peach = Color(0xFFFFB597),
    peachText = Color(0xFFA8410F),
    orange = Color(0xFFFF8A55),
    onOrange = Color(0xFF2B0C00),
    lavender = Color(0xFF4B46A8),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    isDark = false,
)

val LocalWakColors = staticCompositionLocalOf { DarkWakColors }
