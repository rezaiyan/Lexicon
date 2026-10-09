package presentation.ui.components.imports

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import domain.credits.model.CreditAction
import domain.credits.model.CreditTier
import domain.word.add.model.WordOrigin
import events.OnEvents
import expects.BackHandler
import feature.addwords.AddWordsViewModel
import feature.addwords.model.AddWordsEffect
import feature.addwords.model.AddWordsUiState
import feature.addwords.model.SourceEffect
import feature.addwords.model.canStart
import feature.addwords.model.creditAction
import feature.addwords.source.AiSuggestViewModel
import feature.addwords.source.FileImportViewModel
import feature.addwords.source.ManualEntryEffect
import feature.addwords.source.ManualEntryViewModel
import feature.addwords.source.PhotoImportViewModel
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.add_words_native_title
import lexicon.resources.generated.resources.create_tag
import lexicon.resources.generated.resources.new_tag
import lexicon.resources.generated.resources.word_language_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import overlay.bottomsheet.BottomSheetPageConfig
import overlay.bottomsheet.BottomSheetPageNavigator
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import presentation.ui.components.TagFormContent
import utils.Language
import utils.rememberCameraLauncher
import utils.rememberImagePickerLauncher

private sealed interface AddWordsPage {
    data object Chooser : AddWordsPage
    data object Manual : AddWordsPage
    data object File : AddWordsPage
    data object Photo : AddWordsPage
    data object AiLevel : AddWordsPage
    data object AiTopics : AddWordsPage
    data object Review : AddWordsPage
    data object Result : AddWordsPage
    data object CreateTag : AddWordsPage

    /** [origin] (a paid source) costs more credits than the user has. */
    data class OutOfCredits(val origin: WordOrigin) : AddWordsPage

    /** Language setup; continues to [then] (or back to where it was opened) once both are picked. */
    data class Learning(val then: AddWordsPage?) : AddWordsPage
    data class Native(val then: AddWordsPage?) : AddWordsPage
}

/**
 * The one add-words sheet: choose a source (type, file, photo, AI), review what it found, add.
 * Its ViewModels live exactly as long as the sheet, so every opening starts fresh.
 *
 * @param onWordsAdded called after words were saved (refresh counts).
 * @param onStartReview offered on the result page when non-null.
 * @param onOpenSubscription offered when credits run out; the caller closes the sheet and opens the paywall.
 */
@Composable
fun AddWordsSheet(
    onClose: () -> Unit,
    onWordsAdded: () -> Unit,
    onStartReview: (() -> Unit)? = null,
    onOpenSubscription: (() -> Unit)? = null,
) = ScopedViewModelStore {
    AddWordsSheetContent(onClose, onWordsAdded, onStartReview, onOpenSubscription)
}

