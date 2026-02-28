package presentation.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.most_popular_badge
import lexicon.resources.generated.resources.processing_ellipsis
import lexicon.resources.generated.resources.subscribe_now

@Immutable
data class SubscriptionPlan(
    val title: String,
    val billingPeriod: String,
    val description: String,
    val price: String,
    val accentColor: Color
)

@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    isRecommended: Boolean,
    modifier: Modifier = Modifier,
    isPurchasing: Boolean = false,
    onClick: () -> Unit = {}
) {
    val cardShape = RoundedCornerShape(Theme.dimensions.cardCornerRadius)
    val borderColor = if (isRecommended) {
        plan.accentColor.copy(alpha = 0.3f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isRecommended) {
                        Modifier.border(
                            width = 2.dp,
                            color = borderColor,
                            shape = cardShape
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
            ) {
                if (isRecommended) {
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    plan.accentColor,
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(
                                    horizontal = Theme.spacing.extraSmall,
                                    vertical = Theme.spacing.extraSmall2
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(end = Theme.spacing.extraSmall3)
                                    .size(12.dp)
                            )
                            Text(
                                text = stringResource(Res.string.most_popular_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = plan.title,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = Theme.spacing.xs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = plan.price,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = plan.accentColor,
                            textAlign = TextAlign.End,
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 14.sp,
                                maxFontSize = 28.sp,
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = plan.billingPeriod,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall))

                        Text(
                            text = plan.description,
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Theme.spacing.small))

                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPurchasing,
                    shape = RoundedCornerShape(Theme.spacing.extraSmall2),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = plan.accentColor,
                        contentColor = Color.White
                    )
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(Theme.spacing.small))
                    }
                    Text(
                        text = if (isPurchasing) stringResource(Res.string.processing_ellipsis) else stringResource(
                            Res.string.subscribe_now
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isRecommended) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(Theme.dimensions.iconSizeXLarge)
                    .background(plan.accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

