package edu.bluejack26_1.wakord.data.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import androidx.core.net.toUri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import edu.bluejack26_1.wakord.data.remote.NodeOrder
import edu.bluejack26_1.wakord.data.remote.NodeQuery
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.sync.FirebaseBackup
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.ProfileUpdate
import edu.bluejack26_1.wakord.domain.model.User
import edu.bluejack26_1.wakord.domain.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class FirebaseProfileRepository(
    private val auth: FirebaseAuth,
    private val backup: FirebaseBackup,
    private val contentResolver: ContentResolver,
) : ProfileRepository {
    override fun user(uid: String): Flow<User?> =
        backup.observe("users/$uid").map { if (it.exists()) it.toUser(uid) else null }

    override fun progressionsBy(uid: String): Flow<List<ChordProgression>> =
        backup.observe(NodeQuery("progressions", NodeOrder.Child("authorId"), equalTo = uid)).map { snapshot ->
            snapshot.children.map { it.toProgression() }.sortedByDescending { it.likeCount }
        }

    override suspend fun updateProfile(update: ProfileUpdate): Result<Unit> = firebaseCall {
        val uid = auth.requireUid()
        backup.update(
            mapOf(
                "users/$uid/fullName" to update.fullName.trim(),
                "users/$uid/username" to update.username.trim().lowercase(),
                "users/$uid/bio" to update.bio.trim(),
                "users/$uid/instruments" to update.instruments.map { it.name },
                "users/$uid/genres" to update.genres.toList(),
            ),
        )
    }

    override suspend fun uploadPhoto(uri: String): Result<String> = firebaseCall {
        val uid = auth.requireUid()
        val dataUrl = withContext(Dispatchers.IO) { encodePhoto(uri.toUri()) }
        backup.update(mapOf("users/$uid/photoUrl" to dataUrl))
        dataUrl
    }

    private fun encodePhoto(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PHOTO_SIZE) sample *= 2
        val decoded = contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Cannot read photo")
        val scale = PHOTO_SIZE.toFloat() / maxOf(decoded.width, decoded.height)
        val bitmap = if (scale < 1f) {
            decoded.scale((decoded.width * scale).toInt(), (decoded.height * scale).toInt())
        } else {
            decoded
        }
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            out.toByteArray()
        }
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private companion object {
        const val PHOTO_SIZE = 256
        const val JPEG_QUALITY = 80
    }
}
