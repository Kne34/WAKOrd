package edu.bluejack26_1.wakord.data.repository

import edu.bluejack26_1.wakord.core.music.ChordProParser
import edu.bluejack26_1.wakord.data.remote.DataNode
import edu.bluejack26_1.wakord.data.remote.int
import edu.bluejack26_1.wakord.data.remote.long
import edu.bluejack26_1.wakord.data.remote.string
import edu.bluejack26_1.wakord.data.remote.strings
import edu.bluejack26_1.wakord.domain.model.ChordProgression
import edu.bluejack26_1.wakord.domain.model.Difficulty
import edu.bluejack26_1.wakord.domain.model.Instrument
import edu.bluejack26_1.wakord.domain.model.Song
import edu.bluejack26_1.wakord.domain.model.SongSection
import edu.bluejack26_1.wakord.domain.model.User

internal fun DataNode.difficulty(): Difficulty =
    Difficulty.entries.firstOrNull { it.name == string("difficulty") } ?: Difficulty.EASY

internal fun DataNode.toSong() = Song(
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

internal fun DataNode.toProgression() = ChordProgression(
    id = key.orEmpty(),
    title = string("title"),
    key = string("key"),
    bpm = int("bpm").takeIf { it > 0 } ?: 90,
    genre = string("genre"),
    difficulty = difficulty(),
    chords = strings("chords"),
    authorId = string("authorId"),
    authorName = string("authorName"),
    description = string("description"),
    playCount = int("playCount"),
    likeCount = int("likeCount"),
    saveCount = int("saveCount"),
    createdAt = long("createdAt"),
)

internal fun DataNode.toUser(uid: String, fallbackEmail: String = "") = User(
    uid = uid,
    fullName = string("fullName"),
    email = string("email").ifEmpty { fallbackEmail },
    username = string("username"),
    photoUrl = child("photoUrl").asString(),
    bio = string("bio"),
    instruments = strings("instruments").mapNotNull { name -> Instrument.entries.firstOrNull { it.name == name } },
    genres = strings("genres"),
)
