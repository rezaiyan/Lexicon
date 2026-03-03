package presentation.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.subscription.model.SubscriptionCustomerInfo
import kotlin.time.Clock
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.billing_period_annual
import lexicon.resources.generated.resources.billing_period_monthly
import lexicon.resources.generated.resources.cancel_subscription
import lexicon.resources.generated.resources.expires
import lexicon.resources.generated.resources.manage_subscription
import lexicon.resources.generated.resources.premium_label
import lexicon.resources.generated.resources.premium_plan_name
import lexicon.resources.generated.resources.subscription_access_until
import lexicon.resources.generated.resources.subscription_active
import lexicon.resources.generated.resources.subscription_day_remaining
import lexicon.resources.generated.resources.subscription_days_remaining
import lexicon.resources.generated.resources.subscription_expires_today
import lexicon.resources.generated.resources.subscription_cancelled_access_note
import lexicon.resources.generated.resources.subscription_cancelling
import lexicon.resources.generated.resources.subscription_resubscribe

@Composable
fun SubscriptionActiveContent(
    customerInfo: SubscriptionCustomerInfo?,
    formattedExpirationDate: String?,
    willRenew: Boolean = true,
    onManageSubscription: () -> Unit,
    onCancelSubscription: (() -> Unit)? = null
) {
    val expirationDateMillis = customerInfo?.activeEntitlements?.values?.firstOrNull()?.expirationDateMillis
    val isCancelled = !willRenew && expirationDateMillis != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.extraLarge4),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
    ) {
        SubscriptionStatusCard(
            customerInfo = customerInfo,
            formattedExpirationDate = formattedExpirationDate,
            isCancelled = isCancelled,
            onManageSubscription = onManageSubscription
        )

        PremiumFeaturesGrid()

        if (onCancelSubscription != null) {
            OutlinedButton(
                onClick = onCancelSubscription,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.cancel_subscription))
            }
        }
    }
}

@Composable
private fun SubscriptionStatusCard(
    customerInfo: SubscriptionCustomerInfo?,
    formattedExpirationDate: String?,
    isCancelled: Boolean = false,
    onManageSubscription: () -> Unit
) {
    val activeEntitlement = customerInfo?.activeEntitlements?.values?.firstOrNull()
    val productIdentifier = activeEntitlement?.productIdentifier ?: ""
    val planName = getPlanNameFromProductIdentifier(productIdentifier)

    val warningColor = Theme.colors.warning

    val daysRemaining = if (isCancelled) {
        val expirationMillis = activeEntitlement?.expirationDateMillis
        remember(expirationMillis) {
            expirationMillis?.let {
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val diffDays = (it - nowMillis) / (24 * 60 * 60 * 1000L)
                diffDays.coerceAtLeast(0L)
            }
        }
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCancelled) {
                Theme.colors.warningContainer.copy(alpha = 0.3f)
            } else {
                AppColors.subscriptionRecommended.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Theme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.small)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(Theme.spacing.extraSmall),
                            color = AppColors.subscriptionRecommended.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = stringResource(Res.string.premium_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.subscriptionRecommended,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(
                                    horizontal = Theme.spacing.small,
                                    vertical = Theme.spacing.extraSmall
                                )
                            )
                        }
                        if (isCancelled) {
                            Surface(
                                shape = RoundedCornerShape(Theme.spacing.extraSmall),
                                color = warningColor.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3),
                                    modifier = Modifier.padding(
                                        horizontal = Theme.spacing.small,
                                        vertical = Theme.spacing.extraSmall
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = warningColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.subscription_cancelling),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = warningColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(Theme.spacing.extraSmall),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall3),
                                    modifier = Modifier.padding(
                                        horizontal = Theme.spacing.small,
                                        vertical = Theme.spacing.extraSmall
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = stringResource(Res.string.subscription_active),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(Theme.spacing.small))

                    Text(
                        text = planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    formattedExpirationDate?.let { formattedDate ->
                        if (isCancelled) {
                            Text(
                                text = stringResource(Res.string.subscription_access_until, formattedDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = warningColor
                            )
                            daysRemaining?.let { days ->
                                Text(
                                    text = when {
                                        days == 0L -> stringResource(Res.string.subscription_expires_today)
                                        days == 1L -> stringResource(Res.string.subscription_day_remaining)
                                        else -> stringResource(Res.string.subscription_days_remaining, days.toInt())
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = warningColor
                                )
                            }
                        } else {
                            Text(
                                text = stringResource(Res.string.expires, formattedDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(Theme.dimensions.iconSizeXLarge)
                        .background(
                            AppColors.subscriptionRecommended.copy(alpha = 0.2f),
                            RoundedCornerShape(Theme.spacing.medium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AppColors.subscriptionRecommended,
                        modifier = Modifier.size(Theme.dimensions.iconSizeMedium)
                    )
                }
            }

            if (isCancelled) {
                Text(
                    text = stringResource(Res.string.subscription_cancelled_access_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onManageSubscription,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AppColors.subscriptionRecommended,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(Theme.spacing.extraSmall2)
            ) {
                Text(
                    text = if (isCancelled) {
                        stringResource(Res.string.subscription_resubscribe)
                    } else {
                        stringResource(Res.string.manage_subscription)
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Extracts a user-friendly plan name from a product identifier.
 * 
 * iOS format: com.alirezaiyan.vokab.Monthly or com.alirezaiyan.vokab.Annual
 * Android format: com.alirezaiyan.vokab.premium.p1m or com.alirezaiyan.vokab.premium.p1y
 */
@Composable
private fun getPlanNameFromProductIdentifier(productIdentifier: String): String {
    if (productIdentifier.isBlank()) {
        return stringResource(Res.string.premium_plan_name)
    }
    
    val identifierLower = productIdentifier.lowercase()
    
    return when {
        isMonthlyPlan(identifierLower) -> stringResource(Res.string.billing_period_monthly)
        isAnnualPlan(identifierLower) -> stringResource(Res.string.billing_period_annual)
        else -> stringResource(Res.string.premium_plan_name)
    }
}

private fun isMonthlyPlan(identifier: String): Boolean {
    return identifier.contains("monthly") ||
           identifier.contains("p1m") ||
           (identifier.contains("month") && 
            !identifier.contains("annual") && 
            !identifier.contains("p1y"))
}

private fun isAnnualPlan(identifier: String): Boolean {
    return identifier.contains("annual") ||
           identifier.contains("p1y") ||
           identifier.contains("year")
}

