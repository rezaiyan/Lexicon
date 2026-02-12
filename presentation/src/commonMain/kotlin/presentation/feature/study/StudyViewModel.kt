@file:OptIn(ExperimentalTime::class)

package presentation.feature.study

import analytics.IAnalyticsTracker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.notifications.usecase.ScheduleNotificationsUseCase
import domain.word.model.ProgressStats
import domain.word.usecase.GetDueWordsUseCase
import domain.word.usecase.GetProgressStatsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import presentation.model.MessageState
import presentation.model.ProgressScreenState
import presentation.util.NotificationStringHelper
import kotlin.time.ExperimentalTime

class StudyViewModel(
    private val getProgressStatsUseCase: GetProgressStatsUseCase,
    private val scheduleNotificationsUseCase: ScheduleNotificationsUseCase,
    private val getDueWordsUseCase: GetDueWordsUseCase,
    private val analyticsTracker: IAnalyticsTracker
) : ViewModel() {

    private val _progressStatistics = MutableStateFlow<ProgressStats?>(null)

    // Message state for UI feedback
    private val _messageState = MutableStateFlow<MessageState?>(null)

    // Notification settings (passed in for scheduling)
    private val notificationsEnabled = true
    private val systemNotificationsEnabled = true

    // Consolidated Progress Screen State
    private val _progressScreenState = MutableStateFlow(ProgressScreenState())
    val progressScreenState: StateFlow<ProgressScreenState> = _progressScreenState.asStateFlow()

    private val _events = Channel<StudyEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeAndCombineStates()
        startObservingProgress()
    }

    private fun startObservingProgress() {
        viewModelScope.launch {
            getProgressStatsUseCase.invoke()
                .collect { stats ->
                    _progressStatistics.value = stats

                    // Update analytics when stats change
                    analyticsTracker.updateUserProgress(
                        totalWords = stats.totalWords,
                        matureWords = stats.matureWords,
                        currentStreak = 0
                    )

                    // Reschedule notifications when stats change
                    if (notificationsEnabled && systemNotificationsEnabled) {
                        val notifStrings =
                            NotificationStringHelper.getNotificationResources(
                                stats.dueCards
                            )
                        val title = getString(
                            notifStrings.titleRes,
                            *notifStrings.titleParams.toTypedArray()
                        )
                        val message = getString(
                            notifStrings.messageRes,
                            *notifStrings.messageParams.toTypedArray()
                        )
                        scheduleNotificationsUseCase(
                            stats = stats,
                            titleProvider = { title },
                            messageProvider = { message }
                        )
                    }
                }
        }
    }

    private fun observeAndCombineStates() {
        viewModelScope.launch {
            combine(
                _progressStatistics,
                _messageState
            ) { stats, message ->
                ProgressScreenState(
                    progressStats = stats,
                    messageState = message
                )
            }.collect { _progressScreenState.value = it }
        }
    }

    fun startReview() {
        viewModelScope.launch {
            try {
                val words = getDueWordsUseCase().first()
                val firstWord = words.firstOrNull()
                if (firstWord != null) {
                    _events.send(StudyEvent.StartReview(firstWord))
                }
            } catch (_: Exception) {
                // Error loading due words for review
            }
        }
    }

}

