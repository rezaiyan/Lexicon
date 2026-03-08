package presentation.ui.screens.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import domain.word.model.LearningStage
import domain.word.model.Word
import feature.words.model.WordManagerScreenState
import feature.words.model.WordSortOption
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
    onDeleteSelected: () -> Unit,
    onBatchEditLanguages: () -> Unit,
    onExitSelectionMode: () -> Unit,
) {
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
                availableLanguages = state.availableLanguages,
                onSortOptionChange = onSortOptionChange,
                onFilterLanguageChange = onFilterLanguageChange,
                onFilterLearningStageChange = onFilterLearningStageChange
            )

            WordCountSummary(
                filteredCount = state.filteredWords.size,
                totalCount = state.words.size,
                isFiltered = state.isFiltered
            )

            val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
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
                        onLongPress = {
                            if (!state.isSelectionMode) {
                                onEnterSelectionMode(word.id)
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
            onShare = onShareWords,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
