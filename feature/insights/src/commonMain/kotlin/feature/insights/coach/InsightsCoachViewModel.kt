package feature.insights.coach

import analytics.IAnalyticsTracker
import androidx.lifecycle.viewModelScope
import core.base.BaseViewModel
import core.common.fold
import core.error.toUserMessage
import domain.insights.model.CachedInsights
import domain.insights.model.CoachAction
import domain.insights.usecase.DismissCoachCardUseCase
import domain.insights.usecase.ObserveDismissedCoachCardsUseCase
import domain.insights.usecase.ObserveInsightsScreenUseCase
import domain.insights.usecase.RefreshInsightsScreenUseCase
import domain.settings.usecase.SetReviewRemindersEnabledUseCase
import domain.word.model.ReviewSource
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.SectionKind
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class InsightsCoachState(
    val content: InsightsUiModel? = null,
    val isRefreshing: Boolean = false,
    /** Set only when there is nothing cached to show. */
    val error: String? = null,
    /** Cached content shown after a failed refresh. */
    val isStale: Boolean = false,
    val updatedAgo: UpdatedAgo? = null,
    val expanded: Set<SectionKind> = emptySet(),
)

sealed interface InsightsCoachEffect {
    data class StartReview(val source: ReviewSource) : InsightsCoachEffect
    data object StartWordRush : InsightsCoachEffect
    data object ReminderEnabled : InsightsCoachEffect
    data object OpenNotificationSettings : InsightsCoachEffect
}

class InsightsCoachUseCases(
    val observeScreen: ObserveInsightsScreenUseCase,
    val refreshScreen: RefreshInsightsScreenUseCase,
    val observeDismissed: ObserveDismissedCoachCardsUseCase,
    val dismissCard: DismissCoachCardUseCase,
    val setReviewReminders: SetReviewRemindersEnabledUseCase,
)

/** Time sources, injectable for tests. */
class InsightsClock(
    val timeZoneId: () -> String = { TimeZone.currentSystemDefault().id },
    val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    val today: () -> LocalDate = { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date },
)

class InsightsCoachViewModel(
    private val useCases: InsightsCoachUseCases,
    private val analyticsTracker: IAnalyticsTracker,
    private val clock: InsightsClock = InsightsClock(),
    private val use24Hour: () -> Boolean,
) : BaseViewModel<InsightsCoachState, InsightsCoachEffect>() {

    private val trackedShown = mutableSetOf<String>()

    override fun initialState() = InsightsCoachState()

    init {
        combine(useCases.observeScreen(Unit), useCases.observeDismissed(Unit)) { cached, dismissed -> cached to dismissed }
            .onEach { (cached, dismissed) -> render(cached, dismissed) }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        if (currentState.isRefreshing) return
        viewModelScope.launch {
            updateState { copy(isRefreshing = true) }
            useCases.refreshScreen(clock.timeZoneId()).reduce(
                onSuccess = { copy(isRefreshing = false, error = null, isStale = false) },
                onFailure = { error ->
                    if (content == null) copy(isRefreshing = false, error = error.toUserMessage())
                    else copy(isRefreshing = false, isStale = true)
                },
            )
        }
        analyticsTracker.logEvent("insights_viewed", emptyMap())
    }

    fun onCoachAction(card: CoachCardUi) {
        analyticsTracker.logEvent("coach_card_action", mapOf("type" to card.type))
        when (val action = card.action) {
            is CoachAction.ReviewWords -> emitEffect(InsightsCoachEffect.StartReview(ReviewSource.ByWords(action.wordIds)))
            is CoachAction.StartReview -> emitEffect(InsightsCoachEffect.StartReview(ReviewSource.DueCards))
            is CoachAction.StartWordRush -> emitEffect(InsightsCoachEffect.StartWordRush)
            is CoachAction.EnableReminder -> enableReminders()
            CoachAction.None -> Unit
        }
    }

    fun dismissCard(card: CoachCardUi) {
        analyticsTracker.logEvent("coach_card_dismissed", mapOf("type" to card.type))
        viewModelScope.launch { useCases.dismissCard(card.id) }
    }

    fun toggleSection(kind: SectionKind) {
        updateState { copy(expanded = if (kind in expanded) expanded - kind else expanded + kind) }
    }

    private fun enableReminders() {
        viewModelScope.launch {
            useCases.setReviewReminders(true).fold(
                onSuccess = { emitEffect(InsightsCoachEffect.ReminderEnabled) },
                onFailure = { emitEffect(InsightsCoachEffect.OpenNotificationSettings) },
            )
        }
    }

    private fun render(cached: CachedInsights?, dismissed: Set<String>) {
        if (cached == null) return
        val content = InsightsUiMapper.map(cached.screen, dismissed, clock.today(), use24Hour())
        content.coach.filter { trackedShown.add(it.id) }
            .forEach { analyticsTracker.logEvent("coach_card_shown", mapOf("type" to it.type)) }
        updateState {
            copy(
                content = content,
                error = null,
                updatedAgo = InsightsFormatter.updatedAgo(cached.fetchedAtMs, clock.nowMs()),
            )
        }
    }
}
