package presentation.ui.screens

import androidx.compose.foundation.layout.Column
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
import core.getPlatformName
import components.LoadingScreen
import components.scaffold.ActionIconConfig
import components.scaffold.LexiconColumn
import components.sheet.SheetBadge
import components.sheet.SheetGroup
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetSwitchRow
import core.common.UiState
import domain.tag.model.Tag
import domain.word.model.LearningStage
import domain.word.model.ReviewSource
import events.OnEvents
import feature.study.ReviewEffect
import feature.study.ReviewState
import feature.study.ReviewViewModel
import feature.study.StudyProgressViewModel
import domain.focus.model.LearningFocus
import feature.study.ui.focus.FocusNudgeCard
import feature.study.ui.focus.LanguageSwitcherSheetContent
import feature.study.ui.focus.FocusIntroCard
import feature.study.ui.focus.FocusLanguageIcon
import feature.study.listening.ListeningViewModel
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
import lexicon.resources.generated.resources.focus_all_languages
import lexicon.resources.generated.resources.focus_current
import lexicon.resources.generated.resources.filter_tag
import lexicon.resources.generated.resources.import_words
import lexicon.resources.generated.resources.settings
import lexicon.resources.generated.resources.skip_tag_selector_label
import lexicon.resources.generated.resources.start_review
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
    val dueTags = progressState.dueTags
    val skipTagSelector = progressState.skipTagSelector
    val stageTagsMap = progressState.stageTagsMap

    val isListeningSupported = remember { getPlatformName() != "Web" }

    val scrollState = rememberScrollState()
    var statsSectionBottom by remember { mutableIntStateOf(0) }
    val isStatsSectionScrolledAway = remember(scrollState.value, statsSectionBottom) {
        scrollState.value > statsSectionBottom && statsSectionBottom > 0
    }

    val progressStats = (uiState as? UiState.Loaded)?.value?.progressStats

    // Single entry point for all review flows — eliminates 5+ repetitive call sites.
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

    val openListening: () -> Unit = {
        listeningViewModel.start(ReviewSource.DueCards)
        overlayHost.showFullScreen(
            tag = "listening",
            properties = FullScreenProperties(
                dismissOnBackPress = false,
                isNavigationBarsPaddingEnabled = true,
            ),
        ) { navigator ->
            ListeningScreen(
                viewModel = listeningViewModel,
                onRestart = { listeningViewModel.start(ReviewSource.DueCards) },
                onDismiss = {
                    listeningViewModel.abandon()
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
            properties = BottomSheetProperties(
                dismissOnBackPress = true,
                dismissOnTouchOutside = true,
            ),
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
                visible = isStatsSectionScrolledAway && progressStats != null,
                stats = progressStats ?: return@LexiconColumn,
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
                is UiState.Loading -> {
                    LoadingScreen(message = "Preparing your study session...")
                }

                is UiState.Error -> {
                    val errorMessage = uiState.message
                    val isNetworkError = errorMessage.contains("timeout", ignoreCase = true) ||
                        errorMessage.contains("connect", ignoreCase = true) ||
                        errorMessage.contains("network", ignoreCase = true) ||
                        errorMessage.contains("internet", ignoreCase = true)

                    ErrorScreen(
                        message = if (isNetworkError) {
                            "You're offline -- changes will sync when reconnected."
                        } else {
                            errorMessage.ifEmpty { "Something went wrong loading your progress." }
                        },
                        title = if (isNetworkError) "No Connection" else "Oops!",
                        icon = if (isNetworkError) Icons.Default.WifiOff else null,
                        retryLabel = "Try Again",
                        onRetry = { progressViewModel.refreshStats() },
                    )
                }

                is UiState.Loaded -> {
                    val loadedState = uiState.value
                    val loadedStats = loadedState.progressStats
                    val evaluation = loadedState.progressEvaluation

                    if (progressState.showFocusSwitcher && progressState.showIntro) {
                        FocusIntroCard(
                            languageCount = progressState.languages.size,
                            onGotIt = progressViewModel::acknowledgeIntro,
                            modifier = Modifier.padding(bottom = Theme.spacing.sm),
                        )
                    }

                    StatsSection(
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            statsSectionBottom =
                                (coordinates.positionInParent().y + coordinates.size.height).toInt()
                        },
                        evaluation = evaluation,
                        dueCards = loadedStats.dueCards,
                        onImportWords = openImportSheet,
                        onStartReviewLongPress = { openReviewScreen(ReviewSource.DueCards) },
                        onStartReview = {
                            if (dueTags.isNotEmpty() && !skipTagSelector) {
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "review-selector",
                                    properties = BottomSheetProperties(
                                        dismissOnBackPress = true,
                                        dismissOnTouchOutside = true,
                                    ),
                                ) { nav ->
                                    val sheetProgressState by progressViewModel.state()
                                    ReviewSelectorSheetContent(
                                        title = stringResource(Res.string.start_review),
                                        allLabel = "All due words",
                                        allCount = loadedStats.dueCards,
                                        tags = dueTags,
                                        skipTagSelector = sheetProgressState.skipTagSelector,
                                        onSkipTagSelectorChanged = { progressViewModel.setSkipTagSelector(it) },
                                        onAllSelected = {
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.DueCards)
                                        },
                                        onTagSelected = { tag ->
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.ByTag(tag.id))
                                        },
                                        onClose = { nav.dismiss() },
                                    )
                                }
                            } else {
                                openReviewScreen(ReviewSource.DueCards)
                            }
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

                    val wordRushStateHolder = wordRushViewModel.state()
                    val wordRushBestStreak by remember { derivedStateOf { wordRushStateHolder.value.bestStreak } }
                    val wordRushHasEnoughWords by remember {
                        derivedStateOf { wordRushStateHolder.value.hasEnoughWords }
                    }
                    WordRushCard(
                        bestStreak = wordRushBestStreak,
                        hasEnoughWords = wordRushHasEnoughWords,
                        onPlay = {
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
                                        WordRushEffect.GameComplete -> {
                                            progressViewModel.refreshStats()
                                        }
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
                        },
                        modifier = Modifier.padding(top = Theme.spacing.md),
                    )

                    // WasmJs has no on-device TTS engine, so listening mode is mobile-only.
                    if (isListeningSupported) {
                        val listeningStateHolder = listeningViewModel.state()
                        val listeningHasWords by remember {
                            derivedStateOf { listeningStateHolder.value.hasWords }
                        }
                        ListeningCard(
                            hasWords = listeningHasWords,
                            onListen = openListening,
                            modifier = Modifier.padding(top = Theme.spacing.md),
                        )
                    }

                    LearningStagesSection(
                        stats = loadedStats,
                        onStageLongClick = { stage, _ ->
                            openReviewScreen(ReviewSource.ByStage(stage))
                        },
                        onStageClick = { stage, stageName ->
                            val stageTags = stageTagsMap[stage.ordinal].orEmpty()
                            if (stageTags.isNotEmpty() && !skipTagSelector) {
                                val stageCount = when (stage) {
                                    LearningStage.LEVEL_0_FRESH -> loadedStats.level0Count
                                    LearningStage.LEVEL_1_LEARNING -> loadedStats.level1Count
                                    LearningStage.LEVEL_2_FAMILIAR -> loadedStats.level2Count
                                    LearningStage.LEVEL_3_BUILDING -> loadedStats.level3Count
                                    LearningStage.LEVEL_4_ALMOST -> loadedStats.level4Count
                                    LearningStage.LEVEL_5_STRONG -> loadedStats.level5Count
                                    LearningStage.LEVEL_6_MASTERED -> loadedStats.level6Count
                                }
                                overlayHost.showSizeToFitBottomSheet(
                                    tag = "stage-selector-${stage}",
                                    properties = BottomSheetProperties(
                                        dismissOnBackPress = true,
                                        dismissOnTouchOutside = true,
                                    ),
                                ) { nav ->
                                    val sheetProgressState by progressViewModel.state()
                                    ReviewSelectorSheetContent(
                                        title = stageName,
                                        allLabel = "All $stageName",
                                        allCount = stageCount,
                                        tags = stageTags,
                                        skipTagSelector = sheetProgressState.skipTagSelector,
                                        onSkipTagSelectorChanged = { progressViewModel.setSkipTagSelector(it) },
                                        onAllSelected = {
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.ByStage(stage))
                                        },
                                        onTagSelected = { tag ->
                                            nav.dismiss()
                                            openReviewScreen(ReviewSource.ByStageAndTag(stage, tag.id))
                                        },
                                        onClose = { nav.dismiss() },
                                    )
                                }
                            } else {
                                openReviewScreen(ReviewSource.ByStage(stage))
                            }
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
