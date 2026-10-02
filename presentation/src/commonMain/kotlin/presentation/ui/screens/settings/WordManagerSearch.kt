package presentation.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.tag.model.Tag
import domain.word.model.LearningStage
import domain.word.model.WordSortOption
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.filter_all
import lexicon.resources.generated.resources.filter_all_languages
import lexicon.resources.generated.resources.filter_all_levels
import lexicon.resources.generated.resources.filter_language
import lexicon.resources.generated.resources.filter_level
import lexicon.resources.generated.resources.filters
import lexicon.resources.generated.resources.search_words
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
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

@Composable
internal fun SearchRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    filtersActive: Boolean,
    filtersExpanded: Boolean,
    onToggleFilters: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onClear = onClearSearch,
            modifier = Modifier.weight(1f),
        )
        FilterToggleButton(
            active = filtersActive,
            expanded = filtersExpanded,
            onClick = onToggleFilters,
        )
    }
}

@Composable
private fun PillSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val placeholder = stringResource(Res.string.search_words)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.height(Theme.dimensions.touchTarget),
        decorationBox = { innerTextField ->
            Surface(
                shape = RoundedCornerShape(Theme.shapes.pill),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = Theme.elevation.low,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = Theme.spacing.md, end = Theme.spacing.xxs),
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotBlank()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = stringResource(Res.string.cancel),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun FilterToggleButton(
    active: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val highlighted = active || expanded
    Box {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            color = if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)
            } else {
                MaterialTheme.colorScheme.surface
            },
            shadowElevation = if (highlighted) Theme.elevation.none else Theme.elevation.low,
            modifier = Modifier.size(Theme.dimensions.touchTarget),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = stringResource(Res.string.filters),
                    tint = if (highlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
            }
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Theme.spacing.xxs)
                    .size(Theme.spacing.xs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
internal fun TagChipsRow(
    totalCount: Int,
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        item(key = "all") {
            CountChip(
                label = stringResource(Res.string.filter_all),
                count = totalCount,
                selected = selectedTagId == null,
                onClick = { onTagSelected(null) },
            )
        }
        items(tags, key = { it.id }) { tag ->
            CountChip(
                label = tag.name,
                count = tag.wordCount.toInt(),
                selected = tag.id == selectedTagId,
                onClick = { onTagSelected(if (tag.id == selectedTagId) null else tag.id) },
            )
        }
    }
}

@Composable
private fun CountChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Theme.shapes.pill),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shadowElevation = if (selected) Theme.elevation.none else Theme.elevation.low,
        modifier = Modifier.height(Theme.dimensions.touchTargetSmall),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Theme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = Theme.opacity.muted)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
internal fun FilterChipsRow(
    sortOption: WordSortOption,
    filterLanguage: Language?,
    filterLearningStage: LearningStage?,
    availableLanguages: Set<Language>,
    onSortOptionChange: (WordSortOption) -> Unit,
    onFilterLanguageChange: (Language?) -> Unit,
    onFilterLearningStageChange: (LearningStage?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
    ) {
        item {
            SortChip(
                currentOption = sortOption,
                onOptionSelected = onSortOptionChange
            )
        }

        if (availableLanguages.size > 1) {
            item {
                LanguageFilterChip(
                    selectedLanguage = filterLanguage,
                    availableLanguages = availableLanguages,
                    onLanguageSelected = onFilterLanguageChange
                )
            }
        }

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
    onOptionSelected: (WordSortOption) -> Unit,
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
    onLanguageSelected: (Language?) -> Unit,
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
    onStageSelected: (LearningStage?) -> Unit,
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
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
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
