package feature.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoneyOff
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.Pill
import components.sheet.SheetPrimaryButton
import domain.subscription.model.PackagePeriod
import feature.subscription.model.Membership
import feature.subscription.model.MembershipStatus
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel_subscription
import lexicon.resources.generated.resources.paywall_premium_unlocks
import lexicon.resources.generated.resources.sub_active_note
import lexicon.resources.generated.resources.sub_billing_fact_access
import lexicon.resources.generated.resources.sub_billing_fact_fix
import lexicon.resources.generated.resources.sub_billing_fix
import lexicon.resources.generated.resources.sub_billing_title
import lexicon.resources.generated.resources.sub_canceled_days_left
import lexicon.resources.generated.resources.sub_canceled_fact_access
import lexicon.resources.generated.resources.sub_canceled_fact_after
import lexicon.resources.generated.resources.sub_canceled_fact_no_charge
import lexicon.resources.generated.resources.sub_canceled_last_day
import lexicon.resources.generated.resources.sub_canceled_one_day_left
import lexicon.resources.generated.resources.sub_canceled_title
import lexicon.resources.generated.resources.sub_canceled_trial_title
import lexicon.resources.generated.resources.sub_days_left
import lexicon.resources.generated.resources.sub_grant_note
import lexicon.resources.generated.resources.sub_keep_premium
import lexicon.resources.generated.resources.sub_keep_premium_hint
import lexicon.resources.generated.resources.sub_last_day
import lexicon.resources.generated.resources.sub_lifetime_note
import lexicon.resources.generated.resources.sub_manage_in_store
import lexicon.resources.generated.resources.sub_next_grant_end
import lexicon.resources.generated.resources.sub_next_renewal
import lexicon.resources.generated.resources.sub_next_trial_end
import lexicon.resources.generated.resources.sub_one_day_left
import lexicon.resources.generated.resources.sub_pause_detail
import lexicon.resources.generated.resources.sub_pause_next
import lexicon.resources.generated.resources.sub_pause_note
import lexicon.resources.generated.resources.sub_paused_fact_free
import lexicon.resources.generated.resources.sub_paused_fact_no_charge
import lexicon.resources.generated.resources.sub_paused_fact_progress
import lexicon.resources.generated.resources.sub_paused_resume_now
import lexicon.resources.generated.resources.sub_paused_resumes
import lexicon.resources.generated.resources.sub_paused_title
import lexicon.resources.generated.resources.sub_plan_generic
import lexicon.resources.generated.resources.sub_plan_monthly
import lexicon.resources.generated.resources.sub_plan_yearly
import lexicon.resources.generated.resources.sub_price_after_trial
import lexicon.resources.generated.resources.sub_price_renewal
import lexicon.resources.generated.resources.sub_status_active
import lexicon.resources.generated.resources.sub_status_canceled
import lexicon.resources.generated.resources.sub_status_granted
import lexicon.resources.generated.resources.sub_status_payment_issue
import lexicon.resources.generated.resources.sub_status_pausing
import lexicon.resources.generated.resources.sub_status_trial
import lexicon.resources.generated.resources.sub_trial_note
import lexicon.resources.generated.resources.sub_your_features
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

/**
 * Member view: a card that answers "what do I have, what happens next, and what will it cost",
 * then what the plan includes. A canceled-but-unexpired plan gets its own explanation so nobody
 * mistakes "canceled" for "already gone".
 */
