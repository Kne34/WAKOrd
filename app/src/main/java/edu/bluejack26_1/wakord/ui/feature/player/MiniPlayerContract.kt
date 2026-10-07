package edu.bluejack26_1.wakord.ui.feature.player

sealed interface MiniPlayerAction {
    data object TogglePlay : MiniPlayerAction
    data object Close : MiniPlayerAction
}
