package feature.insights.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.ErrorScreen
import components.LoadingScreen
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import domain.word.model.ReviewSource
import events.OnEvents
import feature.insights.coach.InsightsCoachEffect
import feature.insights.coach.InsightsCoachState
import feature.insights.coach.InsightsCoachViewModel
import feature.insights.coach.UpdatedAgo
import feature.insights.coach.model.CoachCardUi
import feature.insights.coach.model.InsightsUiModel
import feature.insights.coach.model.SectionKind
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_deep_dive
import lexicon.resources.generated.resources.insights_coach_next_steps
import lexicon.resources.generated.resources.insights_coach_updated_days
import lexicon.resources.generated.resources.insights_coach_updated_hours
import lexicon.resources.generated.resources.insights_coach_updated_just_now
import lexicon.resources.generated.resources.insights_coach_updated_minutes
import lexicon.resources.generated.resources.insights_coach_welcome_action
import lexicon.resources.generated.resources.insights_coach_welcome_body
import lexicon.resources.generated.resources.insights_coach_welcome_title
import lexicon.resources.generated.resources.insights_loading
import lexicon.resources.generated.resources.insights_title
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

@Composable
fun InsightsCoachScreen(
    onShowLeaderboard: () -> Unit,
    onStartReview: (ReviewSource) -> Unit,
    onStartWordRush: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val viewModel = koinViewModel<InsightsCoachViewModel>()
    val state by viewModel.state()

    // Refresh every time the tab is shown; cached content renders immediately.
    LaunchedEffect(Unit) { viewModel.refresh() }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is InsightsCoachEffect.StartReview -> onStartReview(effect.source)
            InsightsCoachEffect.StartWordRush -> onStartWordRush()
            InsightsCoachEffect.OpenNotificationSettings -> onOpenNotificationSettings()
            InsightsCoachEffect.ReminderEnabled -> Unit
        }
    }

    InsightsCoachContent(
        state = state,
        onShowLeaderboard = onShowLeaderboard,
        onRetry = viewModel::refresh,
        onAction = viewModel::onCoachAction,
        onDismiss = viewModel::dismissCard,
        onToggleSection = viewModel::toggleSection,
        onStartFirstReview = { onStartReview(ReviewSource.DueCards) },
    )
}

@Composable
internal fun InsightsCoachContent(
    state: InsightsCoachState,
    onShowLeaderboard: () -> Unit,
    onRetry: () -> Unit,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    onToggleSection: (SectionKind) -> Unit,
    onStartFirstReview: () -> Unit,
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_title),
        scrollable = false,
        topBarColor = TopBarColor.Background,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Rounded.EmojiEvents,
            contentDescription = stringResource(Res.string.leaderboard),
            onClick = onShowLeaderboard,
            size = Theme.dimensions.iconSize,
        ),
    ) {
        val content = state.content
        val error = state.error
        when {
            content == null && error != null ->
                ErrorScreen(message = error, retryLabel = stringResource(Res.string.retry), onRetry = onRetry)
            content == null -> LoadingScreen(message = stringResource(Res.string.insights_loading))
            content.isNewUser -> WelcomeContent(onStartFirstReview)
            else -> LoadedContent(content, state, onAction, onDismiss, onToggleSection)
        }
    }
}

@Composable
private fun LoadedContent(
    content: InsightsUiModel,
    state: InsightsCoachState,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    onToggleSection: (SectionKind) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        state.updatedAgo?.takeIf { state.isStale }?.let { UpdatedAgoLabel(it) }
        HeroSection(content.hero)
        if (content.coach.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.insights_coach_next_steps))
            content.coach.forEach { card -> CoachCardItem(card, onAction, onDismiss) }
        }
        if (content.sections.isNotEmpty()) {
            SectionHeader(stringResource(Res.string.insights_coach_deep_dive))
            content.sections.forEach { section ->
                DeepDiveSection(section, section.kind in state.expanded, onToggleSection)
            }
        }
        Spacer(Modifier.height(Theme.spacing.xl))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UpdatedAgoLabel(ago: UpdatedAgo) {
    val text = when (ago) {
        UpdatedAgo.JustNow -> stringResource(Res.string.insights_coach_updated_just_now)
        is UpdatedAgo.Minutes -> stringResource(Res.string.insights_coach_updated_minutes, ago.value)
        is UpdatedAgo.Hours -> stringResource(Res.string.insights_coach_updated_hours, ago.value)
        is UpdatedAgo.Days -> stringResource(Res.string.insights_coach_updated_days, ago.value)
    }
    Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun WelcomeContent(onStartFirstReview: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(horizontal = Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Text(
                text = stringResource(Res.string.insights_coach_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.insights_coach_welcome_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onStartFirstReview) { Text(stringResource(Res.string.insights_coach_welcome_action)) }
        }
    }
}