@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod") // page router: one branch per page
private fun AddWordsSheetContent(
    onClose: () -> Unit,
    onWordsAdded: () -> Unit,
    onStartReview: (() -> Unit)?,
    onOpenSubscription: (() -> Unit)?,
) {
    val host = koinViewModel<AddWordsViewModel>()
    val manual = koinViewModel<ManualEntryViewModel>()
    val file = koinViewModel<FileImportViewModel>()
    val photo = koinViewModel<PhotoImportViewModel>()
    val ai = koinViewModel<AiSuggestViewModel>()
    val state by host.state()
    val pages = rememberBottomSheetPageNavigator<AddWordsPage>(AddWordsPage.Chooser)
    var confirmDiscard by remember { mutableStateOf(false) }

    OnEvents(host.effects) { effect ->
        when (effect) {
            AddWordsEffect.OpenReview -> pages.navigateTo(AddWordsPage.Review)
            AddWordsEffect.ShowResult -> {
                if (pages.currentPage == AddWordsPage.Review) pages.navigateBack()
                photo.clearPhoto()
                onWordsAdded()
                pages.navigateTo(AddWordsPage.Result)
            }
        }
    }
    val onSourceEffect: (WordOrigin, SourceEffect) -> Unit = { origin, effect ->
        when (effect) {
            is SourceEffect.CandidatesReady -> host.openReview(origin, effect.drafts, effect.rejected)
            SourceEffect.OutOfCredits -> pages.navigateTo(AddWordsPage.OutOfCredits(origin))
        }
    }
    // Typed words are saved one by one, so refresh right away: the user may close without tapping Done.
    OnEvents(manual.effects) { effect -> if (effect == ManualEntryEffect.WordAdded) onWordsAdded() }
    OnEvents(file.effects) { onSourceEffect(WordOrigin.File, it) }
    OnEvents(photo.effects) { onSourceEffect(WordOrigin.Photo, it) }
    OnEvents(ai.effects) { onSourceEffect(WordOrigin.AiSuggestion, it) }

    // Every source needs the language pair; ask for it first when there is none yet.
    val open: (AddWordsPage) -> Unit = { page ->
        when {
            !state.languagesLoaded -> Unit
            state.languages == null -> pages.navigateTo(AddWordsPage.Learning(then = page))
            else -> pages.navigateTo(page)
        }
    }
    val openSource: (WordOrigin, AddWordsPage) -> Unit = { origin, page ->
        when {
            // Known to be short: say so now rather than after the user picked a photo or topics
            !state.credits.canStart(origin) -> pages.navigateTo(AddWordsPage.OutOfCredits(origin))
            else -> {
                if (state.languagesLoaded) host.sourceOpened(origin)
                open(page)
            }
        }
    }
    val changeLanguages = { pages.navigateTo(AddWordsPage.Learning(then = null)) }
    val openCreateTag = { pages.navigateTo(AddWordsPage.CreateTag) }
    val discardReview = {
        confirmDiscard = false
        host.discardReview()
        if (pages.currentPage == AddWordsPage.Review) pages.navigateBack()
    }

    if (confirmDiscard) {
        DiscardConfirmationDialog(onDiscard = discardReview, onKeep = { confirmDiscard = false })
    }

    BottomSheetPages(
        navigator = pages,
        onClose = { if (state.review != null) confirmDiscard = true else onClose() },
        label = "AddWordsPages",
        pageConfig = { page ->
            BottomSheetPageConfig(
                showBackButton = page != AddWordsPage.Review && page != AddWordsPage.Result,
            )
        },
    ) { page ->
        val learning = state.learning ?: Language.ENGLISH
        val native = state.native ?: Language.ENGLISH
        when (page) {
            AddWordsPage.Chooser -> {
                // The sheet ignores Back (LockedSheetProperties) so deeper pages can't lose work;
                // the chooser has nothing to lose, so Back closes the sheet here.
                BackHandler(onBack = onClose)
                val credits = state.credits
                AddWordsChooserContent(
                    aiCost = credits?.costOf(CreditAction.AI_SUGGESTION),
                    photoCost = credits?.costOf(CreditAction.PHOTO_EXTRACTION),
                    onAiAssistant = { openSource(WordOrigin.AiSuggestion, AddWordsPage.AiLevel) },
                    onTypeWord = { openSource(WordOrigin.Manual, AddWordsPage.Manual) },
                    onImportFile = { openSource(WordOrigin.File, AddWordsPage.File) },
                    onScanPhoto = { openSource(WordOrigin.Photo, AddWordsPage.Photo) },
                )
            }

            is AddWordsPage.Learning -> ImportLanguageListPage(
                title = stringResource(Res.string.word_language_title),
                languages = Language.entries,
                selected = state.learning,
                onLanguageSelected = { language ->
                    host.setLearningLanguage(language)
                    pages.navigateTo(AddWordsPage.Native(page.then))
                },
            )

            is AddWordsPage.Native -> ImportLanguageListPage(
                title = stringResource(Res.string.add_words_native_title),
                languages = Language.entries.filter { it != state.learning },
                selected = state.native,
                onLanguageSelected = { language ->
                    host.setNativeLanguage(language)
                    pages.finishLanguageSetup(page.then)
                },
            )

            AddWordsPage.Manual -> {
                val manualState by manual.state()
                TextImportContent(
                    state = manualState,
                    learning = learning,
                    native = native,
                    tags = state.tags,
                    selectedTagId = state.selectedTagId,
                    onTagSelected = host::selectTag,
                    onCreateTag = openCreateTag,
                    onChangeLanguage = changeLanguages,
                    onTermChange = manual::setTerm,
                    onTranslationChange = manual::setTranslation,
                    onNoteChange = manual::setNote,
                    onAddWord = { manual.add(state.languages, state.tagIds) },
                    onDone = {
                        if (manualState.addedCount > 0) {
                            host.finishManualEntry(manualState.addedCount, manualState.recent.map { it.term })
                            manual.clearSession()
                        } else {
                            pages.navigateBack()
                        }
                    },
                )
            }

            AddWordsPage.File -> {
                val fileState by file.state()
                FileImportContent(
                    isLoading = fileState.isParsing,
                    problem = fileState.problem.text(),
                    learning = learning,
                    native = native,
                    tags = state.tags,
                    selectedTagId = state.selectedTagId,
                    onTagSelected = host::selectTag,
                    onCreateTag = openCreateTag,
                    onChangeLanguage = changeLanguages,
                    onFilePicked = { picked -> file.onFilePicked(picked?.name, picked?.bytes) },
                )
            }

            AddWordsPage.Photo -> PhotoPage(
                state = state,
                photo = photo,
                onTagSelected = host::selectTag,
                onCreateTag = openCreateTag,
                onChangeLanguage = changeLanguages,
            )

            AddWordsPage.AiLevel -> {
                val aiState by ai.state()
                AiLevelStep(
                    selectedLevel = aiState.level,
                    error = null,
                    onLevelSelected = ai::selectLevel,
                    onContinue = { pages.navigateTo(AddWordsPage.AiTopics) },
                )
            }

            AddWordsPage.AiTopics -> {
                val aiState by ai.state()
                if (aiState.isGenerating) {
                    BackHandler { /* generation in progress: stay */ }
                    AiGeneratingContent(learning = state.learning, level = aiState.level, topics = aiState.topics)
                } else {
                    AiTopicsStep(
                        topics = aiState.availableTopics,
                        selectedTopics = aiState.topics,
                        error = aiState.problem.text(),
                        onToggleTopic = ai::toggleTopic,
                        onGenerate = { ai.generate(state.languages) },
                        generateEnabled = aiState.canGenerate,
                    )
                }
            }

            AddWordsPage.Review -> ReviewPage(
                state = state,
                host = host,
                learning = learning,
                native = native,
                onCreateTag = openCreateTag,
                onChangeLanguage = changeLanguages,
                onRequestDiscard = { confirmDiscard = true },
            )

            AddWordsPage.Result -> {
                val result = state.result ?: return@BottomSheetPages
                ImportSuccessContent(
                    count = result.added,
                    duplicates = result.duplicates,
                    previewWords = result.previewTerms,
                    onStartReview = onStartReview,
                    onAddMore = { while (pages.canNavigateBack) pages.navigateBack() },
                    onDone = onClose,
                )
            }

            is AddWordsPage.OutOfCredits -> {
                val credits = state.credits
                OutOfCreditsContent(
                    cost = page.origin.creditAction?.let { credits?.costOf(it) },
                    balance = credits?.balance,
                    refillsAtMillis = credits?.periodEndsAtMillis,
                    premiumMonthlyAllowance = credits?.premiumMonthlyAllowance,
                    // Premium members can't buy more; they wait for the refill
                    onUpgrade = onOpenSubscription?.takeIf { credits?.tier != CreditTier.PREMIUM },
                    onTypeInstead = {
                        while (pages.currentPage != AddWordsPage.Chooser && pages.navigateBack()) Unit
                        openSource(WordOrigin.Manual, AddWordsPage.Manual)
                    },
                )
            }

            AddWordsPage.CreateTag -> TagFormContent(
                title = stringResource(Res.string.new_tag),
                confirmText = stringResource(Res.string.create_tag),
                onConfirm = { name ->
                    host.createTag(name)
                    pages.navigateBack()
                },
                onDismiss = { pages.navigateBack() },
            )
        }
    }
}

