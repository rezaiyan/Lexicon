package presentation.ui.screens.settings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import domain.word.model.LearningStage
import domain.word.model.Word
import kotlinx.coroutines.delay
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
    onEnterSelectionMode: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onShareWords: () -> Unit,
    onSortOptionChange: (WordSortOption) -> Unit,
    onFilterLanguageChange: (Language?) -> Unit,
    onFilterLearningStageChange: (LearningStage?) -> Unit,
    onFilterTagChange: (Long?) -> Unit,
    onDeleteSelected: () -> Unit,
    onBatchEditLanguages: () -> Unit,
    onBatchAssignTags: () -> Unit,
    onExitSelectionMode: () -> Unit,
) {
    DragSelectScrollViewSetup()
    val lazyListState = rememberLazyListState()
    val dragSelectState = remember { DragSelectState() }
    // rememberUpdatedState ensures the gesture lambda reads the latest list
    // even though pointerInput keeps a single coroutine alive across recompositions.
    val currentFilteredWords = rememberUpdatedState(state.filteredWords)

    // Auto-scroll: runs whenever the gesture sets a non-zero speed.
    // Pattern from jordond/drag-select-compose: LaunchedEffect watches the speed
    // state so it restarts automatically when scrolling starts/stops.
    LaunchedEffect(dragSelectState.autoScrollSpeed) {
        if (dragSelectState.autoScrollSpeed == 0f) return@LaunchedEffect
        while (isActive) {
            lazyListState.scrollBy(dragSelectState.autoScrollSpeed)
            delay(16L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch
            )

            FilterChipsRow(
                sortOption = state.sortOption,
                filterLanguage = state.filterLanguage,
                filterLearningStage = state.filterLearningStage,
                filterTagId = state.filterTagId,
                tags = state.tags,
                availableLanguages = state.availableLanguages,
                onSortOptionChange = onSortOptionChange,
                onFilterLanguageChange = onFilterLanguageChange,
                onFilterLearningStageChange = onFilterLearningStageChange,
                onFilterTagChange = onFilterTagChange
            )

            WordCountSummary(
                filteredCount = state.filteredWords.size,
                totalCount = state.words.size,
                isFiltered = state.isFiltered
            )

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
                        onDragStarted = { index ->
                            currentFilteredWords.value.getOrNull(index)?.let { word ->
                                onEnterSelectionMode(word.id)
                            }
                        },
                        onItemEntered = { index ->
                            currentFilteredWords.value.getOrNull(index)?.let { word ->
                                onToggleSelection(word.id)
                            }
                        },
                    ),
                contentPadding = PaddingValues(
                    bottom = if (state.isSelectionMode) {
                        Theme.dimensions.bottomBarHeight + navBarBottom
                    } else {
                        Theme.spacing.small + navBarBottom
                    }
                ),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
            ) {
                items(state.filteredWords, key = { it.id }) { word ->
                    WordCard(
                        word = word,
                        isSelected = state.selectedWordIds.contains(word.id),
                        isSelectionMode = state.isSelectionMode,
                        onTap = {
                            if (state.isSelectionMode) {
                                onToggleSelection(word.id)
                            } else {
                                onOpenDetail(word)
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }

                if (state.searchQuery.isNotBlank() && state.filteredWords.isEmpty()) {
                    item {
                        EmptySearchView()
                    }
                }
            }
        }

        SelectionActionBar(
            isVisible = state.isSelectionMode && state.selectedCount > 0,
            selectedCount = state.selectedCount,
            isUserSubscribed = state.isUserSubscribed,
            onClose = onExitSelectionMode,
            onSelectAll = onSelectAll,
            onDelete = onDeleteSelected,
            onBatchEditLanguages = onBatchEditLanguages,
            onBatchAssignTags = onBatchAssignTags,
            onShare = onShareWords,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
