package presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import theme.Theme

enum class ButtonType {
    Default,
    Error
}

sealed class DialogIconState {
    data object None : DialogIconState()
    data class Icon(val imageVector: ImageVector, val tint: Color? = null) : DialogIconState()
    data class CircularProgress(val tint: Color? = null) : DialogIconState()
}

sealed class DialogProgressState {
    data object None : DialogProgressState()
    data object Circular : DialogProgressState()
    data class Linear(val progress: Float? = null) : DialogProgressState()
}

data class ButtonState(
    val text: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val type: ButtonType = ButtonType.Default
)

@Composable
fun LexiconDialogContent(
    iconState: DialogIconState = DialogIconState.None,
    title: String? = null,
    message: String? = null,
    progressState: DialogProgressState = DialogProgressState.None,
    content: @Composable (() -> Unit)? = null,
    primaryButton: ButtonState? = null,
    secondaryButton: ButtonState? = null,
    negativeButton: ButtonState? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header: icon circle + title, vertically centered ──────────────────
        if (iconState !is DialogIconState.None || title != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
            ) {
                when (iconState) {
                    is DialogIconState.Icon -> {
                        val iconColor = iconState.tint ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconState.imageVector,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    is DialogIconState.CircularProgress -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Theme.dimensions.iconSizeLarge),
                            color = iconState.tint ?: MaterialTheme.colorScheme.primary
                        )
                    }

                    is DialogIconState.None -> {}
                }

                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(Theme.spacing.small))
        }

        // ── Message ───────────────────────────────────────────────────────────
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Theme.spacing.small))
        }

        // ── Progress ──────────────────────────────────────────────────────────
        when (progressState) {
            is DialogProgressState.Circular -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(Theme.spacing.small))
                    CircularProgressIndicator()
                }
            }

            is DialogProgressState.Linear -> {
                if (progressState.progress != null) {
                    LinearProgressIndicator(
                        progress = progressState.progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                }
            }

            is DialogProgressState.None -> {}
        }

        // ── Custom content slot ───────────────────────────────────────────────
        if (content != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }

        // ── Buttons ───────────────────────────────────────────────────────────
        if (primaryButton != null || secondaryButton != null || negativeButton != null) {
            Spacer(Modifier.height(Theme.spacing.small))

            if (negativeButton != null) {
                // 3-button layout:
                //   Row (end-aligned): secondary text | primary filled
                //   Full-width outlined error button for destructive action
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall2)
                ) {
                    if (primaryButton != null || secondaryButton != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (secondaryButton != null) {
                                TextButton(
                                    onClick = secondaryButton.onClick,
                                    enabled = secondaryButton.enabled
                                ) {
                                    Text(secondaryButton.text)
                                }
                                if (primaryButton != null) {
                                    Spacer(Modifier.width(Theme.spacing.extraSmall2))
                                }
                            }
                            if (primaryButton != null) {
                                val colors = when (primaryButton.type) {
                                    ButtonType.Default -> ButtonDefaults.buttonColors()
                                    ButtonType.Error -> ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                }
                                Button(
                                    onClick = primaryButton.onClick,
                                    enabled = primaryButton.enabled,
                                    colors = colors
                                ) {
                                    Text(primaryButton.text)
                                }
                            }
                        }
                    }

                    // Destructive action — full-width, outlined with error styling so it's
                    // visible but less aggressive than a solid filled error button.
                    OutlinedButton(
                        onClick = negativeButton.onClick,
                        enabled = negativeButton.enabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(negativeButton.text, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                // 2-button layout — end-aligned row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (secondaryButton != null) {
                        TextButton(
                            onClick = secondaryButton.onClick,
                            enabled = secondaryButton.enabled
                        ) {
                            Text(secondaryButton.text)
                        }
                        if (primaryButton != null) {
                            Spacer(Modifier.width(Theme.spacing.extraSmall2))
                        }
                    }
                    if (primaryButton != null) {
                        val colors = when (primaryButton.type) {
                            ButtonType.Default -> ButtonDefaults.buttonColors()
                            ButtonType.Error -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        }
                        Button(
                            onClick = primaryButton.onClick,
                            enabled = primaryButton.enabled,
                            colors = colors
                        ) {
                            Text(primaryButton.text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LexiconDialogContent(
    icon: ImageVector? = null,
    title: String? = null,
    message: String? = null,
    content: @Composable (() -> Unit)? = null,
    primaryButtonText: String? = null,
    primaryButtonOnClick: (() -> Unit)? = null,
    secondaryButtonText: String? = null,
    secondaryButtonOnClick: (() -> Unit)? = null,
    primaryButtonType: ButtonType = ButtonType.Default,
    iconTint: Color? = null
) {
    LexiconDialogContent(
        iconState = when {
            icon != null -> DialogIconState.Icon(icon, iconTint)
            else -> DialogIconState.None
        },
        title = title,
        message = message,
        progressState = DialogProgressState.None,
        content = content,
        primaryButton = primaryButtonText?.let { text ->
            primaryButtonOnClick?.let { onClick ->
                ButtonState(text = text, onClick = onClick, enabled = true, type = primaryButtonType)
            }
        },
        secondaryButton = secondaryButtonText?.let { text ->
            secondaryButtonOnClick?.let { onClick ->
                ButtonState(text = text, onClick = onClick, enabled = true, type = ButtonType.Default)
            }
        }
    )
}
