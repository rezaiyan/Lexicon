@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package presentation.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import components.EmptyScreen
import components.ErrorScreen
import components.LoadingScreen
import components.Pill
import domain.word.model.LearningStage
import domain.word.model.Word
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.batch_edit_languages
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.delete
import lexicon.resources.generated.resources.empty_library
import lexicon.resources.generated.resources.error
import lexicon.resources.generated.resources.filter_all_languages
import lexicon.resources.generated.resources.filter_all_levels
import lexicon.resources.generated.resources.filter_language
import lexicon.resources.generated.resources.filter_level
import lexicon.resources.generated.resources.level_0_fresh
import lexicon.resources.generated.resources.level_1_learning
import lexicon.resources.generated.resources.level_2_familiar
import lexicon.resources.generated.resources.level_3_building
import lexicon.resources.generated.resources.level_4_almost
import lexicon.resources.generated.resources.level_5_strong
import lexicon.resources.generated.resources.level_6_mastered
import lexicon.resources.generated.resources.loading_words
import lexicon.resources.generated.resources.no_results_found
import lexicon.resources.generated.resources.search_words
import lexicon.resources.generated.resources.select_all
import lexicon.resources.generated.resources.selected_format
import lexicon.resources.generated.resources.share
import lexicon.resources.generated.resources.sort_a_to_z
import lexicon.resources.generated.resources.sort_level_asc
import lexicon.resources.generated.resources.sort_level_desc
import lexicon.resources.generated.resources.sort_level_high_low
import lexicon.resources.generated.resources.sort_level_low_high
import lexicon.resources.generated.resources.sort_newest
import lexicon.resources.generated.resources.sort_newest_first
import lexicon.resources.generated.resources.sort_oldest
import lexicon.resources.generated.resources.sort_oldest_first
import lexicon.resources.generated.resources.sort_z_to_a
import lexicon.resources.generated.resources.start_by_importing
import lexicon.resources.generated.resources.word_count_filtered_format
import lexicon.resources.generated.resources.word_count_format
import org.jetbrains.compose.resources.stringResource

import presentation.model.WordManagerScreenState
import presentation.model.WordSortOption
import theme.AppColors
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
    onExitSelectionMode: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            SearchBar(
                searchQuery = state.searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch
            )

            // Filter chips row
            FilterChipsRow(
                sortOption = state.sortOption,
                filterLanguage = state.filterLanguage,
                filterLearningStage = state.filterLearningStage,
                availableLanguages = state.availableLanguages,
                onSortOptionChange = onSortOptionChange,
                onFilterLanguageChange = onFilterLanguageChange,
                onFilterLearningStageChange = onFilterLearningStageChange
            )

            // Word count summary
            WordCountSummary(
                filteredCount = state.filteredWords.size,
                totalCount = state.words.size,
                isFiltered = state.isFiltered
            )

            // Words list
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

                // Empty search results
                if (state.searchQuery.isNotBlank() && state.filteredWords.isEmpty()) {
                    item {
                        EmptySearchView()
                    }
                }
            }
        }

        // Bottom selection action bar
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

// region Search & Filters

@Composable
internal fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    vertical = Theme.spacing.extraSmall
                ),
            placeholder = {
                Text(
                    text = stringResource(Res.string.search_words),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(Res.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(Theme.spacing.small)
        )
    }
}

@Composable
private fun FilterChipsRow(
    sortOption: WordSortOption,
    filterLanguage: Language?,
    filterLearningStage: LearningStage?,
    availableLanguages: Set<Language>,
    onSortOptionChange: (WordSortOption) -> Unit,
    onFilterLanguageChange: (Language?) -> Unit,
    onFilterLearningStageChange: (LearningStage?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
    ) {
        // Sort chip
        item {
            SortChip(
                currentOption = sortOption,
                onOptionSelected = onSortOptionChange
            )
        }

        // Language chip (only if multiple languages)
        if (availableLanguages.size > 1) {
            item {
                LanguageFilterChip(
                    selectedLanguage = filterLanguage,
                    availableLanguages = availableLanguages,
                    onLanguageSelected = onFilterLanguageChange
                )
            }
        }

        // Level chip
        item {
            LevelFilterChip(
                selectedStage = filterLearningStage,
                onStageSelected = onFilterLearningStageChange
            )
        }
    }
}

