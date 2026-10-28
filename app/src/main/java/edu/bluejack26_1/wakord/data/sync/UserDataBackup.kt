package edu.bluejack26_1.wakord.data.sync

import edu.bluejack26_1.wakord.core.discover.SearchEngine
import edu.bluejack26_1.wakord.data.remote.firebaseCall

object UserDataBackup {
    private val SHARED = listOf("songs", "progressions", "users")
    private val PERSONAL = listOf("userLikes", "plays", "saves", "tunings", "notifications", "follows", "followers")
    private val COMMENTED = listOf("songs", "progressions")

    fun paths(uid: String, now: Long): List<String> =
        SHARED + "weeklyLikes/${SearchEngine.weekKey(now)}" + PERSONAL.map { "$it/$uid" }

    suspend fun run(backup: FirebaseBackup, uid: String, now: Long = System.currentTimeMillis()): Int {
        val saved = backup.backup(paths(uid, now))
        val targets = COMMENTED.flatMap { path ->
            firebaseCall { backup.get(path).children.mapNotNull { it.key } }.getOrDefault(emptyList())
        }
        return saved + backup.backup(targets.map { "comments/$it" })
    }
}
