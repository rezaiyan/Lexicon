package components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import components.dialog.ContentToolbar
import theme.Theme

/*
 * Lexicon bottom-sheet kit. Every sheet page shares one anatomy:
 * toolbar (back / close) → left-aligned header (eyebrow, title, subtitle) → body → footer under a divider.
 * The sheet container already insets content by md on every side, so pages add no horizontal padding.
 */

/**
 * Standard sheet page.
 *
 * Inside [overlay.bottomsheet.BottomSheetPages] the pager owns the toolbar, so leave [onBack]/[onClose] null.
 * A standalone sheet passes them to get the same 48dp toolbar row.
 * The body scrolls when [scrollable]; pass false when it hosts its own lazy list.
 */
@Composable
fun SheetPage(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    highlight: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null,
    headerAccessory: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = Theme.spacing
    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        ContentToolbar(onBack = onBack, onClose = onClose)
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(bottom = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.textGap)) {
                headerAccessory?.let {
                    it()
                    Spacer(Modifier.size(spacing.xs))
                }
                eyebrow?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                SheetTitle(title = title, highlight = highlight)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }

        if (footer != null) {
            SheetFooter(content = footer)
        } else {
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** Sticky action area: hairline divider, then buttons stacked with xs spacing. */
@Composable
fun SheetFooter(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = Theme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        content = content,
    )
}

/** headlineSmall title; [highlight] is appended in the accent color ("What topics *interest you?*"). */
@Composable
fun SheetTitle(title: String, modifier: Modifier = Modifier, highlight: String? = null) {
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            append(title)
            if (highlight != null) {
                append(" ")
                withStyle(SpanStyle(color = accent)) { append(highlight) }
            }
        },
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun SheetSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
