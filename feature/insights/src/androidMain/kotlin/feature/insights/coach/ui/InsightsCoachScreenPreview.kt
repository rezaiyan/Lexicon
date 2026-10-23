package feature.insights.coach.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import domain.insights.model.CoachAction
import domain.insights.model.InsightWord
import domain.insights.model.WordChip
import feature.insights.coach.InsightsCoachState
import feature.insights.coach.model.ChangeUnit
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.DayDotUi
import feature.insights.coach.model.ForecastBarUi
import feature.insights.coach.model.ForecastUi
import feature.insights.coach.model.HeroUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.StagesUi
import feature.insights.coach.model.StatUi
import feature.insights.coach.model.Trend
import theme.LexiconTheme

private val previewState = InsightsCoachState(
    content = InsightsUiModel(
        hero = HeroUi(
            headline = "Your best week in a month",
            reviews = StatUi("212", Trend.UP, 18, ChangeUnit.COUNT),
            accuracy = StatUi("84%", Trend.UP, 5, ChangeUnit.POINTS),
            leveledUp = StatUi("31", Trend.UP, 9, ChangeUnit.COUNT),
            streak = 5,
            week = (1..7).map { DayDotUi(it, if (it <= 5) 30 else 0, isToday = it == 5, isFuture = it > 5) },
        ),
        coach = listOf(
            CoachCardUi(
                id = "SLIPPING_WORDS:2026-10-23",
                type = "SLIPPING_WORDS",
                title = "12 words need a refresh",
                body = "They dropped a level this week. A quick review locks them back in.",
                words = listOf(WordChip(1, "Katze"), WordChip(2, "gehen"), WordChip(3, "Hund")),
                moreWordsCount = 9,
                action = CoachAction.ReviewWords("Review 12 words", (1L..12L).toList()),
                hourLabel = null,
            ),
        ),
        hardest = listOf(InsightWord(1, "ubiquitous", 33), InsightWord(2, "Eichhörnchen", 40)),
        isNewUser = false,
    ),
    forecast = ForecastUi(
        dueNow = 29,
        tomorrow = 17,
        busiest = 41,
        bars = listOf(29, 17, 9, 41, 6, 12, 3).mapIndexed { i, n ->
            ForecastBarUi((i + 4) % 7 + 1, n, n / 41f, isToday = i == 0, isBusiest = n == 41)
        },
    ),
    stages = StagesUi(new = 40, learning = 70, strong = 60, mastered = 30),
)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun InsightsCoachContentPreview() {
    LexiconTheme {
        InsightsCoachContent(
            state = previewState,
            onShowLeaderboard = {},
            onRetry = {},
            onAction = {},
            onDismiss = {},
            onReviewDueNow = {},
            onReviewHardest = {},
        )
    }
}
