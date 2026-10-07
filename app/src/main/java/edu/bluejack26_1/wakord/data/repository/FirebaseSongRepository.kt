package edu.bluejack26_1.wakord.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import edu.bluejack26_1.wakord.core.music.ChordProParser
import edu.bluejack26_1.wakord.data.remote.firebaseCall
import edu.bluejack26_1.wakord.data.remote.int
import edu.bluejack26_1.wakord.data.remote.requireUid
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.strings
import edu.bluejack26_1.wakord.data.remote.valueFlow
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.SongSection
import edu.bluejack26_1.wakord.domain.repository.SongRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseSongRepository(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : SongRepository {
    override fun song(id: String): Flow<Song?> =
        database.getReference("songs/$id").valueFlow().map { if (it.exists()) it.toSong() else null }

    override fun isLiked(id: String): Flow<Boolean> = database.likedFlow(auth, id)

    override suspend fun setLiked(id: String, liked: Boolean): Result<Unit> = firebaseCall {
        database.toggleLike(auth.requireUid(), id, "songs/$id", liked)
    }

    private fun DataSnapshot.toSong() = Song(
        id = key.orEmpty(),
        title = string("title"),
        artist = string("artist"),
        key = string("key"),
        capo = int("capo"),
        tempo = int("tempo").takeIf { it > 0 } ?: 90,
        difficulty = difficulty(),
        genre = string("genre"),
        likeCount = int("likeCount"),
        saveCount = int("saveCount"),
        sections = child("sections").children.map { section ->
            SongSection(
                name = section.string("name"),
                lines = section.strings("lines").map(ChordProParser::parseLine),
            )
        },
    )
}
