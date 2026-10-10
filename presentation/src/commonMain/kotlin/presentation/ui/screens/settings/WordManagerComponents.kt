package presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import domain.word.model.LearningStage
import domain.word.model.Word
import kotlinx.coroutines.isActive
import feature.words.model.WordManagerScreenState
import domain.word.model.WordSortOption
import theme.Theme
import utils.Language

@Composable
internal fun WordListContent(
    state: WordManagerScreenState,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleSelection: (Int) -> Unit,
    onOpenDetail: (Word) -> Unit,
    onDragSelectStart: (index: Int) -> Unit,
    onDragSelectTo: (index: Int) -> Unit,
    onDragSelectEnd: () -> Unit,
    onShareWords: () -> Unit,
    onSortOptionChange: (WordSortOption) -> Unit,
    onFilterLanguageChange: (Language?) -> Unit,
    onFilterLearningStageChange: (LearningStage?) -> Unit,
    onFilterTagChange: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBatchEditLanguages: () -> Unit,
    onBatchAssignTags: () -> Unit,
) {
    DragSelectScrollViewSetup()
    val lazyListState = rememberLazyListState()
    val dragSelectState = remember { DragSelectState() }
    val haptics = LocalHapticFeedback.current
    // pointerInput keeps one coroutine alive across recompositions; read the latest callbacks.
    val onDragStart by rememberUpdatedState<(Int) -> Unit>({ index ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        onDragSelectStart(index)
    })
    val onDragTo by rememberUpdatedState<(Int) -> Unit>({ index ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onDragSelectTo(index)
    })
    val onDragEnd by rememberUpdatedState(onDragSelectEnd)

    // Auto-scroll near the list edges, one step per frame. Rows that scroll under a finger
    // holding still are picked up here, since no pointer event arrives for them.
    val autoScrolling = dragSelectState.autoScrollSpeed != 0f
    LaunchedEffect(autoScrolling) {
        while (isActive && dragSelectState.autoScrollSpeed != 0f) {
            withFrameNanos { }
            lazyListState.scrollBy(dragSelectState.autoScrollSpeed)
            val index = lazyListState.rowAt(dragSelectState.fingerY)
            if (dragSelectState.moveTo(index)) onDragTo(index)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            var filtersExpanded by rememberSaveable { mutableStateOf(false) }
            val filtersActive = state.sortOption != WordSortOption.DATE_ADDED_DESC ||
                state.filterLanguage != null ||
                state.filterLearningStage != null

            Column(
                modifier = Modifier.padding(top = Theme.spacing.xs, bottom = Theme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
            ) {
                SearchRow(
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onClearSearch = onClearSearch,
                    filtersActive = filtersActive,
                    filtersExpanded = filtersExpanded,
                    onToggleFilters = { filtersExpanded = !filtersExpanded }
                )

                AnimatedVisibility(visible = filtersExpanded) {
                    FilterChipsRow(
                        sortOption = state.sortOption,
                        filterLanguage = state.filterLanguage,
                        filterLearningStage = state.filterLearningStage,
                        availableLanguages = state.availableLanguages,
                        onSortOptionChange = onSortOptionChange,
                        onFilterLanguageChange = onFilterLanguageChange,
                        onFilterLearningStageChange = onFilterLearningStageChange,
                    )
                }

                TagChipsRow(
                    totalCount = state.words.size,
                    tags = state.tags,
                    selectedTagId = state.filterTagId,
                    onTagSelected = onFilterTagChange
                )
            }

            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                state = lazyListState,
                userScrollEnabled = !dragSelectState.isDragging,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .dragSelectGesture(
                        lazyListState = lazyListState,
                        dragSelectState = dragSelectState,
                        onDragStart = { onDragStart(it) },
                        onDragTo = { onDragTo(it) },
                        onDragEnd = { onDragEnd() },
                    ),
                contentPadding = PaddingValues(
                    bottom = if (state.isSelectionMode) {
                        Theme.dimensions.bottomBarHeight + Theme.spacing.lg + navBarBottom
                    } else {
                        Theme.spacing.md + navBarBottom
                    }
                ),
            ) {
                val lastIndex = state.filteredWords.lastIndex
                itemsIndexed(state.filteredWords, key = { _, word -> word.id }) { index, word ->
                    WordCard(
                        word = word,
                        isFirst = index == 0,
                        isLast = index == lastIndex,
                        isSelected = state.selectedWordIds.contains(word.id),
                        isSelectionMode = state.isSelectionMode,
                        onTap = {
                            if (state.isSelectionMode) {
                                onToggleSelection(word.id)
                            } else {
                                onOpenDetail(word)
                            }
                        },
                        // Touch long press is the list's drag gesture; this is the accessibility action.
                        onLongPress = {
                            onDragSelectStart(index)
                            onDragSelectEnd()
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                if (state.isFiltered && state.filteredWords.isEmpty()) {
                    item(key = "empty-results") {
                        EmptySearchView(
                            hasSearchQuery = state.searchQuery.isNotBlank(),
                            onClearFilters = onClearFilters,
                        )
                    }
                }
            }
        }

        SelectionActionBar(
            isVisible = state.isSelectionMode,
            enabled = state.selectedCount > 0,
            onDelete = onDeleteSelected,
            onBatchEditLanguages = onBatchEditLanguages,
            onBatchAssignTags = onBatchAssignTags,
            onShare = onShareWords,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
