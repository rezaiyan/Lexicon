package feature.insights

import core.common.UiState

/**
 * Determines which insights tabs have content to display.
 *
 * Computed from [InsightsState] after data loads. Used by the screen
 * to hide empty tabs and by the CTA to decide visibility.
 */
data class InsightsAvailability(
    val hasOverview: Boolean,
    val hasTrends: Boolean,
    val hasWords: Boolean,
    val hasWordRush: Boolean,
) {
    val hasAnyContent: Boolean
        get() = hasOverview || hasTrends || hasWords || hasWordRush

    companion object {
        fun from(state: InsightsState): InsightsAvailability = InsightsAvailability(
            hasOverview = state.overview.hasNonEmptyData { it.totalCardsReviewed > 0 },
            hasTrends = state.accuracyByLevel.hasNonEmptyList()
                    || state.heatmap.hasNonEmptyList()
                    || state.levelTransitions.hasNonEmptyList()
                    || state.responseTimeTrend.hasNonEmptyList(),
            hasWords = state.difficultWords.hasNonEmptyList(),
            hasWordRush = state.wordRushInsights.hasNonEmptyData { it.totalGames > 0 },
        )

        private fun <T> UiState<T>.hasNonEmptyData(predicate: (T) -> Boolean): Boolean =
            this is UiState.Loaded && predicate(value)

        private fun <T> UiState<List<T>>.hasNonEmptyList(): Boolean =
            this is UiState.Loaded && value.isNotEmpty()
    }
}