@Composable
private fun SortChip(
    currentOption: WordSortOption,
    onOptionSelected: (WordSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val sortLabel = when (currentOption) {
        WordSortOption.DATE_ADDED_DESC -> stringResource(Res.string.sort_newest)
        WordSortOption.DATE_ADDED_ASC -> stringResource(Res.string.sort_oldest)
        WordSortOption.ALPHABETICAL_AZ -> stringResource(Res.string.sort_a_to_z)
        WordSortOption.ALPHABETICAL_ZA -> stringResource(Res.string.sort_z_to_a)
        WordSortOption.LEVEL_ASC -> stringResource(Res.string.sort_level_asc)
        WordSortOption.LEVEL_DESC -> stringResource(Res.string.sort_level_desc)
    }

    Box {
        FilterChip(
            selected = currentOption != WordSortOption.DATE_ADDED_DESC,
            onClick = { expanded = true },
            label = { Text(sortLabel, style = MaterialTheme.typography.labelMedium) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            WordSortOption.entries.forEach { option ->
                val label = when (option) {
                    WordSortOption.DATE_ADDED_DESC -> stringResource(Res.string.sort_newest_first)
                    WordSortOption.DATE_ADDED_ASC -> stringResource(Res.string.sort_oldest_first)
                    WordSortOption.ALPHABETICAL_AZ -> stringResource(Res.string.sort_a_to_z)
                    WordSortOption.ALPHABETICAL_ZA -> stringResource(Res.string.sort_z_to_a)
                    WordSortOption.LEVEL_ASC -> stringResource(Res.string.sort_level_low_high)
                    WordSortOption.LEVEL_DESC -> stringResource(Res.string.sort_level_high_low)
                }
                DropdownMenuItem(
                    text = {
                        Text(
                            label,
                            fontWeight = if (option == currentOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageFilterChip(
    selectedLanguage: Language?,
    availableLanguages: Set<Language>,
    onLanguageSelected: (Language?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedLanguage != null,
            onClick = { expanded = true },
            label = {
                Text(
                    selectedLanguage?.displayName ?: stringResource(Res.string.filter_language),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.filter_all_languages)) },
                onClick = {
                    onLanguageSelected(null)
                    expanded = false
                }
            )
            availableLanguages.sortedBy { it.displayName }.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            "${language.nativeName} (${language.displayName})",
                            fontWeight = if (language == selectedLanguage) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onLanguageSelected(language)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LevelFilterChip(
    selectedStage: LearningStage?,
    onStageSelected: (LearningStage?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val stageLabel = selectedStage?.let { stageName(it) } ?: stringResource(Res.string.filter_level)

    Box {
        FilterChip(
            selected = selectedStage != null,
            onClick = { expanded = true },
            label = { Text(stageLabel, style = MaterialTheme.typography.labelMedium) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.filter_all_levels)) },
                onClick = {
                    onStageSelected(null)
                    expanded = false
                }
            )
            LearningStage.entries.forEach { stage ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(Theme.spacing.xs)
                                    .clip(RoundedCornerShape(Theme.shapes.extraSmall))
                                    .background(levelColor(stage))
                            )
                            Text(
                                stageName(stage),
                                fontWeight = if (stage == selectedStage) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onStageSelected(stage)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WordCountSummary(
    filteredCount: Int,
    totalCount: Int,
    isFiltered: Boolean
) {
    val pattern1 = "%1" + '$' + "d"
    val pattern2 = "%2" + '$' + "d"
    val text = if (isFiltered && filteredCount != totalCount) {
        stringResource(Res.string.word_count_filtered_format)
            .replace(pattern1, filteredCount.toString())
            .replace(pattern2, totalCount.toString())
    } else {
        stringResource(Res.string.word_count_format)
            .replace(pattern1, totalCount.toString())
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            vertical = Theme.spacing.extraSmall3
        )
    )
}

// endregion

// region Word Card

@Composable
internal fun WordCard(
    word: Word,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = LearningStage.fromLevel(word.level)
    val color = levelColor(stage)

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerLow,
        animationSpec = tween(200)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.dimensions.cardCornerRadius))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius),
        color = backgroundColor,
        tonalElevation = if (isSelected) Theme.elevation.medium else Theme.elevation.none
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Level color strip
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(color)
            )

            // Content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = Theme.spacing.extraSmall,
                        end = Theme.spacing.small,
                        top = Theme.spacing.extraSmall,
                        bottom = Theme.spacing.extraSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Word info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3)
                ) {
                    Text(
                        text = word.originalWord,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = word.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (word.description.isNotBlank()) {
                        Text(
                            text = word.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Meta row: languages + level pill + date
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LanguagePill(
                            text = word.targetLanguage.code.uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "\u2192",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        LanguagePill(
                            text = word.sourceLanguage.code.uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )

                        Text(
                            text = "\u00b7",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )

                        // Level pill
                        LevelPill(stage = stage, color = color)

                        if (word.dateAdded > 0L) {
                            Text(
                                text = "\u00b7 ${formatDateAdded(word.dateAdded)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Checkbox (only in selection mode)
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelPill(
    stage: LearningStage,
    color: androidx.compose.ui.graphics.Color
) {
    Pill(
        text = stageName(stage),
        color = color,
        backgroundAlpha = 0.15f,
        fontWeight = FontWeight.Medium
    )
}

// endregion

// region Selection Action Bar

@Composable
internal fun SelectionActionBar(
    isVisible: Boolean,
    selectedCount: Int,
    isUserSubscribed: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onBatchEditLanguages: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
        exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250)),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainer,
            shadowElevation = Theme.elevation.overlay,
            shape = RoundedCornerShape(topStart = Theme.shapes.large, topEnd = Theme.shapes.large)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = Theme.spacing.extraSmall,
                        vertical = Theme.spacing.extraSmall
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Count
                val countPattern = "%1" + '$' + "d"
                Text(
                    text = stringResource(Res.string.selected_format).replace(countPattern, selectedCount.toString()),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.weight(1f))

                // Select all
                IconButton(onClick = onSelectAll) {
                    Icon(
                        Icons.Default.SelectAll,
                        contentDescription = stringResource(Res.string.select_all),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Language edit
                IconButton(onClick = onBatchEditLanguages) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = stringResource(Res.string.batch_edit_languages),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Delete
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                // Share (premium only)
                if (isUserSubscribed) {
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.FileUpload,
                            contentDescription = stringResource(Res.string.share),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// endregion

// region Helper Views

@Composable
internal fun LoadingView() {
    LoadingScreen(message = stringResource(Res.string.loading_words))
}

@Composable
internal fun ErrorView(message: String) {
    ErrorScreen(
        message = message,
        title = stringResource(Res.string.error),
        icon = Icons.Default.Error
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
private fun EmptySearchView() {
    // Inline layout instead of EmptyScreen to avoid fillMaxSize/verticalScroll inside LazyColumn
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg)
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
    }
}

@Composable
private fun LanguagePill(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Pill(
        text = text,
        color = color,
        modifier = modifier,
        cornerRadius = Theme.shapes.extraSmall
    )
}

// endregion

// region Utilities

private fun formatDateAdded(timestamp: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestamp)
    val timeZone = TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(timeZone)
    val month = localDateTime.month.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    return "${localDateTime.dayOfMonth} $month ${localDateTime.year}"
}

internal fun levelColor(stage: LearningStage): androidx.compose.ui.graphics.Color {
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

// endregion
