package components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import theme.Theme

private const val StepMillis = 2500L
private val RingSize = 120.dp

/**
 * Spinner ring, title, optional summary and a checklist for a long request with no progress signal.
 * The checklist advances on a timer and holds on the last step until the caller swaps the content out.
 */
@Composable
fun GeneratingProgress(
    title: String,
    steps: List<String>,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    var activeIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(steps.size) {
        while (activeIndex < steps.lastIndex) {
            delay(StepMillis)
            activeIndex++
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(RingSize),
                strokeWidth = Theme.spacing.xs,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.iconSizeMassive + Theme.spacing.xxs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            steps.forEachIndexed { index, label ->
                ChecklistRow(text = label, done = index < activeIndex, active = index == activeIndex)
            }
        }
    }
}

@Composable
private fun ChecklistRow(text: String, done: Boolean, active: Boolean) {
    val accent = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.outlineVariant
    val labelColor = if (done || active) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Theme.spacing.xs + Theme.spacing.xxxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.iconSize)
                .clip(CircleShape)
                .then(
                    when {
                        done -> Modifier.background(accent)
                        active -> Modifier.border(Theme.spacing.xxxs, accent, CircleShape)
                        else -> Modifier.border(Theme.spacing.xxxs, idle, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Theme.dimensions.iconSizeSmall - Theme.spacing.xxxs),
                )
                active -> Box(Modifier.size(Theme.spacing.xs).clip(CircleShape).background(accent))
            }
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = labelColor,
        )
    }
}