/** Pops the two language pages, then continues to [then] when the setup was opened by a source. */
private fun BottomSheetPageNavigator<AddWordsPage>.finishLanguageSetup(then: AddWordsPage?) {
    while (currentPage is AddWordsPage.Native || currentPage is AddWordsPage.Learning) {
        if (!navigateBack()) break
    }
    if (then != null) navigateTo(then)
}

@Composable
private fun PhotoPage(
    state: AddWordsUiState,
    photo: PhotoImportViewModel,
    onTagSelected: (Long?) -> Unit,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
) {
    val photoState by photo.state()
    val galleryLauncher = rememberImagePickerLauncher(photo::onPhotoPicked)
    val cameraLauncher = rememberCameraLauncher(photo::onPhotoPicked)
    val picked = photoState.photo

    if (picked == null) {
        PhotoSourcePage(
            isEnabled = true,
            sourceLanguage = state.learning ?: Language.ENGLISH,
            targetLanguage = state.native ?: Language.ENGLISH,
            tags = state.tags,
            selectedTagId = state.selectedTagId,
            onTagSelected = onTagSelected,
            onCreateTag = onCreateTag,
            onChangeLanguage = onChangeLanguage,
            onCameraClick = cameraLauncher,
            onGalleryClick = galleryLauncher,
        )
    } else {
        PhotoPreviewPage(
            imageBytes = picked.bytes,
            isLoading = photoState.isExtracting,
            isEnabled = !photoState.isExtracting,
            problem = photoState.problem.text(),
            quarterTurns = photoState.quarterTurns,
            onRotate = photo::rotatePhoto,
            onConfirm = { photo.extract(state.languages) },
            onRetake = photo::clearPhoto,
        )
    }
}

@Composable
private fun ReviewPage(
    state: AddWordsUiState,
    host: AddWordsViewModel,
    learning: Language,
    native: Language,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
    onRequestDiscard: () -> Unit,
) {
    val review = state.review ?: return
    // Composed inside the page so it outranks the pager's back handler: leaving asks first.
    BackHandler(enabled = review.editing == null, onBack = onRequestDiscard)
    CandidateReviewPage(
        review = review,
        learning = learning,
        native = native,
        tags = state.tags,
        selectedTagId = state.selectedTagId,
        isCommitting = state.isCommitting,
        problem = state.problem.text(),
        onTagSelected = host::selectTag,
        onCreateTag = onCreateTag,
        onChangeLanguage = onChangeLanguage,
        onToggle = host::toggleCandidate,
        onSetAllSelected = host::setAllCandidatesSelected,
        onStartEdit = host::startEditingCandidate,
        onCancelEdit = host::cancelEditingCandidate,
        onSaveEdit = host::saveCandidateEdit,
        onCommit = host::commitReview,
    )
}