@Composable
fun SubscriptionActiveContent(
    membership: Membership,
    monthlyAiCredits: Int?,
    onManage: () -> Unit,
) {
    val status = membership.status
    val canCancel = membership.isManageable &&
        (status is MembershipStatus.Renewing || status is MembershipStatus.Trial)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        MembershipCard(membership = membership, onManage = onManage)

        PremiumFeatureList(
            title = stringResource(Res.string.sub_your_features),
            monthlyAiCredits = monthlyAiCredits,
            unlocked = true,
        )

        if (canCancel) {
            TextButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.cancel_subscription),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MembershipCard(membership: Membership, onManage: () -> Unit) {
    val status = membership.status
    val accent = status.accentColor()
    val cornerRadius = Theme.shapes.extraLarge

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(cornerRadius)),
    ) {
        PlanHeader(
            planName = planName(membership.period),
            price = membership.price,
            statusLabel = status.label(),
            statusColor = accent,
            cornerRadius = cornerRadius,
        )

        Column(
            modifier = Modifier.padding(Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            when (status) {
                is MembershipStatus.Canceled -> CanceledDetails(
                    status = status,
                    accent = accent,
                    isManageable = membership.isManageable,
                    onKeepPremium = onManage,
                )
                is MembershipStatus.Renewing -> {
                    NextEventRow(
                        label = stringResource(Res.string.sub_next_renewal),
                        date = status.renewsOn,
                        detail = membership.price?.let { stringResource(Res.string.sub_price_renewal, it) },
                        accent = accent,
                    )
                    CardNote(stringResource(Res.string.sub_active_note))
                    ManageButton(membership.isManageable, onManage)
                }
                is MembershipStatus.Trial -> {
                    NextEventRow(
                        label = stringResource(Res.string.sub_next_trial_end),
                        date = status.endsOn,
                        detail = membership.price?.let { stringResource(Res.string.sub_price_after_trial, it) },
                        accent = accent,
                        badge = daysLeftLabel(status.daysLeft),
                    )
                    CardNote(stringResource(Res.string.sub_trial_note))
                    ManageButton(membership.isManageable, onManage)
                }
                is MembershipStatus.Granted -> {
                    status.until?.let { until ->
                        NextEventRow(
                            label = stringResource(Res.string.sub_next_grant_end),
                            date = until,
                            detail = null,
                            accent = accent,
                        )
                    }
                    CardNote(stringResource(Res.string.sub_grant_note))
                }
                is MembershipStatus.BillingIssue -> BillingIssueDetails(status = status, onFixPayment = onManage)
                is MembershipStatus.PauseScheduled -> {
                    NextEventRow(
                        label = stringResource(Res.string.sub_pause_next),
                        date = status.pausesOn,
                        detail = stringResource(Res.string.sub_pause_detail, status.resumesOn),
                        accent = accent,
                    )
                    CardNote(stringResource(Res.string.sub_pause_note))
                    ManageButton(membership.isManageable, onManage)
                }
                MembershipStatus.Lifetime -> CardNote(stringResource(Res.string.sub_lifetime_note))
            }
        }
    }
}

@Composable
private fun PlanHeader(
    planName: String,
    price: String?,
    statusLabel: String,
    statusColor: Color,
    cornerRadius: androidx.compose.ui.unit.Dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.gradients.premiumHero, RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
            .padding(Theme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTarget)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(Theme.shapes.large)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
            Text(
                text = planName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            price?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
        Pill(
            text = statusLabel,
            color = Color.White,
            backgroundColor = statusColor,
            height = Theme.spacing.lg,
            cornerRadius = Theme.spacing.sm,
        )
    }
}

/**
 * Canceled is not expired: the user still has everything until [MembershipStatus.Canceled.accessEndsOn].
 * Lead with that, show how much is left, state the three consequences plainly, and offer the
 * one action that undoes it.
 */
@Composable
private fun ColumnScope.CanceledDetails(
    status: MembershipStatus.Canceled,
    accent: Color,
    isManageable: Boolean,
    onKeepPremium: () -> Unit,
) {
    Text(
        text = if (status.wasTrial) {
            stringResource(Res.string.sub_canceled_trial_title)
        } else {
            stringResource(Res.string.sub_canceled_title, status.accessEndsOn)
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        Text(
            text = when (status.daysLeft) {
                0 -> stringResource(Res.string.sub_canceled_last_day)
                1 -> stringResource(Res.string.sub_canceled_one_day_left)
                else -> stringResource(Res.string.sub_canceled_days_left, status.daysLeft)
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        status.remainingFraction?.let { remaining ->
            LinearProgressIndicator(
                progress = { remaining },
                modifier = Modifier.fillMaxWidth().height(Theme.dimensions.progressBarHeight),
                color = accent,
                trackColor = accent.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
        }
    }

    HorizontalDivider(thickness = Theme.dimensions.hairlineThickness, color = MaterialTheme.colorScheme.outlineVariant)

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        FactRow(
            Icons.Outlined.CheckCircle,
            Theme.colors.success,
            stringResource(Res.string.sub_canceled_fact_access, status.accessEndsOn),
        )
        FactRow(Icons.Outlined.MoneyOff, Theme.colors.success, stringResource(Res.string.sub_canceled_fact_no_charge))
        FactRow(
            Icons.Outlined.Restore,
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(Res.string.sub_canceled_fact_after),
        )
    }

    if (isManageable) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            SheetPrimaryButton(
                text = stringResource(Res.string.sub_keep_premium),
                onClick = onKeepPremium,
                containerColor = AppColors.subscriptionPremiumAccent,
                contentColor = Color.White,
            )
            Text(
                text = stringResource(Res.string.sub_keep_premium_hint, storeName()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The store couldn't charge the renewal. Premium usually keeps working while the store retries
 * (grace period), so say how long, and make fixing the payment the one obvious action. The button
 * shows even when this device didn't make the purchase: the store page is where it gets fixed.
 */
@Composable
private fun BillingIssueDetails(status: MembershipStatus.BillingIssue, onFixPayment: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.sub_billing_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        status.daysLeft?.let { daysLeft ->
            Pill(
                text = daysLeftLabel(daysLeft),
                color = MaterialTheme.colorScheme.error,
                height = Theme.spacing.lg,
                cornerRadius = Theme.spacing.sm,
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
        status.accessEndsOn?.let { date ->
            FactRow(
                Icons.Outlined.CheckCircle,
                Theme.colors.success,
                stringResource(Res.string.sub_billing_fact_access, date),
            )
        }
        FactRow(
            Icons.Outlined.CreditCard,
            MaterialTheme.colorScheme.error,
            stringResource(Res.string.sub_billing_fact_fix),
        )
    }

    SheetPrimaryButton(
        text = stringResource(Res.string.sub_billing_fix, storeName()),
        onClick = onFixPayment,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    )
}

/**
 * A paused store subscription: not a member right now, but not a stranger to sell to either.
 * Say when Premium comes back on its own, that nothing is charged or lost meanwhile, and how to
 * resume early.
 */
@Composable
fun SubscriptionPausedContent(
    resumesOn: String,
    daysLeft: Int,
    monthlyAiCredits: Int?,
    onResume: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.xxxl),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Theme.shapes.extraLarge))
                .padding(Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                Icon(
                    Icons.Outlined.PauseCircle,
                    contentDescription = null,
                    tint = AppColors.subscriptionPremiumAccent,
                    modifier = Modifier.size(Theme.dimensions.touchTargetSmall),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs),
                ) {
                    Text(
                        text = stringResource(Res.string.sub_paused_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.sub_paused_resumes, resumesOn),
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                    )
                }
                Pill(
                    text = daysLeftLabel(daysLeft),
                    color = accent,
                    height = Theme.spacing.lg,
                    cornerRadius = Theme.spacing.sm,
                )
            }

            HorizontalDivider(
                thickness = Theme.dimensions.hairlineThickness,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm)) {
                FactRow(
                    Icons.Outlined.MoneyOff,
                    Theme.colors.success,
                    stringResource(Res.string.sub_paused_fact_no_charge),
                )
                FactRow(
                    Icons.Outlined.CheckCircle,
                    Theme.colors.success,
                    stringResource(Res.string.sub_paused_fact_progress),
                )
                FactRow(Icons.Outlined.Info, accent, stringResource(Res.string.sub_paused_fact_free))
            }

            SheetPrimaryButton(
                text = stringResource(Res.string.sub_paused_resume_now, storeName()),
                onClick = onResume,
                containerColor = AppColors.subscriptionPremiumAccent,
                contentColor = Color.White,
            )
        }

        PremiumFeatureList(
            title = stringResource(Res.string.paywall_premium_unlocks),
            monthlyAiCredits = monthlyAiCredits,
        )
    }
}

