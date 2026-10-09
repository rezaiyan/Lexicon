package feature.profile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import domain.common.util.EpochDateFormatter
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.credits_balance_refills
import lexicon.resources.generated.resources.credits_balance_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/**
 * "305 AI credits · Refills Nov 9, 2026" at the top of the profile. Follows the shared balance, so
 * it changes as soon as a spend is known. Opens the plan page, where the monthly allowance lives.
 */
@Composable
internal fun CreditsBalanceCard(
    balance: Int,
    refillsAtMillis: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.large),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Text(
                text = pluralStringResource(Res.plurals.credits_balance_title, balance, balance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            refillsAtMillis?.let {
                Text(
                    text = stringResource(Res.string.credits_balance_refills, EpochDateFormatter.toMediumDate(it)),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
