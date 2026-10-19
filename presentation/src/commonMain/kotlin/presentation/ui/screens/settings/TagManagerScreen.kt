package presentation.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.EmptyScreen
import components.GroupedRow
import components.GroupedSection
import components.LabeledSwitch
import components.scaffold.LexiconColumn
import components.scaffold.TopBarColor
import components.sheet.ConfirmSheetContent
import components.sheet.ConfirmTone
import components.sheet.SheetGroup
import components.sheet.SheetOptionRow
import components.sheet.SheetPage
import components.sheet.SheetSearchField
import components.snackbar.showError
import domain.tag.model.Tag
import events.OnEvents
import feature.words.TagManagerViewModel
import feature.words.model.TagManagerEffect
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.ask_tag_before_review
import lexicon.resources.generated.resources.ask_tag_before_review_subtitle
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.content_description_close
import lexicon.resources.generated.resources.create_tag
import lexicon.resources.generated.resources.delete_tag
import lexicon.resources.generated.resources.delete_tag_confirm
import lexicon.resources.generated.resources.new_tag
import lexicon.resources.generated.resources.no_tags
import lexicon.resources.generated.resources.no_tags_match
import lexicon.resources.generated.resources.no_tags_subtitle
import lexicon.resources.generated.resources.rename_tag
import lexicon.resources.generated.resources.search_tags
import lexicon.resources.generated.resources.tag_manager
import lexicon.resources.generated.resources.tag_manager_review_section
import lexicon.resources.generated.resources.tag_manager_tags_section
import lexicon.resources.generated.resources.word_count_label
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import overlay.LocalOverlayHost
import overlay.OverlayHost
import overlay.bottomsheet.BottomSheetPages
import overlay.bottomsheet.rememberBottomSheetPageNavigator
import overlay.bottomsheet.showSizeToFitBottomSheet
import overlay.fullscreen.FullScreenContainer
import overlay.fullscreen.FullScreenProperties
import overlay.fullscreen.showFullScreen
import presentation.ui.LocalSnackbarHostState
import presentation.ui.components.TagFormContent
import theme.AppColors
import theme.Theme

/** Room under the last row so the floating "New Tag" button never covers it. */
private val FabClearance = 88.dp

fun OverlayHost.showTagManagerScreen() {
    showFullScreen(
        tag = "tag-manager",
        properties = FullScreenProperties(
            dismissOnBackPress = false,
            container = FullScreenContainer.Background,
        ),
    ) { nav ->
        TagManagerContent(onDismiss = { nav.dismiss() })
    }
}

@Composable
private fun TagManagerContent(onDismiss: () -> Unit) {
    val viewModel = koinViewModel<TagManagerViewModel>()
    val state by viewModel.state()
    val snackbarHostState = LocalSnackbarHostState.current
    val overlayHost = LocalOverlayHost.current

    val listState = rememberLazyListState()
    // Rows that sit above the tags: review section, tags header, and the search field when shown
    val leadingItemCount = if (state.showSearch) 3 else 2
    // Tags are sorted by name and the list keeps its first visible row in place, so a tag
    // created or renamed above it would land out of view. Scroll to it once it's listed.
    var revealTagId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(revealTagId, state.visibleTags) {
        val index = state.visibleTags.indexOfFirst { it.id == revealTagId }
        if (index >= 0) {
            listState.animateScrollToItem(index + leadingItemCount)
            revealTagId = null
        }
    }

    OnEvents(viewModel.effects) { effect ->
        when (effect) {
            is TagManagerEffect.TagCreated -> revealTagId = effect.tag.id
            is TagManagerEffect.TagRenamed -> revealTagId = effect.tag.id
            is TagManagerEffect.TagDeleted -> Unit
            is TagManagerEffect.Error -> snackbarHostState.showError(effect.message)
        }
    }

    val showCreateTag = {
        overlayHost.showSizeToFitBottomSheet(tag = "create-tag") { nav ->
            TagFormContent(
                title = stringResource(Res.string.new_tag),
                confirmText = stringResource(Res.string.create_tag),
                onConfirm = { name ->
                    viewModel.createTag(name)
                    nav.dismiss()
                },
                onDismiss = { nav.dismiss() },
                onClose = { nav.dismiss() },
            )
        }
    }

    LexiconColumn(
        title = stringResource(Res.string.tag_manager),
        showNavigationIcon = true,
        navigationIcon = Icons.Default.Close,
        navigationIconContentDescription = stringResource(Res.string.content_description_close),
        onNavigationClick = onDismiss,
        scrollable = false,
        topBarColor = TopBarColor.Background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingView()

                state.errorMessage != null -> ErrorView(message = state.errorMessage ?: "")

                state.tags.isEmpty() -> EmptyScreen(
                    modifier = Modifier.navigationBarsPadding(),
                    title = stringResource(Res.string.no_tags),
                    subtitle = stringResource(Res.string.no_tags_subtitle),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Label,
                            contentDescription = null,
                            modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
                            tint = AppColors.settingsTagManagerIcon
                        )
                    },
                    actionLabel = stringResource(Res.string.new_tag),
                    onAction = showCreateTag,
                )

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                        contentPadding = PaddingValues(top = Theme.spacing.xs, bottom = FabClearance),
                    ) {
                        item(key = "review") {
                            GroupedSection(
                                title = stringResource(Res.string.tag_manager_review_section),
                                modifier = Modifier.padding(bottom = Theme.spacing.lg),
                            ) {
                                LabeledSwitch(
                                    title = stringResource(Res.string.ask_tag_before_review),
                                    subtitle = stringResource(Res.string.ask_tag_before_review_subtitle),
                                    checked = !state.skipTagSelector,
                                    onCheckedChange = { viewModel.setSkipTagSelector(!it) },
                                    modifier = Modifier.padding(
                                        horizontal = Theme.spacing.cardPadding,
                                        vertical = Theme.spacing.xs,
                                    ),
                                )
                            }
                        }
                        item(key = "tags-header") {
                            TagsSectionHeader(count = state.tags.size)
                        }
                        if (state.showSearch) {
                            item(key = "search") {
                                SheetSearchField(
                                    value = state.query,
                                    onValueChange = viewModel::search,
                                    placeholder = stringResource(Res.string.search_tags),
                                    modifier = Modifier.padding(bottom = Theme.spacing.sm),
                                )
                            }
                        }
                        if (state.visibleTags.isEmpty()) {
                            item(key = "no-match") {
                                Text(
                                    text = stringResource(Res.string.no_tags_match, state.query.trim()),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = Theme.spacing.lg),
                                )
                            }
                        }
                        val lastIndex = state.visibleTags.lastIndex
                        itemsIndexed(state.visibleTags, key = { _, tag -> tag.id }) { index, tag ->
                            TagRow(
                                tag = tag,
                                isFirst = index == 0,
                                isLast = index == lastIndex,
                                onClick = { overlayHost.showTagActionsSheet(tag, viewModel) },
                                // No staggered entrance: rows re-enter on every scroll, and its
                                // alpha-0 start hid them from accessibility services.
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    val newTagLabel = stringResource(Res.string.new_tag)
                    ExtendedFloatingActionButton(
                        onClick = showCreateTag,
                        icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(newTagLabel) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            // The extended FAB doesn't expose its label to accessibility, so
                            // screen readers announced an unlabelled button.
                            .semantics { contentDescription = newTagLabel }
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(Theme.spacing.md)
                    )
                }
            }
        }
    }
}

