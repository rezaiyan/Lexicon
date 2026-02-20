package presentation.ui.components.profile

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

    Box(
        modifier = modifier
            .size(Theme.dimensions.profilePictureSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
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
