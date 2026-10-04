package presentation.ui.components.imports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.sheet.LanguageCodeTile
import components.sheet.LanguageListRow
import components.sheet.SheetGroup
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetSearchField
import components.sheet.SheetSectionLabel
import components.sheet.SheetTonalButton
import domain.tag.model.Tag
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.confirm_import_subtitle
import lexicon.resources.generated.resources.confirm_import_title
import lexicon.resources.generated.resources.field_tag
import lexicon.resources.generated.resources.import_words_cta
import lexicon.resources.generated.resources.new_tag
import lexicon.resources.generated.resources.no_tag
import lexicon.resources.generated.resources.search_languages
import lexicon.resources.generated.resources.swap_languages
import lexicon.resources.generated.resources.translated_to
import lexicon.resources.generated.resources.words_are_in
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.Language

private const val PreviewLineCount = 3

/** "Check your languages" — confirms source/target before a file import, with a peek at the first lines. */
@Composable
internal fun ImportLanguageConfirmationContent(
    sourceLanguage: Language,
    targetLanguage: Language,
    previewLines: List<Pair<String, String>>,
    totalLines: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
    onSwapLanguages: () -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.confirm_import_title),
        subtitle = stringResource(Res.string.confirm_import_subtitle),
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                SheetTonalButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.width(IntrinsicSize.Max),
                )
                SheetPrimaryButton(
                    text = stringResource(Res.string.import_words_cta),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        LanguagePairCard(
            topLabel = stringResource(Res.string.words_are_in),
            top = sourceLanguage,
            onTopClick = onShowSourceLanguage,
            bottomLabel = stringResource(Res.string.translated_to),
            bottom = targetLanguage,
            onBottomClick = onShowTargetLanguage,
            onSwap = onSwapLanguages,
        )

        if (previewLines.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Theme.shapes.medium))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs + Theme.spacing.xxxs),
            ) {
                previewLines.take(PreviewLineCount).forEach { (word, translation) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                        Text(
                            word,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                }
                val remaining = totalLines - PreviewLineCount
                if (remaining > 0) {
                    Text(
                        "+ $remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Two tappable language rows ("Words are in" / "Translated to") with a swap button straddling the divider. */
@Composable
internal fun LanguagePairCard(
    topLabel: String,
    top: Language,
    onTopClick: () -> Unit,
    bottomLabel: String,
    bottom: Language,
    onBottomClick: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        SheetGroup {
            LanguageRow(
                label = topLabel,
                language = top,
                highlighted = true,
                onClick = onTopClick,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
            LanguageRow(
                label = bottomLabel,
                language = bottom,
                highlighted = false,
                onClick = onBottomClick,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                Theme.dimensions.borderWidth,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
            ),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = Theme.spacing.xxxl),
        ) {
            IconButton(onClick = onSwap) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = stringResource(Res.string.swap_languages),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    language: Language,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        LanguageCodeTile(code = language.code, selected = highlighted)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(language.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
    }
}

/**
 * Searchable language list. Used for the manual word language (first time and via the
 * language chip), the confirm-languages pickers, and the AI wizard's language steps.
 */
@Composable
internal fun ImportLanguageListPage(
    title: String,
    languages: List<Language>,
    selected: Language?,
    onLanguageSelected: (Language) -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    highlight: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    pinned: (@Composable () -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, languages) {
        val q = query.trim()
        if (q.isEmpty()) {
            languages
        } else {
            languages.filter {
                it.displayName.contains(q, ignoreCase = true) || it.nativeName.contains(q, ignoreCase = true)
            }
        }
    }

    SheetPage(
        title = title,
        eyebrow = eyebrow,
        highlight = highlight,
        subtitle = subtitle,
        onBack = onBack,
        onClose = onClose,
        modifier = modifier,
    ) {
        pinned?.invoke()
        SheetSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(Res.string.search_languages),
        )
        Column {
            filtered.forEachIndexed { index, language ->
                LanguageListRow(
                    code = language.code,
                    name = language.displayName,
                    nativeName = language.nativeName,
                    selected = language == selected,
                    showDivider = index < filtered.lastIndex,
                    onClick = { onLanguageSelected(language) },
                )
            }
        }
    }
}

/** "Tag" label plus a horizontal row of chips: None, each tag, and a dashed "New tag". */
@Composable
internal fun TagSelectorRow(
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    onCreateTag: (() -> Unit)? = null,
) {
    if (tags.isEmpty() && onCreateTag == null) return
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        SheetSectionLabel(stringResource(Res.string.field_tag))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
            item(key = "none") {
                TagChip(
                    label = stringResource(Res.string.no_tag),
                    selected = selectedTagId == null,
                    onClick = { onTagSelected(null) },
                )
            }
            items(tags, key = { it.id }) { tag ->
                TagChip(
                    label = tag.name,
                    selected = selectedTagId == tag.id,
                    onClick = { onTagSelected(tag.id) },
                )
            }
            if (onCreateTag != null) {
                item(key = "new") {
                    Surface(
                        onClick = onCreateTag,
                        shape = RoundedCornerShape(Theme.shapes.pill),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(Theme.dimensions.borderWidth, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = Theme.dimensions.touchTargetSmall)
                                .padding(horizontal = Theme.spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(Res.string.new_tag),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TagChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
        shape = RoundedCornerShape(Theme.shapes.pill),
        color = if (selected) {
            accent.copy(alpha = Theme.opacity.focus)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = Theme.dimensions.touchTargetSmall)
                .padding(horizontal = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
