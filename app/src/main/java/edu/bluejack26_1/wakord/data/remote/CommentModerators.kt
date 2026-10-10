package edu.bluejack26_1.wakord.data.remote

import com.google.firebase.ai.type.content
import edu.bluejack26_1.wakord.domain.repository.CommentModerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class WordListModerator : CommentModerator {
    override suspend fun isAllowed(text: String): Boolean {
        val words = normalize(text).split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.toSet()
        val joined = normalize(text).replace(Regex("[^a-z]"), "")
        return words.none { it in BLOCKED } && PHRASES.none { joined.contains(it) }
    }

    private fun normalize(text: String): String = text.lowercase()
        .replace('0', 'o').replace('1', 'i').replace('3', 'e').replace('4', 'a')
        .replace('5', 's').replace('7', 't').replace('@', 'a').replace('$', 's')

    private companion object {
        val BLOCKED = setOf(
            "fuck", "fucking", "shit", "bitch", "bastard", "asshole", "cunt", "slut", "whore", "retard", "nigger",
            "anjing", "bangsat", "bajingan", "kontol", "memek", "ngentot", "goblok", "tolol", "babi", "jancok", "pepek",
        )
        val PHRASES = listOf("killyourself", "bunuhdiri")
    }
}

class GeminiCommentModerator(private val fallback: CommentModerator = WordListModerator()) : CommentModerator {
    override suspend fun isAllowed(text: String): Boolean {
        if (!fallback.isAllowed(text)) return false
        return try {
            val reply = withTimeout(TIMEOUT_MS) { geminiModel.generateContent(content { text(PROMPT + text) }) }
            parseVerdict(requireNotNull(reply.text))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            true
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10_000L
        private const val PROMPT = "You moderate comments in a music app for guitarists and singers. " +
            "Block insults, harassment, hate speech, sexual content, threats and spam links in any language, including Indonesian. " +
            "Allow honest criticism of music and friendly jokes. Reply with JSON only: {\"allowed\": true} or {\"allowed\": false}. Comment: "

        fun parseVerdict(json: String): Boolean =
            JSONObject(json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```")).optBoolean("allowed", true)
    }
}
