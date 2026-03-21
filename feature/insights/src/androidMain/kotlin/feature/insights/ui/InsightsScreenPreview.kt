package feature.insights.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import core.common.UiState
import domain.analytics.model.AccuracyByLevel
import domain.analytics.model.HourlyAccuracy
import domain.analytics.model.StudyHeatmapDay
import domain.analytics.model.StudyInsights
import domain.analytics.model.WordDifficulty
import feature.insights.InsightsState
import theme.LexiconTheme

private val previewState = InsightsState(
    overview = UiState.Loaded(
        StudyInsights(
            totalCardsReviewed = 1_240,
            totalCorrect = 1_054,
            accuracyPercent = 85.0,
            totalStudyTimeMs = 7_200_000,
            totalSessions = 42,
            daysStudied = 18,
            uniqueWordsReviewed = 320,
            averageResponseTimeMs = 1_800,
            averageSessionDurationMs = 171_428,
            wordsMasteredCount = 87,
            sessionCompletionRate = 0.0
        )
    ),
    accuracyByLevel = UiState.Loaded(
        listOf(
            AccuracyByLevel(level = 1, totalReviews = 120, correctCount = 96, accuracyPercent = 80.0),
            AccuracyByLevel(level = 2, totalReviews = 98, correctCount = 84, accuracyPercent = 85.7),
            AccuracyByLevel(level = 3, totalReviews = 76, correctCount = 68, accuracyPercent = 89.5),
            AccuracyByLevel(level = 4, totalReviews = 54, correctCount = 50, accuracyPercent = 92.6),
            AccuracyByLevel(level = 5, totalReviews = 30, correctCount = 29, accuracyPercent = 96.7),
        )
    ),
    difficultWords = UiState.Loaded(
        listOf(
            WordDifficulty(
                wordId = 1, wordText = "ephemeral", wordTranslation = "lasting a very short time",
                sourceLanguage = "en", targetLanguage = "en", totalReviews = 12, errorCount = 7, errorRate = 0.58,
            ),
            WordDifficulty(
                wordId = 2, wordText = "melancholy", wordTranslation = "deep sadness",
                sourceLanguage = "en", targetLanguage = "en", totalReviews = 10, errorCount = 5, errorRate = 0.50,
            ),
            WordDifficulty(
                wordId = 3, wordText = "sycophant", wordTranslation = "a person who flatters",
                sourceLanguage = "en", targetLanguage = "en", totalReviews = 8, errorCount = 4, errorRate = 0.50,
            ),
            WordDifficulty(
                wordId = 4, wordText = "quixotic", wordTranslation = "exceedingly idealistic",
                sourceLanguage = "en", targetLanguage = "en", totalReviews = 9, errorCount = 4, errorRate = 0.44,
            ),
        )
    ),
    heatmap = UiState.Loaded(
        listOf(
            StudyHeatmapDay(date = "2026-03-14", count = 12),
            StudyHeatmapDay(date = "2026-03-15", count = 0),
            StudyHeatmapDay(date = "2026-03-16", count = 8),
            StudyHeatmapDay(date = "2026-03-17", count = 21),
            StudyHeatmapDay(date = "2026-03-18", count = 5),
            StudyHeatmapDay(date = "2026-03-19", count = 17),
            StudyHeatmapDay(date = "2026-03-20", count = 9),
        )
    ),
    bestStudyTime = UiState.Loaded(
        HourlyAccuracy(hour = 9, totalReviews = 280, correctCount = 245, accuracyPercent = 87.5)
    ),
)

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InsightsScreenPreview() {
    LexiconTheme {
        InsightsContent(
            state = previewState,
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Insights - Loading")
@Composable
private fun InsightsScreenLoadingPreview() {
    LexiconTheme {
        InsightsContent(
            state = InsightsState(),
            onNavigateBack = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Insights - Dark")
@Composable
private fun InsightsScreenDarkPreview() {
    LexiconTheme(darkTheme = true) {
        InsightsContent(
            state = previewState,
            onNavigateBack = {},
        )
    }
}
