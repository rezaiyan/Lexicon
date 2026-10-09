package presentation.ui.components.imports

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
import androidx.compose.ui.text.style.TextAlign
import components.sheet.SheetBadge
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTextButton
import domain.common.util.EpochDateFormatter
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.credits_balance_refills
import lexicon.resources.generated.resources.credits_balance_title
import lexicon.resources.generated.resources.credits_count
import lexicon.resources.generated.resources.credits_spend_hint
import lexicon.resources.generated.resources.out_of_credits_cost
import lexicon.resources.generated.resources.out_of_credits_refill
import lexicon.resources.generated.resources.out_of_credits_title
import lexicon.resources.generated.resources.out_of_credits_type_instead
import lexicon.resources.generated.resources.out_of_credits_upgrade
import lexicon.resources.generated.resources.out_of_credits_upgrade_amount
import lexicon.resources.generated.resources.out_of_credits_upgrade_pitch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import theme.Theme

/** "1 credit" / "12 credits". */
@Composable
internal fun creditsText(count: Int): String = pluralStringResource(Res.plurals.credits_count, count, count)

/** What a paid source costs, next to its title. Nothing for a free (0) or unknown cost. */
@Composable
internal fun CreditCostBadge(cost: Int?, modifier: Modifier = Modifier) {
    if (cost == null || cost <= 0) return
    SheetBadge(
        text = creditsText(cost),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    )
}

/**
 * The balance, at the top of the add-words sheet so it's the first thing seen before choosing a
 * source. Follows the shared balance, so it changes as soon as a spend is known.
 */
@Composable
internal fun CreditsBalanceCard(balance: Int, refillsAtMillis: Long?, modifier: Modifier = Modifier) {
    Surface(
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

/** "Uses 1 credit · 309 left", under the button that spends. Nothing while either is unknown. */
@Composable
internal fun CreditSpendHint(cost: Int?, balance: Int?, modifier: Modifier = Modifier) {
    if (cost == null || cost <= 0 || balance == null) return
    Text(
        text = stringResource(Res.string.credits_spend_hint, creditsText(cost), balance),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * A paid source costs more than the balance. Explains it, says when credits come back, and offers
 * the ways forward: upgrading (when [onUpgrade] is set) or adding words by hand, which is free.
 *
 * @param premiumMonthlyAllowance credits premium includes per month, for the upgrade button.
 */
@Composable
internal fun OutOfCreditsContent(
    cost: Int?,
    balance: Int?,
    refillsAtMillis: Long?,
    premiumMonthlyAllowance: Int?,
    onUpgrade: (() -> Unit)?,
    onTypeInstead: () -> Unit,
) {
    SheetPage(
        title = stringResource(Res.string.out_of_credits_title),
        subtitle = if (cost != null && balance != null) {
            stringResource(Res.string.out_of_credits_cost, creditsText(cost), creditsText(balance))
        } else {
            null
        },
        footer = {
            if (onUpgrade != null) {
                SheetPrimaryButton(
                    text = premiumMonthlyAllowance
                        ?.let { stringResource(Res.string.out_of_credits_upgrade_amount, it) }
                        ?: stringResource(Res.string.out_of_credits_upgrade),
                    icon = Icons.Default.AutoAwesome,
                    onClick = onUpgrade,
                )
            }
            SheetTextButton(text = stringResource(Res.string.out_of_credits_type_instead), onClick = onTypeInstead)
        },
    ) {
        refillsAtMillis?.let {
            Text(
                text = stringResource(Res.string.out_of_credits_refill, EpochDateFormatter.toMediumDate(it)),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (onUpgrade != null) {
            Text(
                text = stringResource(Res.string.out_of_credits_upgrade_pitch),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
