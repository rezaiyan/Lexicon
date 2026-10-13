package feature.study.listening.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the full listening player is on screen. The mini player hides while it is, so the
 * session is never shown twice. Set by the player overlay for as long as it's composed.
 */
class ListeningPlayerVisibility {
    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    fun setOpen(open: Boolean) {
        _isOpen.value = open
    }
}
