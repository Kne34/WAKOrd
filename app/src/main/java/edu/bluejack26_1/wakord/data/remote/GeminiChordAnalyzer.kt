package edu.bluejack26_1.wakord.data.remote

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import edu.bluejack26_1.wakord.core.music.Chord
import edu.bluejack26_1.wakord.domain.model.AnalysisResult
import edu.bluejack26_1.wakord.domain.model.AnalysisSource
import edu.bluejack26_1.wakord.domain.model.DetectedChord
import edu.bluejack26_1.wakord.domain.repository.ChordAnalyzer
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

internal val geminiModel: GenerativeModel by lazy {
    Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = "gemini-2.5-flash",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
            temperature = 0.2f
        },
    )
}

class GeminiChordAnalyzer(private val clock: () -> Long = System::currentTimeMillis) : ChordAnalyzer {
    override suspend fun analyze(samples: FloatArray, sampleRate: Int): Result<AnalysisResult> = runCatching {
        val response = withTimeout(TIMEOUT_MS) {
            geminiModel.generateContent(
                content {
                    inlineData(WavEncoder.encode(samples, sampleRate), "audio/wav")
                    text(PROMPT)
                },
            )
        }
        GeminiAnalysisParser.parse(
            json = requireNotNull(response.text),
            id = UUID.randomUUID().toString(),
            createdAt = clock(),
            durationMs = samples.size * 1000L / sampleRate,
        )
    }

    private companion object {
        const val TIMEOUT_MS = 45_000L
        const val PROMPT = "You transcribe music. Listen to the recording and find its chord progression. " +
            "Reply with JSON only, shaped like {\"key\":\"C\",\"confidence\":0.8,\"bpm\":92," +
            "\"chords\":[{\"symbol\":\"C\",\"startMs\":0,\"endMs\":2000}]}. " +
            "Use standard chord symbols such as C, Am, F#m7, Bb, G7, Dsus4 or Cmaj7. " +
            "Use \"m\" for minor keys, for example Am. Return an empty chords list if no chords can be heard."
    }
}

object GeminiAnalysisParser {
    fun parse(json: String, id: String, createdAt: Long, durationMs: Long): AnalysisResult {
        val root = JSONObject(json.trim().removePrefix("```json").removePrefix("```").removeSuffix("```"))
        val list = root.optJSONArray("chords")
        val chords = (0 until (list?.length() ?: 0)).mapNotNull { i ->
            val c = list!!.getJSONObject(i)
            val symbol = c.optString("symbol")
            val parsed = Chord.parse(symbol) ?: return@mapNotNull null
            val start = c.optLong("startMs").coerceAtLeast(0)
            val end = c.optLong("endMs").coerceIn(start, durationMs.coerceAtLeast(start))
            DetectedChord(parsed.symbol, start, end)
        }
        val key = root.optString("key").let { k -> Chord.parse(k)?.let { c -> c.root + if (c.suffix.trim().let { it.startsWith("m") && !it.startsWith("maj") }) "m" else "" } }
        return AnalysisResult(
            id = id,
            createdAt = createdAt,
            durationMs = durationMs,
            key = key ?: chords.firstOrNull()?.symbol.orEmpty(),
            confidence = root.optDouble("confidence", 0.5).toFloat().coerceIn(0f, 1f),
            bpm = root.optInt("bpm", 90).coerceIn(40, 240),
            chords = chords,
            source = AnalysisSource.AI,
        )
    }
}

object WavEncoder {
    fun encode(samples: FloatArray, sampleRate: Int): ByteArray {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { data.putShort((it.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()) }
        val header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(HEADER_BYTES - 8 + samples.size * 2)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(sampleRate)
            putInt(sampleRate * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(samples.size * 2)
        }
        return ByteArrayOutputStream(HEADER_BYTES + samples.size * 2).apply {
            write(header.array())
            write(data.array())
        }.toByteArray()
    }

    private const val HEADER_BYTES = 44
}
