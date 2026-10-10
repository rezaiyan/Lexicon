package presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import components.ErrorScreen
import components.LoadingScreen
import components.SectionHeader
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import components.sheet.SheetBadge
import components.sheet.SheetGroup
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetSwitchRow
import core.common.UiState
import core.getPlatformName
import domain.focus.model.LearningFocus
import domain.tag.model.Tag
import domain.word.model.ProgressTier
import domain.word.model.ReviewSource
import events.OnEvents
import feature.study.ReviewEffect
import feature.study.ReviewState
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import feature.study.listening.ListeningViewModel
import feature.study.ui.focus.FocusIntroCard
import feature.study.ui.focus.FocusLanguageIcon
import feature.study.ui.focus.FocusNudgeCard
import feature.study.ui.focus.LanguageSwitcherSheetContent
import feature.study.ui.listening.ListeningCard
import feature.study.ui.listening.ListeningScreen
import feature.study.ui.review.ReviewScreen
import feature.study.ui.study.CollapsedStatsBar
import feature.study.ui.study.LearningStagesSection
import feature.study.ui.study.StatsSection
import feature.study.ui.study.TagsSection
import feature.study.ui.wordrush.WordRushCard
import feature.study.ui.wordrush.WordRushGameScreen
import feature.study.wordrush.WordRushEffect
import feature.study.wordrush.WordRushViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.filter_tag
import lexicon.resources.generated.resources.focus_all_languages
import lexicon.resources.generated.resources.focus_current
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.no_connection
import lexicon.resources.generated.resources.offline_changes_will_sync
import lexicon.resources.generated.resources.retry
import lexicon.resources.generated.resources.review_all_due
import lexicon.resources.generated.resources.review_all_in_stage
import lexicon.resources.generated.resources.settings
import lexicon.resources.generated.resources.skip_tag_selector_label
import lexicon.resources.generated.resources.start_review
import lexicon.resources.generated.resources.study_error_title
import lexicon.resources.generated.resources.study_load_failed
import lexicon.resources.generated.resources.study_loading
import lexicon.resources.generated.resources.study_practice
import lexicon.resources.generated.resources.word_count_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import overlay.LocalOverlayHost
import overlay.bottomsheet.BottomSheetProperties
import overlay.bottomsheet.showSizeToFitBottomSheet
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import presentation.navigation.NotificationNavigator
import presentation.ui.components.imports.AddWordsSheet
import theme.Theme

/** Non-dismissable sheet configuration reused for import flows. */
private val LockedSheetProperties = BottomSheetProperties(
    dismissOnTouchOutside = false,
    dismissOnBackPress = false,
    isNavigationBarsPaddingEnabled = true,
    sheetGesturesEnabled = false,
    showDragHandle = false,
)

private val DismissableSheetProperties = BottomSheetProperties(
    dismissOnBackPress = true,
    dismissOnTouchOutside = true,
)

private val NetworkErrorHints = listOf("timeout", "connect", "network", "internet")

