package feature.profile.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import feature.profile.model.ProfileSubscriptionStatus
import feature.profile.model.ProfileUserUiModel
import components.Pill
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.free_plan
import lexicon.resources.generated.resources.member_since
import lexicon.resources.generated.resources.premium_member
import lexicon.resources.generated.resources.profile_payment_issue
import lexicon.resources.generated.resources.profile_premium_paused
import lexicon.resources.generated.resources.profile_premium_until
import lexicon.resources.generated.resources.trial_active
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import domain.common.util.EpochDateFormatter
import lexicon.resources.generated.resources.credits_balance_refills
import lexicon.resources.generated.resources.credits_balance_title
import org.jetbrains.compose.resources.pluralStringResource

/**
 * Profile header card: compact avatar, display name, email, subscription status pill and
 * optional "member since" line. Display-only; editing and subscription live in the Account section.
 */
@Composable
fun UserInfoSection(
    userInfo: ProfileUserUiModel,
    memberSince: String?,
    subscriptionStatus: ProfileSubscriptionStatus,
    modifier: Modifier = Modifier,
    creditBalance: Int? = null,
    creditsRefillAtMillis: Long? = null,
) {
    val displayName = userInfo.displayAlias ?: userInfo.name.ifBlank { userInfo.email }
    val memberSinceText = memberSince?.let {
        stringResource(Res.string.member_since, remember(it) { formatMemberSince(it) })
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.extraLarge - Theme.spacing.xxs),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Theme.spacing.heroPadding),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactAvatar(name = userInfo.name, email = userInfo.email)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.textGap),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = userInfo.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SubscriptionStatusPill(status = subscriptionStatus)
                    if (memberSinceText != null) {
                        Text(
                            text = memberSinceText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (creditBalance != null) {
                HorizontalDivider(
                    thickness = Theme.dimensions.hairlineThickness,
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                CreditsRow(balance = creditBalance, refillsAtMillis = creditsRefillAtMillis)
            }
        }
    }
}

/**
 * "305 AI credits · Refills Nov 9, 2026" along the bottom of the header. Follows the shared balance,
 * so it changes as soon as a spend is known.
 */
@Composable
private fun CreditsRow(balance: Int, refillsAtMillis: Long?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.heroPadding, vertical = Theme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = pluralStringResource(Res.plurals.credits_balance_title, balance, balance),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        refillsAtMillis?.let {
            Text(
                text = stringResource(Res.string.credits_balance_refills, EpochDateFormatter.toMediumDate(it)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubscriptionStatusPill(status: ProfileSubscriptionStatus) {
    val colors = MaterialTheme.colorScheme
    val (text, color) = when (status) {
        ProfileSubscriptionStatus.Free -> stringResource(Res.string.free_plan) to colors.onSurfaceVariant
        ProfileSubscriptionStatus.Trial -> stringResource(Res.string.trial_active) to colors.tertiary
        ProfileSubscriptionStatus.Premium -> stringResource(Res.string.premium_member) to colors.primary
        is ProfileSubscriptionStatus.Canceled ->
            stringResource(Res.string.profile_premium_until, status.accessUntil) to Theme.colors.warning
        ProfileSubscriptionStatus.PaymentIssue -> stringResource(Res.string.profile_payment_issue) to colors.error
        is ProfileSubscriptionStatus.Paused ->
            stringResource(Res.string.profile_premium_paused, status.resumesOn) to colors.onSurfaceVariant
    }
    Pill(text = text, color = color, modifier = Modifier.padding(top = Theme.spacing.xxs))
}

@Composable
private fun CompactAvatar(
    name: String,
    email: String,
) {
    val initials = remember(name, email) { extractInitials(name, email) }
    Box(
        modifier = Modifier
            .size(Theme.dimensions.iconSizeMassive + Theme.spacing.xxs)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            initials != null -> Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            else -> Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Theme.dimensions.iconSizeXLarge),
            )
        }
    }
}

@Composable
internal fun ProfileAvatar(
    name: String,
    email: String,
    modifier: Modifier = Modifier
) {
    val initials = remember(name, email) { extractInitials(name, email) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Breathing scale animation on the glow
    val infiniteTransition = rememberInfiniteTransition(label = "avatarGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Radial glow behind avatar
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(glowScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Gradient border ring
        Box(
            modifier = Modifier
                .size(126.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.4f),
                            tertiaryColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Avatar circle with gradient background
            Box(
                modifier = Modifier
                    .size(Theme.dimensions.profilePictureSize)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.15f),
                                tertiaryColor.copy(alpha = 0.10f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (initials != null) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(Theme.dimensions.iconSizeMassive),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

private fun extractInitials(name: String, email: String): String? {
    val cleanName = name.trim()

    if (cleanName.isNotEmpty() && cleanName.all { it.isLetter() || it.isWhitespace() }) {
        val parts = cleanName.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) {
            return when {
                parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
                else -> parts[0].take(2).uppercase()
            }
        }
    }

    val emailFirstChar = email.firstOrNull()
    if (emailFirstChar != null && emailFirstChar.isLetter()) {
        return emailFirstChar.uppercaseChar().toString()
    }

    return null
}
