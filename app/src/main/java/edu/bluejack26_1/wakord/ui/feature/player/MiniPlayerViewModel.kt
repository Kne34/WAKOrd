package edu.bluejack26_1.wakord.ui.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import edu.bluejack26_1.wakord.WAKordApp
import edu.bluejack26_1.wakord.core.audio.PlaybackController
import edu.bluejack26_1.wakord.core.audio.PlaybackState
import kotlinx.coroutines.flow.StateFlow

class MiniPlayerViewModel(private val playbackController: PlaybackController) : ViewModel() {
    val state: StateFlow<PlaybackState> = playbackController.state

    fun onAction(action: MiniPlayerAction) {
        when (action) {
            MiniPlayerAction.TogglePlay -> playbackController.toggle()
            MiniPlayerAction.Close -> playbackController.close()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                MiniPlayerViewModel((this[APPLICATION_KEY] as WAKordApp).container.playbackController)
            }
        }
    }
}