@Composable
fun StudyScreen(
    onNavigateToSettings: () -> Unit,
    onOpenSubscription: () -> Unit,
) {
    val progressViewModel = koinViewModel<StudyProgressViewModel>()
    val reviewViewModel = koinViewModel<ReviewViewModel>()
    val wordRushViewModel = koinViewModel<WordRushViewModel>()
    val listeningViewModel = koinViewModel<ListeningViewModel>()
    val overlayHost = LocalOverlayHost.current

    val progressState by progressViewModel.state()
    val uiState = progressState.progress
    val skipTagSelector = progressState.skipTagSelector

    // WasmJs has no on-device TTS engine, so listening mode is mobile-only.
    val isListeningSupported = remember { getPlatformName() != "Web" }

    val scrollState = rememberScrollState()
    var statsSectionBottom by remember { mutableIntStateOf(0) }
    val isStatsSectionScrolledAway by remember {
        derivedStateOf { statsSectionBottom > 0 && scrollState.value > statsSectionBottom }
    }

    val loadedProgress = (uiState as? UiState.Loaded)?.value

    // Single entry point for all review flows.
    val openReviewScreen: (ReviewSource) -> Unit = { source ->
        reviewViewModel.startSession(source)
        overlayHost.showFullScreen(
            tag = "review-${source::class.simpleName}",
            // ReviewScreen paints its background edge to edge and applies the system-bar insets itself.
            properties = FullScreenProperties(
                dismissOnBackPress = false,
                isStatusBarsPaddingEnabled = false,
                isNavigationBarsPaddingEnabled = false,
            ),
        ) { navigator ->
            OnEvents(reviewViewModel.effects) { effect ->
                when (effect) {
                    ReviewEffect.SessionComplete -> {
                        progressViewModel.refreshStats()
                        navigator.dismiss()
                    }
                }
            }
            ReviewScreen(
                viewModel = reviewViewModel,
                onDismiss = { reviewViewModel.abandonSession(); navigator.dismiss() },
            )
        }
    }

    // Review-reminder tap: start a due-cards review, unless one is already running (restarting it
    // would drop the user's place and orphan the session's analytics)
    OnEvents(koinInject<NotificationNavigator>().reviewRequests) {
        if (reviewViewModel.currentState.review !is ReviewState.Active) {
            openReviewScreen(ReviewSource.DueCards)
        }
    }

    /**
     * Opens the review directly, or — when [tags] narrow it further and the user hasn't opted
     * out — asks first whether to review everything or a single tag.
     */
    val reviewWithOptionalTagChoice: (ReviewChoice) -> Unit = { choice ->
        if (choice.tags.isEmpty() || skipTagSelector) {
            openReviewScreen(choice.allSource)
        } else {
            overlayHost.showSizeToFitBottomSheet(
                tag = "review-selector-${choice.allSource}",
                properties = DismissableSheetProperties,
            ) { nav ->
                val sheetProgressState by progressViewModel.state()
                ReviewSelectorSheetContent(
                    title = choice.title(),
                    allLabel = choice.allLabel(),
                    allCount = choice.allCount,
                    tags = choice.tags,
                    skipTagSelector = sheetProgressState.skipTagSelector,
                    onSkipTagSelectorChanged = progressViewModel::setSkipTagSelector,
                    onAllSelected = {
                        nav.dismiss()
                        openReviewScreen(choice.allSource)
                    },
                    onTagSelected = { tag ->
                        nav.dismiss()
                        openReviewScreen(choice.tagSource(tag))
                    },
                    onClose = { nav.dismiss() },
                )
            }
        }
    }

    val openListening: () -> Unit = {
        listeningViewModel.open()
        overlayHost.showFullScreen(
            tag = "listening",
            properties = FullScreenProperties(
                dismissOnBackPress = false,
                isNavigationBarsPaddingEnabled = true,
            ),
        ) { navigator ->
            ListeningScreen(
                viewModel = listeningViewModel,
                onDismiss = {
                    listeningViewModel.abandon()
                    navigator.dismiss()
                },
            )
        }
    }

    val openWordRush: () -> Unit = {
        wordRushViewModel.startGame()
        overlayHost.showFullScreen(
            tag = "word-rush",
            properties = FullScreenProperties(
                dismissOnBackPress = false,
                isNavigationBarsPaddingEnabled = true,
            ),
        ) { navigator ->
            // No `by` — stateHolder is passed directly to WordRushGameScreen,
            // which uses derivedStateOf internally. This overlay composable
            // itself does not recompose on every 50 ms timer tick.
            val gameStateHolder = wordRushViewModel.state()
            OnEvents(wordRushViewModel.effects) { effect ->
                when (effect) {
                    WordRushEffect.GameComplete -> progressViewModel.refreshStats()
                }
            }
            WordRushGameScreen(
                stateHolder = gameStateHolder,
                onSelectAnswer = wordRushViewModel::selectAnswer,
                onUsePowerUp = wordRushViewModel::usePowerUp,
                onPlayAgain = wordRushViewModel::startGame,
                onDismiss = {
                    wordRushViewModel.dismiss()
                    navigator.dismiss()
                },
            )
        }
    }

    val openImportSheet: () -> Unit = {
        overlayHost.showSizeToFitBottomSheet(
            tag = "import",
            properties = LockedSheetProperties,
        ) { sheetNav ->
            AddWordsSheet(
                onClose = { sheetNav.dismiss() },
                onWordsAdded = progressViewModel::refreshStats,
                onStartReview = {
                    sheetNav.dismiss()
                    openReviewScreen(ReviewSource.DueCards)
                },
                onOpenSubscription = {
                    sheetNav.dismiss()
                    onOpenSubscription()
                },
            )
        }
    }

    val openFocusSwitcher: () -> Unit = {
        overlayHost.showSizeToFitBottomSheet(
            tag = "focus-switcher",
            properties = DismissableSheetProperties,
        ) { nav ->
            val sheetProgressState by progressViewModel.state()
            LanguageSwitcherSheetContent(
                focus = sheetProgressState.focus,
                languages = sheetProgressState.languages,
                onSelect = { focus ->
                    progressViewModel.selectFocus(focus)
                    nav.dismiss()
                },
            )
        }
    }

    LexiconColumn(
        title = null,
        scrollState = scrollState,
        collapsedContent = {
            CollapsedStatsBar(
                visible = isStatsSectionScrolledAway && loadedProgress != null,
                stats = loadedProgress?.progressStats ?: return@LexiconColumn,
                evaluation = loadedProgress.progressEvaluation,
            )
        },
        actionIcon1 = ActionIconConfig(
            icon = Icons.Default.Add,
            contentDescription = stringResource(Res.string.import_words),
            onClick = openImportSheet,
            size = Theme.dimensions.iconSize,
        ),
        actionIcon2 = ActionIconConfig(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(Res.string.settings),
            onClick = onNavigateToSettings,
            size = Theme.dimensions.iconSize,
        ),
        actionIcon3 = if (progressState.showFocusSwitcher) {
            ActionIconConfig(
                icon = FocusLanguageIcon,
                contentDescription = stringResource(
                    Res.string.focus_current,
                    when (val focus = progressState.focus) {
                        is LearningFocus.Single -> focus.language.nativeName
                        LearningFocus.All -> stringResource(Res.string.focus_all_languages)
                    },
                ),
                onClick = openFocusSwitcher,
                tint = if (progressState.focus is LearningFocus.Single) MaterialTheme.colorScheme.primary else null,
                size = Theme.dimensions.iconSize,
            )
        } else {
            null
        },
        scrollable = true,
    ) {
        Column(Modifier.padding(bottom = Theme.spacing.sectionGap)) {
            when (uiState) {
                is UiState.Loading -> LoadingScreen(message = stringResource(Res.string.study_loading))

                is UiState.Error -> {
                    val isNetworkError = NetworkErrorHints.any { uiState.message.contains(it, ignoreCase = true) }
                    ErrorScreen(
                        message = when {
                            isNetworkError -> stringResource(Res.string.offline_changes_will_sync)
                            uiState.message.isNotEmpty() -> uiState.message
                            else -> stringResource(Res.string.study_load_failed)
                        },
                        title = stringResource(
                            if (isNetworkError) Res.string.no_connection else Res.string.study_error_title
                        ),
                        icon = if (isNetworkError) Icons.Default.WifiOff else null,
                        retryLabel = stringResource(Res.string.retry),
                        onRetry = progressViewModel::refreshStats,
                    )
                }

                is UiState.Loaded -> {
                    val stats = uiState.value.progressStats
                    val evaluation = uiState.value.progressEvaluation
                    val hasWords = evaluation.tier != ProgressTier.EMPTY

                    if (progressState.showFocusSwitcher && progressState.showIntro) {
                        FocusIntroCard(
                            languageCount = progressState.languages.size,
                            onGotIt = progressViewModel::acknowledgeIntro,
                            modifier = Modifier.padding(bottom = Theme.spacing.sm),
                        )
                    }

                    StatsSection(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            statsSectionBottom = (coordinates.positionInParent().y + coordinates.size.height).toInt()
                        },
                        evaluation = evaluation,
                        dueCards = stats.dueCards,
                        onImportWords = openImportSheet,
                        onStartReviewLongPress = { openReviewScreen(ReviewSource.DueCards) },
                        onStartReview = {
                            reviewWithOptionalTagChoice(
                                ReviewChoice(
                                    title = { stringResource(Res.string.start_review) },
                                    allLabel = { stringResource(Res.string.review_all_due) },
                                    allCount = stats.dueCards,
                                    tags = progressState.dueTags,
                                    allSource = ReviewSource.DueCards,
                                    tagSource = { tag -> ReviewSource.ByTag(tag.id) },
                                )
                            )
                        },
                    )

                    progressState.nudge?.let { nudge ->
                        FocusNudgeCard(
                            summary = nudge,
                            onSwitch = { progressViewModel.selectFocus(LearningFocus.Single(nudge.language)) },
                            onDismiss = progressViewModel::dismissNudge,
                            modifier = Modifier.padding(top = Theme.spacing.sm),
                        )
                    }

                    // A brand-new library has nothing to practise or sort into stages; the hero's
                    // import button is the only useful action, so keep the screen focused on it.
                    if (hasWords) {
                        PracticeSection(
                            wordRushViewModel = wordRushViewModel,
                            listeningViewModel = listeningViewModel.takeIf { isListeningSupported },
                            onPlayWordRush = openWordRush,
                            onListen = openListening,
                        )

                        LearningStagesSection(
                            stats = stats,
                            onStageLongClick = { stage, _ -> openReviewScreen(ReviewSource.ByStage(stage)) },
                            onStageClick = { stage, stageName ->
                                reviewWithOptionalTagChoice(
                                    ReviewChoice(
                                        title = { stageName },
                                        allLabel = { stringResource(Res.string.review_all_in_stage, stageName) },
                                        allCount = stats.countFor(stage),
                                        tags = progressState.stageTagsMap[stage.ordinal].orEmpty(),
                                        allSource = ReviewSource.ByStage(stage),
                                        tagSource = { tag -> ReviewSource.ByStageAndTag(stage, tag.id) },
                                    )
                                )
                            },
                        )

                        TagsSection(
                            tags = progressState.tags,
                            onTagClick = { tag -> openReviewScreen(ReviewSource.ByTag(tag.id)) },
                        )
                    }
                }
            }
        }
    }
}

