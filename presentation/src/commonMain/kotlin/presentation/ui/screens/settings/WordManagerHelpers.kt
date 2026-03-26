package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import domain.word.model.ImportErrorClassification
import domain.word.model.LearningStage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.empty_library
import lexicon.resources.generated.resources.error
import lexicon.resources.generated.resources.level_0_fresh
import lexicon.resources.generated.resources.level_1_learning
import lexicon.resources.generated.resources.level_2_familiar
import lexicon.resources.generated.resources.level_3_building
import lexicon.resources.generated.resources.level_4_almost
import lexicon.resources.generated.resources.level_5_strong
import lexicon.resources.generated.resources.level_6_mastered
import lexicon.resources.generated.resources.loading_words
import lexicon.resources.generated.resources.no_results_found
import lexicon.resources.generated.resources.start_by_importing
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
internal fun LoadingView() {
    LoadingScreen(message = stringResource(Res.string.loading_words))
}

@Composable
internal fun ErrorView(message: String, classification: ImportErrorClassification = ImportErrorClassification.GenericError) {
    val isNetworkError = classification is ImportErrorClassification.NetworkError
    ErrorScreen(
        message = if (isNetworkError) {
            "You're offline -- your word library couldn't be loaded. Check your connection and try again."
        } else {
            message.ifEmpty { "Something went wrong loading your words." }
        },
        title = if (isNetworkError) "No Connection" else stringResource(Res.string.error),
        icon = Icons.Default.Error,
        retryLabel = "Try Again",
    )
}

@Composable
internal fun EmptyLibraryView() {
    EmptyScreen(
        title = stringResource(Res.string.empty_library),
        subtitle = stringResource(Res.string.start_by_importing),
        icon = {
            Icon(
                imageVector = Icons.Default.FileUpload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}

@Composable
internal fun EmptySearchView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.touchTarget),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.no_results_found),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Try a different spelling or search by translation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

internal fun formatDateAdded(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val timeZone = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(timeZone)
    val month = localDateTime.month.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    return "${localDateTime.dayOfMonth} $month ${localDateTime.year}"
}

internal fun levelColor(stage: LearningStage): Color {
    return when (stage) {
        LearningStage.LEVEL_0_FRESH -> AppColors.novice
        LearningStage.LEVEL_1_LEARNING -> AppColors.apprentice
        LearningStage.LEVEL_2_FAMILIAR -> AppColors.apprentice
        LearningStage.LEVEL_3_BUILDING -> AppColors.adept
        LearningStage.LEVEL_4_ALMOST -> AppColors.adept
        LearningStage.LEVEL_5_STRONG -> AppColors.master
        LearningStage.LEVEL_6_MASTERED -> AppColors.master
    }
}

@Composable
internal fun stageName(stage: LearningStage): String {
    return when (stage) {
        LearningStage.LEVEL_0_FRESH -> stringResource(Res.string.level_0_fresh)
        LearningStage.LEVEL_1_LEARNING -> stringResource(Res.string.level_1_learning)
        LearningStage.LEVEL_2_FAMILIAR -> stringResource(Res.string.level_2_familiar)
        LearningStage.LEVEL_3_BUILDING -> stringResource(Res.string.level_3_building)
        LearningStage.LEVEL_4_ALMOST -> stringResource(Res.string.level_4_almost)
        LearningStage.LEVEL_5_STRONG -> stringResource(Res.string.level_5_strong)
        LearningStage.LEVEL_6_MASTERED -> stringResource(Res.string.level_6_mastered)
    }
}
