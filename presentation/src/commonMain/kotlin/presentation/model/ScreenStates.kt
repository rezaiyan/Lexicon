package presentation.model

import domain.word.model.ProgressEvaluation
import domain.word.model.ProgressStats
import domain.word.model.Word
import domain.settings.model.ThemeMode
import core.common.UiState
import utils.Language


data class ProgressScreenState(
    val progressStats: ProgressStats,
    val progressEvaluation: ProgressEvaluation,
    val messageState: MessageState? = null
)

sealed class MessageState {
    data class Error(val message: String) : MessageState()
}

data class SettingsScreenState(
    val currentLanguage: Language = Language.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val notificationsEnabled: Boolean = true,
    val systemNotificationsEnabled: Boolean = true,
    val isPremiumFeatureEnabled: Boolean = false,
    val appVersion: String = "Loading.."
)

data class ReviewScreenState(
    val wordListState: UiState<List<Word>> = UiState.Loading,
    val reviewType: ReviewType = ReviewType.REVIEW
)