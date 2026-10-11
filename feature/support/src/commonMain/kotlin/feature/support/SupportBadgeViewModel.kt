package feature.support

import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import domain.support.model.SupportUnread
import domain.support.usecase.MarkSupportSeenInSettingsUseCase
import domain.support.usecase.ObserveSupportUnreadUseCase
import kotlinx.coroutines.launch

/**
 * Unread dots for support replies outside the chat: on the Settings icon until Settings is opened,
 * and on the Help & Support row until the chat is read.
 */
class SupportBadgeViewModel(
    observeUnread: ObserveSupportUnreadUseCase,
    private val markSeenInSettings: MarkSupportSeenInSettingsUseCase,
) : BaseViewModel<SupportUnread, Nothing>() {

    override fun initialState(): SupportUnread = SupportUnread()

    init {
        viewModelScope.launch {
            observeUnread(Unit).collect { unread -> updateState { unread } }
        }
    }

    /** Settings is on screen: the reply has been noticed there, so the Settings icon dot goes. */
    fun onSettingsOpened() {
        viewModelScope.launch { markSeenInSettings(Unit) }
    }
}
