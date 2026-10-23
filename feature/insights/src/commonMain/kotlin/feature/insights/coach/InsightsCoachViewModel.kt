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
import domain.word.usecase.GetProgressStatsUseCase
import domain.word.usecase.ObserveReviewForecastUseCase
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.ForecastUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.StagesUi
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
    /** From local words, so it shows offline and before the server answers. */
    val forecast: ForecastUi? = null,
    val stages: StagesUi? = null,
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
    val observeForecast: ObserveReviewForecastUseCase,
    val observeProgress: GetProgressStatsUseCase,
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
        combine(
            useCases.observeScreen(Unit),
            useCases.observeDismissed(Unit),
        ) { cached, dismissed -> cached to dismissed }
            .onEach { (cached, dismissed) -> render(cached, dismissed) }
            .launchIn(viewModelScope)
        useCases.observeForecast(Unit)
            .onEach { updateState { copy(forecast = InsightsUiMapper.forecast(it)) } }
            .launchIn(viewModelScope)
        useCases.observeProgress(Unit)
            .onEach { updateState { copy(stages = InsightsUiMapper.stages(it)) } }
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
            is CoachAction.ReviewWords ->
                emitEffect(InsightsCoachEffect.StartReview(ReviewSource.ByWords(action.wordIds)))
            is CoachAction.StartReview -> emitEffect(InsightsCoachEffect.StartReview(ReviewSource.DueCards))
            is CoachAction.StartWordRush -> emitEffect(InsightsCoachEffect.StartWordRush)
            is CoachAction.EnableReminder -> enableReminders(card)
            CoachAction.None -> Unit
        }
    }

    fun dismissCard(card: CoachCardUi) {
        analyticsTracker.logEvent("coach_card_dismissed", mapOf("type" to card.type))
        viewModelScope.launch { useCases.dismissCard(card.id) }
    }

    fun reviewDueNow() {
        analyticsTracker.logEvent("insights_review_due", emptyMap())
        emitEffect(InsightsCoachEffect.StartReview(ReviewSource.DueCards))
    }

    fun reviewHardest() {
        val ids = currentState.content?.hardest?.map { it.id }.orEmpty()
        if (ids.isEmpty()) return
        analyticsTracker.logEvent("insights_review_hardest", mapOf("count" to ids.size.toString()))
        emitEffect(InsightsCoachEffect.StartReview(ReviewSource.ByWords(ids)))
    }

    private fun enableReminders(card: CoachCardUi) {
        viewModelScope.launch {
            useCases.setReviewReminders(true).fold(
                onSuccess = {
                    // Done: the card has nothing left to offer today
                    useCases.dismissCard(card.id)
                    emitEffect(InsightsCoachEffect.ReminderEnabled)
                },
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
