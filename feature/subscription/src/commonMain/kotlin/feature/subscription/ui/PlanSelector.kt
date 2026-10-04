package feature.subscription.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.Pill
import domain.subscription.model.PackagePeriod
import feature.subscription.PlanOption
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.billing_period_annual
import lexicon.resources.generated.resources.billing_period_monthly
import lexicon.resources.generated.resources.paywall_best_value
import lexicon.resources.generated.resources.paywall_billed_monthly
import lexicon.resources.generated.resources.paywall_billed_yearly
import lexicon.resources.generated.resources.paywall_per_month_label
import lexicon.resources.generated.resources.paywall_save_percent
import lexicon.resources.generated.resources.paywall_trial_days
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

/**
 * Radio-style plan picker. Every tile states the full price and how it's billed, so the
 * per-month figure for annual never hides what is actually charged.
 */
@Composable
fun PlanSelector(
    plans: List<PlanOption>,
    selectedPlanId: String?,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        plans.forEach { plan ->
            PlanTile(
                plan = plan,
                selected = plan.identifier == selectedPlanId,
                enabled = enabled,
                onClick = { onSelect(plan.identifier) },
            )
        }
    }
}

@Composable
private fun PlanTile(
    plan: PlanOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val accent = AppColors.subscriptionPremiumAccent
    val shape = RoundedCornerShape(Theme.shapes.large)
    val borderColor by animateColorAsState(
        if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        label = "planBorder",
    )
    val borderWidth by animateDpAsState(if (selected) 2.dp else Theme.dimensions.borderWidth, label = "planBorderWidth")
    val isAnnual = plan.period == PackagePeriod.ANNUAL

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick),
        shape = shape,
        color = if (selected) accent.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(Theme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Icon(
                imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (selected) accent else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(Theme.dimensions.iconSize),
            )

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                ) {
                    Text(
                        text = stringResource(
                            if (isAnnual) Res.string.billing_period_annual else Res.string.billing_period_monthly
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (isAnnual) {
                        val badge = plan.savingsPercent
                            ?.let { stringResource(Res.string.paywall_save_percent, it) }
                            ?: stringResource(Res.string.paywall_best_value)
                        Pill(text = badge, color = Theme.colors.success)
                    }
                }
                Text(
                    text = if (isAnnual) {
                        stringResource(Res.string.paywall_billed_yearly, plan.price)
                    } else {
                        stringResource(Res.string.paywall_billed_monthly)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                plan.trialDays?.let { days ->
                    Text(
                        text = stringResource(Res.string.paywall_trial_days, days),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = accent,
                    )
                }
            }

            Box(contentAlignment = Alignment.CenterEnd) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plan.perMonthPrice ?: plan.price,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(Res.string.paywall_per_month_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
