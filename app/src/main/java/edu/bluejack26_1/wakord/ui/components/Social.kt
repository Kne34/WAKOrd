package edu.bluejack26_1.wakord.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.bluejack26_1.wakord.R
import edu.bluejack26_1.wakord.domain.repository.UserPhotoRepository
import edu.bluejack26_1.wakord.ui.theme.WakColors
import edu.bluejack26_1.wakord.ui.theme.WakTheme
import edu.bluejack26_1.wakord.ui.theme.spaceGrotesk
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

val AvatarSmall = 32.dp
val AvatarLarge = 38.dp

fun compactCount(value: Int): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> trimmed(value / 1_000f) + "k"
    else -> trimmed(value / 1_000_000f) + "M"
}

private fun trimmed(value: Float): String {
    val text = String.format(Locale.US, "%.1f", value)
    return if (text.endsWith(".0")) text.dropLast(2) else text
}

@Composable
fun relativeTime(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - createdAt).coerceAtLeast(0) / 60_000).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.time_now)
        minutes < 60 -> stringResource(R.string.time_minutes, minutes)
        minutes < 60 * 24 -> stringResource(R.string.time_hours, minutes / 60)
        minutes < 60 * 24 * 7 -> stringResource(R.string.time_days, minutes / (60 * 24))
        else -> stringResource(R.string.time_weeks, minutes / (60 * 24 * 7))
    }
}

fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

private fun avatarColors(seed: String, c: WakColors): Pair<Color, Color> =
    c.avatars[abs(seed.hashCode()) % c.avatars.size]

@Composable
fun InitialsAvatar(name: String, seed: String, size: Dp, modifier: Modifier = Modifier, fontSize: TextUnit = 12.sp) {
    val (container, content) = avatarColors(seed, WakTheme.colors)
    Box(
        modifier = modifier
            .size(size)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials(name), style = spaceGrotesk(fontSize), color = content)
    }
}

private const val PHOTO_CACHE_SIZE = 48

private val PhotoCache = LruCache<String, ImageBitmap>(PHOTO_CACHE_SIZE)

val LocalUserPhotos = staticCompositionLocalOf<UserPhotoRepository?> { null }

private fun decodePhoto(photoUrl: String): ImageBitmap? = PhotoCache.get(photoUrl) ?: runCatching {
    val bytes = Base64.decode(photoUrl.substringAfter("base64,"), Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
}.getOrNull()?.also { PhotoCache.put(photoUrl, it) }

@Composable
fun ProfileAvatar(
    name: String,
    seed: String,
    photoUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val photo by produceState(photoUrl?.let { PhotoCache.get(it) }, photoUrl) {
        value = if (photoUrl.isNullOrBlank() || !photoUrl.startsWith("data:")) null else withContext(Dispatchers.Default) { decodePhoto(photoUrl) }
    }
    val bitmap = photo
    if (bitmap == null) {
        InitialsAvatar(name, seed, size, modifier, fontSize)
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape),
        )
    }
}

@Composable
fun UserAvatar(
    name: String,
    uid: String,
    size: Dp,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val photos = LocalUserPhotos.current
    val source = remember(photos, uid) { if (photos == null || uid.isBlank()) flowOf(null) else photos.photo(uid) }
    val photoUrl by source.collectAsState(initial = null)
    ProfileAvatar(name, uid, photoUrl, size, modifier, fontSize)
}
