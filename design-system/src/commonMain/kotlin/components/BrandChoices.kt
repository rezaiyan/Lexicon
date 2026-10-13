package components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import theme.Theme

/*
 * Selection controls in brand colours. Material's default selected colour is the green secondary,
 * which reads as "success" here; selection uses a soft primary tint with a primary edge instead.
 */

/** A pill [FilterChip] whose selection is the brand tint; the label turns bold when selected. */
@Composable
fun BrandFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = primary.copy(alpha = Theme.opacity.focus),
            selectedLabelColor = primary,
            selectedLeadingIconColor = primary,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            selectedBorderColor = primary,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

/**
 * Equal-width single choice in brand colours. [selectedIndex] null shows none selected, for
 * optional choices where tapping the selected option clears it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandSegmentedRow(
    labels: List<String>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: (Int) -> Boolean = { true },
) {
    val primary = MaterialTheme.colorScheme.primary
    val colors = SegmentedButtonDefaults.colors(
        activeContainerColor = primary.copy(alpha = Theme.opacity.focus),
        activeContentColor = primary,
        activeBorderColor = primary,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
    )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            SegmentedButton(
                selected = selected,
                onClick = { onSelect(index) },
                enabled = enabled(index),
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
                colors = colors,
                icon = {},
                label = {
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}
