package presentation.ui.components.imports

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import components.sheet.SheetBadge
import components.sheet.SheetPage
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTextButton
import domain.common.util.EpochDateFormatter
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.credits_balance_left
import lexicon.resources.generated.resources.credits_balance_left_refills
import lexicon.resources.generated.resources.credits_count
import lexicon.resources.generated.resources.out_of_credits_cost
import lexicon.resources.generated.resources.out_of_credits_refill
import lexicon.resources.generated.resources.out_of_credits_title
import lexicon.resources.generated.resources.out_of_credits_type_instead
import lexicon.resources.generated.resources.out_of_credits_upgrade
import lexicon.resources.generated.resources.out_of_credits_upgrade_amount
import lexicon.resources.generated.resources.out_of_credits_upgrade_pitch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

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

/** "12 credits left · refills Nov 9, 2026" under the sources. */
@Composable
internal fun CreditsBalanceLine(balance: Int, refillsAtMillis: Long?, modifier: Modifier = Modifier) {
    val credits = creditsText(balance)
    val refillDate = refillsAtMillis?.let(EpochDateFormatter::toMediumDate)
    Text(
        text = refillDate
            ?.let { stringResource(Res.string.credits_balance_left_refills, credits, it) }
            ?: stringResource(Res.string.credits_balance_left, credits),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
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