@Composable
private fun FactRow(icon: ImageVector, tint: Color, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Theme.dimensions.iconSizeMedium))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** The one date that matters next, with its cost consequence and an optional countdown. */
@Composable
private fun NextEventRow(
    label: String,
    date: String,
    detail: String?,
    accent: Color,
    badge: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(Theme.dimensions.touchTargetSmall)
                .background(accent.copy(alpha = 0.12f), RoundedCornerShape(Theme.shapes.medium)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Event,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        badge?.let { Pill(text = it, color = accent, height = Theme.spacing.lg, cornerRadius = Theme.spacing.sm) }
    }
}

@Composable
private fun CardNote(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ManageButton(isManageable: Boolean, onManage: () -> Unit) {
    if (!isManageable) return
    SheetPrimaryButton(
        text = stringResource(Res.string.sub_manage_in_store, storeName()),
        onClick = onManage,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
private fun daysLeftLabel(daysLeft: Int): String = when (daysLeft) {
    0 -> stringResource(Res.string.sub_last_day)
    1 -> stringResource(Res.string.sub_one_day_left)
    else -> stringResource(Res.string.sub_days_left, daysLeft)
}

@Composable
private fun MembershipStatus.accentColor(): Color = when (this) {
    is MembershipStatus.Renewing, is MembershipStatus.Granted, MembershipStatus.Lifetime -> Theme.colors.success
    is MembershipStatus.Trial, is MembershipStatus.PauseScheduled -> AppColors.subscriptionPremiumAccent
    is MembershipStatus.Canceled -> Theme.colors.warning
    is MembershipStatus.BillingIssue -> MaterialTheme.colorScheme.error
}

@Composable
private fun MembershipStatus.label(): String = stringResource(
    when (this) {
        is MembershipStatus.Renewing, MembershipStatus.Lifetime -> Res.string.sub_status_active
        is MembershipStatus.Trial -> Res.string.sub_status_trial
        is MembershipStatus.Canceled -> Res.string.sub_status_canceled
        is MembershipStatus.Granted -> Res.string.sub_status_granted
        is MembershipStatus.BillingIssue -> Res.string.sub_status_payment_issue
        is MembershipStatus.PauseScheduled -> Res.string.sub_status_pausing
    }
)

@Composable
private fun planName(period: PackagePeriod?): String = stringResource(
    when (period) {
        PackagePeriod.ANNUAL -> Res.string.sub_plan_yearly
        PackagePeriod.MONTHLY -> Res.string.sub_plan_monthly
        else -> Res.string.sub_plan_generic
    }
)
