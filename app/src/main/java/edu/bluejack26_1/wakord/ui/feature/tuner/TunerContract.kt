package edu.bluejack26_1.wakord.ui.feature.tuner

import edu.bluejack26_1.wakord.core.audio.NoteReading
import edu.bluejack26_1.wakord.domain.model.TuneStatus
import edu.bluejack26_1.wakord.domain.model.TunerInstrument
import edu.bluejack26_1.wakord.domain.model.Tuning
import edu.bluejack26_1.wakord.domain.model.TuningPresets

enum class MicPermission { UNKNOWN, GRANTED, DENIED }

enum class TunerSheet { INSTRUMENT, CUSTOM_TUNING }

data class TunerUiState(
    val instrument: TunerInstrument = TunerInstrument.GUITAR,
    val tuning: Tuning = TuningPresets.default(TunerInstrument.GUITAR),
    val customTunings: List<Tuning> = emptyList(),
    val micPermission: MicPermission = MicPermission.UNKNOWN,
    val autoDetect: Boolean = true,
    val selectedString: Int = 0,
    val activeString: Int? = null,
    val reading: NoteReading? = null,
    val centsOff: Float = 0f,
    val status: TuneStatus = TuneStatus.IDLE,
    val tunedStrings: Set<Int> = emptySet(),
    val referencePitch: Float = 440f,
    val pitchPipeMode: Boolean = false,
    val pitchPipeString: Int? = null,
    val sheet: TunerSheet? = null,
) {
    val presets: List<Tuning> get() = TuningPresets.forInstrument(instrument)
    val customForInstrument: List<Tuning> get() = customTunings.filter { it.instrument == instrument }
}

sealed interface TunerAction {
    data class PermissionResult(val granted: Boolean) : TunerAction
    data object StopListening : TunerAction
    data class SelectInstrument(val instrument: TunerInstrument) : TunerAction
    data class SelectTuning(val tuning: Tuning) : TunerAction
    data class SelectString(val index: Int) : TunerAction
    data object ToggleAuto : TunerAction
    data object UsePitchPipe : TunerAction
    data object UseListening : TunerAction
    data class PlayPitchPipe(val index: Int) : TunerAction
    data class SaveCustomTuning(val name: String, val notes: List<String>) : TunerAction
    data class OpenSheet(val sheet: TunerSheet) : TunerAction
    data object CloseSheet : TunerAction
}