/** Matches [GroupedSection]'s header so the tag list reads as the same grouped card below it. */
@Composable
private fun TagsSectionHeader(count: Int) {
    Text(
        text = "${stringResource(Res.string.tag_manager_tags_section).uppercase()} · $count",
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = Theme.spacing.xxs, bottom = Theme.spacing.xs)
            .semantics { heading() },
    )
}

/**
 * One tag as a slice of a grouped card. Each row is its own lazy item (so long lists stay lazy
 * and a new tag can be scrolled to), shaped by its position to read as one rounded card.
 */
@Composable
private fun TagRow(
    tag: Tag,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val corner = Theme.shapes.large
    val shape = RoundedCornerShape(
        topStart = if (isFirst) corner else 0.dp,
        topEnd = if (isFirst) corner else 0.dp,
        bottomStart = if (isLast) corner else 0.dp,
        bottomEnd = if (isLast) corner else 0.dp,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        GroupedRow(
            title = tag.name,
            value = stringResource(Res.string.word_count_label, tag.wordCount.toInt()),
            icon = Icons.Rounded.Label,
            iconColor = AppColors.settingsTagManagerIcon,
            onClick = onClick,
            showDivider = !isLast,
        )
    }
}

private sealed interface TagSheetPage {
    data object Actions : TagSheetPage
    data object Rename : TagSheetPage
    data object Delete : TagSheetPage
}

/** One sheet per tag: pick an action, then rename or confirm the delete on the next page. */
private fun OverlayHost.showTagActionsSheet(tag: Tag, viewModel: TagManagerViewModel) {
    showSizeToFitBottomSheet(tag = "tag-actions") { nav ->
        val pages = rememberBottomSheetPageNavigator<TagSheetPage>(TagSheetPage.Actions)
        BottomSheetPages(navigator = pages, onClose = { nav.dismiss() }, label = "tagActionsPages") { page ->
            when (page) {
                TagSheetPage.Actions -> SheetPage(
                    title = tag.name,
                    subtitle = stringResource(Res.string.word_count_label, tag.wordCount.toInt()),
                ) {
                    SheetGroup {
                        SheetOptionRow(
                            icon = Icons.Rounded.Edit,
                            title = stringResource(Res.string.rename_tag),
                            onClick = { pages.navigateTo(TagSheetPage.Rename) },
                            showDivider = true,
                        )
                        SheetOptionRow(
                            icon = Icons.Default.DeleteOutline,
                            title = stringResource(Res.string.delete_tag),
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { pages.navigateTo(TagSheetPage.Delete) },
                            showDivider = false,
                        )
                    }
                }

                TagSheetPage.Rename -> TagFormContent(
                    title = stringResource(Res.string.rename_tag),
                    confirmText = stringResource(Res.string.rename_tag),
                    onConfirm = { name ->
                        viewModel.renameTag(tag.id, name)
                        nav.dismiss()
                    },
                    onDismiss = { pages.navigateBack() },
                    initialName = tag.name,
                )

                TagSheetPage.Delete -> ConfirmSheetContent(
                    icon = Icons.Default.DeleteOutline,
                    title = stringResource(Res.string.delete_tag),
                    message = stringResource(Res.string.delete_tag_confirm),
                    confirmText = stringResource(Res.string.delete_tag),
                    onConfirm = {
                        viewModel.deleteTag(tag.id)
                        nav.dismiss()
                    },
                    dismissText = stringResource(Res.string.cancel),
                    onDismiss = { pages.navigateBack() },
                    tone = ConfirmTone.Danger,
                )
            }
        }
    }
}
