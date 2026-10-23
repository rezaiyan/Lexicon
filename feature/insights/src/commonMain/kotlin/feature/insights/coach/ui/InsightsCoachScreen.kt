package feature.insights.coach.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import components.SectionHeader
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
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.insights_coach_next_steps
import lexicon.resources.generated.resources.insights_coach_section_words
import lexicon.resources.generated.resources.insights_coach_updated_days
import lexicon.resources.generated.resources.insights_coach_updated_hours
import lexicon.resources.generated.resources.insights_coach_updated_just_now
import lexicon.resources.generated.resources.insights_coach_updated_minutes
import lexicon.resources.generated.resources.insights_coach_welcome_action
import lexicon.resources.generated.resources.insights_coach_welcome_body
import lexicon.resources.generated.resources.insights_coach_welcome_title
import lexicon.resources.generated.resources.insights_coming_up
import lexicon.resources.generated.resources.insights_offline
import lexicon.resources.generated.resources.insights_tab
import lexicon.resources.generated.resources.leaderboard
import lexicon.resources.generated.resources.retry
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import theme.Theme

/**
 * Insights tab. Answers three questions, each with something to tap: how this week went (hero),
 * what to do next (coach cards), and whether the learner keeps up and retains words (forecast and
 * library, both from local words so they show offline).
 */
@Composable
fun InsightsCoachScreen(
    onShowLeaderboard: () -> Unit,
    onStartReview: (ReviewSource) -> Unit,
    onStartWordRush: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onReminderEnabled: () -> Unit,
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
            InsightsCoachEffect.ReminderEnabled -> onReminderEnabled()
        }
    }

    InsightsCoachContent(
        state = state,
        onShowLeaderboard = onShowLeaderboard,
        onRetry = viewModel::refresh,
        onAction = viewModel::onCoachAction,
        onDismiss = viewModel::dismissCard,
        onReviewDueNow = viewModel::reviewDueNow,
        onReviewHardest = viewModel::reviewHardest,
    )
}

@Composable
internal fun InsightsCoachContent(
    state: InsightsCoachState,
    onShowLeaderboard: () -> Unit,
    onRetry: () -> Unit,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    onReviewDueNow: () -> Unit,
    onReviewHardest: () -> Unit,
) {
    LexiconColumn(
        title = stringResource(Res.string.insights_tab),
        scrollable = false,
        topBarColor = TopBarColor.Background,
        actionIcon1 = ActionIconConfig(
            icon = Icons.Rounded.EmojiEvents,
            contentDescription = stringResource(Res.string.leaderboard),
            onClick = onShowLeaderboard,
            size = Theme.dimensions.iconSize,
        ),
    ) {
        LoadedContent(state, onRetry, onAction, onDismiss, onReviewDueNow, onReviewHardest)
    }
}

@Composable
private fun LoadedContent(
    state: InsightsCoachState,
    onRetry: () -> Unit,
    onAction: (CoachCardUi) -> Unit,
    onDismiss: (CoachCardUi) -> Unit,
    onReviewDueNow: () -> Unit,
    onReviewHardest: () -> Unit,
) {
    val content = state.content
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        state.updatedAgo?.takeIf { state.isStale }?.let { UpdatedAgoLabel(it) }
        when {
            // No reviews yet: nothing to summarise, but the forecast and words below already apply.
            content?.isNewUser == true -> WelcomeCard(onReviewDueNow)
            content != null -> HeroSection(content.hero)
            state.error != null -> OfflineNotice(onRetry)
            else -> HeroPlaceholder()
        }
        // A new learner's only next step is the welcome card's first review
        if (content != null && !content.isNewUser && content.coach.isNotEmpty()) {
            Section(stringResource(Res.string.insights_coach_next_steps)) {
                content.coach.forEach { card -> CoachCardItem(card, onAction, onDismiss) }
            }
        }
        state.forecast?.takeIf { state.stages != null }?.let { forecast ->
            Section(stringResource(Res.string.insights_coming_up)) {
                // The welcome card already offers the first review
                ForecastSection(forecast, onReviewDueNow, showReviewNow = content?.isNewUser != true)
            }
        }
        state.stages?.let { stages ->
            Section(stringResource(Res.string.insights_coach_section_words)) {
                LibrarySection(stages, content?.hardest.orEmpty(), onReviewHardest)
            }
        }
        Spacer(Modifier.height(Theme.spacing.xl))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(top = Theme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sectionHeaderGap),
    ) {
        SectionHeader(title = title)
        content()
    }
}

/** Plain raised panel shared by the Insights blocks. */
@Composable
internal fun InsightsPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Theme.elevation.low,
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            content = content,
        )
    }
}

@Composable
private fun HeroPlaceholder() {
    InsightsPanel {
        Box(Modifier.fillMaxWidth().height(Theme.dimensions.profilePictureSize), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

/** Shown in place of the week summary when it can't load; local blocks below still work. */
@Composable
private fun OfflineNotice(onRetry: () -> Unit) {
    InsightsPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.insights_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
        }
    }
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
private fun WelcomeCard(onStartFirstReview: () -> Unit) {
    InsightsPanel {
        Text(
            text = stringResource(Res.string.insights_coach_welcome_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(Res.string.insights_coach_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onStartFirstReview) { Text(stringResource(Res.string.insights_coach_welcome_action)) }
    }
}
