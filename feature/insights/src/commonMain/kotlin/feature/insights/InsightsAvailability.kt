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
) {
    val visibleTabs: List<InsightsTab>
        get() = buildList {
            if (hasOverview) add(InsightsTab.OVERVIEW)
            if (hasTrends) add(InsightsTab.TRENDS)
            if (hasWords) add(InsightsTab.WORDS)
        }

    val hasAnyContent: Boolean
        get() = hasOverview || hasTrends || hasWords

    companion object {
        fun from(state: InsightsState): InsightsAvailability = InsightsAvailability(
            hasOverview = state.overview.hasNonEmptyData { it.totalCardsReviewed > 0 },
            hasTrends = state.accuracyByLevel.hasNonEmptyList()
                    || state.heatmap.hasNonEmptyList(),
            hasWords = state.difficultWords.hasNonEmptyList(),
        )

        private fun <T> UiState<T>.hasNonEmptyData(predicate: (T) -> Boolean): Boolean =
            this is UiState.Loaded && predicate(value)

        private fun <T> UiState<List<T>>.hasNonEmptyList(): Boolean =
            this is UiState.Loaded && value.isNotEmpty()
    }
}
