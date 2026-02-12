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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.kmp.models.CustomerInfo
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import vokab.resources.generated.resources.Res
import vokab.resources.generated.resources.billing_period_annual
import vokab.resources.generated.resources.billing_period_monthly
import vokab.resources.generated.resources.cancel_subscription
import vokab.resources.generated.resources.expires
import vokab.resources.generated.resources.manage_subscription
import vokab.resources.generated.resources.premium_label
import vokab.resources.generated.resources.premium_plan_name
import vokab.resources.generated.resources.subscription_active

@Composable
fun SubscriptionActiveContent(
    customerInfo: CustomerInfo?,
    formattedExpirationDate: String?,
    onManageSubscription: () -> Unit,
    onCancelSubscription: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.extraLarge4),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.medium)
    ) {
        SubscriptionStatusCard(
            customerInfo = customerInfo,
            formattedExpirationDate = formattedExpirationDate,
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
    customerInfo: CustomerInfo?,
    formattedExpirationDate: String?,
    onManageSubscription: () -> Unit
) {
    val activeEntitlement = customerInfo?.entitlements?.active?.values?.firstOrNull()
    val productIdentifier = activeEntitlement?.productIdentifier ?: ""
    val planName = getPlanNameFromProductIdentifier(productIdentifier)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.subscriptionRecommended.copy(alpha = 0.1f)
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

                    Spacer(modifier = Modifier.size(Theme.spacing.small))

                    Text(
                        text = planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    formattedExpirationDate?.let { formattedDate ->
                        Text(
                            text = stringResource(Res.string.expires, formattedDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    text = stringResource(Res.string.manage_subscription),
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

