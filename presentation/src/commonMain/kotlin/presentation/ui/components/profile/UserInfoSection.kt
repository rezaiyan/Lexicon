package presentation.ui.components.profile

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import presentation.model.ProfileUserUiModel
import theme.Theme

@Composable
fun UserInfoSection(
    userInfo: ProfileUserUiModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileAvatar(
            name = userInfo.name,
            email = userInfo.email
        )

        Spacer(modifier = Modifier.height(Theme.spacing.cardSpacingLarge))

        Text(
            text = userInfo.name.ifBlank { userInfo.email },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(Theme.spacing.extraSmall3))

        Text(
            text = userInfo.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ProfileAvatar(
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

/**
 * Returns 1-2 letter initials if the name looks like a real human name (letters + spaces only).
 * Falls back to the first letter of the email local part.
 * Returns null if no meaningful initials can be derived (shows icon instead).
 */
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

    // Name is absent or looks non-human (e.g. Apple private relay random string) —
    // fall back to the first letter of the email local part.
    val emailFirstChar = email.firstOrNull()
    if (emailFirstChar != null && emailFirstChar.isLetter()) {
        return emailFirstChar.uppercaseChar().toString()
    }

    return null
}