/** What a tap on "Start Review" or a stage card can review: everything, or one of [tags]. */
private class ReviewChoice(
    val title: @Composable () -> String,
    val allLabel: @Composable () -> String,
    val allCount: Int,
    val tags: List<Tag>,
    val allSource: ReviewSource,
    val tagSource: (Tag) -> ReviewSource,
)

/** Word Rush and Listening side by side, equal height. Listening is null where TTS is unavailable. */
@Composable
private fun PracticeSection(
    wordRushViewModel: WordRushViewModel,
    listeningViewModel: ListeningViewModel?,
    onPlayWordRush: () -> Unit,
    onListen: () -> Unit,
) {
    val wordRushStateHolder = wordRushViewModel.state()
    val wordRushBestStreak by remember { derivedStateOf { wordRushStateHolder.value.bestStreak } }
    val wordRushHasEnoughWords by remember { derivedStateOf { wordRushStateHolder.value.hasEnoughWords } }

    Column(
        modifier = Modifier.padding(top = Theme.spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sectionHeaderGap),
    ) {
        SectionHeader(title = stringResource(Res.string.study_practice))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.listGap),
        ) {
            WordRushCard(
                bestStreak = wordRushBestStreak,
                hasEnoughWords = wordRushHasEnoughWords,
                onPlay = onPlayWordRush,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            if (listeningViewModel != null) {
                val listeningStateHolder = listeningViewModel.state()
                val listeningHasWords by remember { derivedStateOf { listeningStateHolder.value.hasWords } }
                ListeningCard(
                    hasWords = listeningHasWords,
                    onListen = onListen,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ReviewSelectorSheetContent(
    title: String,
    allLabel: String,
    allCount: Int,
    tags: List<Tag>,
    skipTagSelector: Boolean,
    onSkipTagSelectorChanged: (Boolean) -> Unit,
    onAllSelected: () -> Unit,
    onTagSelected: (Tag) -> Unit,
    onClose: () -> Unit,
) {
    SheetPage(title = title, onClose = onClose) {
        SheetGroup {
            SheetOptionRow(
                icon = Icons.Rounded.MenuBook,
                title = allLabel,
                onClick = onAllSelected,
                showDivider = tags.isNotEmpty(),
                trailingContent = { CountBadge(allCount) },
            )
            tags.forEachIndexed { index, tag ->
                SheetOptionRow(
                    icon = Icons.Rounded.Sell,
                    title = tag.name,
                    subtitle = stringResource(Res.string.filter_tag),
                    onClick = { onTagSelected(tag) },
                    showDivider = index < tags.lastIndex,
                    trailingContent = { CountBadge(tag.wordCount.toInt()) },
                )
            }
        }
        SheetGroup {
            SheetSwitchRow(
                title = stringResource(Res.string.skip_tag_selector_label),
                checked = skipTagSelector,
                onCheckedChange = onSkipTagSelectorChanged,
                showDivider = false,
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    SheetBadge(
        text = stringResource(Res.string.word_count_label, count),
        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
        contentColor = MaterialTheme.colorScheme.primary,
    )
}
