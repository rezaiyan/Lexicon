package presentation.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import domain.tag.model.Tag
import domain.word.model.LearningStage
import feature.words.model.WordSortOption
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.filter_all_languages
import lexicon.resources.generated.resources.filter_all_levels
import lexicon.resources.generated.resources.filter_all_tags
import lexicon.resources.generated.resources.filter_language
import lexicon.resources.generated.resources.filter_level
import lexicon.resources.generated.resources.filter_tag
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
import lexicon.resources.generated.resources.word_count_filtered_format
import lexicon.resources.generated.resources.word_count_format
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

@Composable
internal fun SearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
internal fun FilterChipsRow(
    sortOption: WordSortOption,
    filterLanguage: Language?,
    filterLearningStage: LearningStage?,
    filterTagId: Long?,
    tags: List<Tag>,
    availableLanguages: Set<Language>,
    onSortOptionChange: (WordSortOption) -> Unit,
    onFilterLanguageChange: (Language?) -> Unit,
    onFilterLearningStageChange: (LearningStage?) -> Unit,
    onFilterTagChange: (Long?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
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

        if (tags.isNotEmpty()) {
            item {
                TagFilterChip(
                    selectedTagId = filterTagId,
                    tags = tags,
                    onTagSelected = onFilterTagChange
                )
            }
        }
    }
}

@Composable
private fun TagFilterChip(
    selectedTagId: Long?,
    tags: List<Tag>,
    onTagSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTag = tags.find { it.id == selectedTagId }

    Box {
        FilterChip(
            selected = selectedTagId != null,
            onClick = { expanded = true },
            label = {
                Text(
                    selectedTag?.name ?: stringResource(Res.string.filter_tag),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.filter_all_tags)) },
                onClick = {
                    onTagSelected(null)
                    expanded = false
                }
            )
            tags.forEach { tag ->
                DropdownMenuItem(
                    text = {
                        Text(
                            tag.name,
                            fontWeight = if (tag.id == selectedTagId) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onTagSelected(tag.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun WordCountSummary(
    filteredCount: Int,
    totalCount: Int,
    isFiltered: Boolean,
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
