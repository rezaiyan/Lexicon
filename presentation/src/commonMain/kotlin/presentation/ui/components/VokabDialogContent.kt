package presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
fun VokabDialogContent(
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
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.small)
    ) {
        if (iconState !is DialogIconState.None || title != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (iconState) {
                    is DialogIconState.Icon -> {
                        Icon(
                            imageVector = iconState.imageVector,
                            contentDescription = null,
                            tint = iconState.tint ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(Theme.dimensions.iconSizeLarge)
                        )
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
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when (progressState) {
            is DialogProgressState.Circular -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(Theme.spacing.small))
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

        if (content != null) {
            content()
        }

        if (primaryButton != null || secondaryButton != null || negativeButton != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (negativeButton != null) {
                    val buttonColors = when (negativeButton.type) {
                        ButtonType.Default -> ButtonDefaults.buttonColors()
                        ButtonType.Error -> ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    }
                    Button(
                        onClick = negativeButton.onClick,
                        enabled = negativeButton.enabled,
                        colors = buttonColors
                    ) {
                        Text(
                            text = negativeButton.text,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }
                
                Row(horizontalArrangement = Arrangement.End) {
                    if (secondaryButton != null) {
                        TextButton(onClick = secondaryButton.onClick) {
                            Text(
                                text = secondaryButton.text,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(modifier = Modifier.width(Theme.spacing.extraSmall2))
                    }
                    if (primaryButton != null) {
                        val buttonColors = when (primaryButton.type) {
                            ButtonType.Default -> ButtonDefaults.buttonColors()
                            ButtonType.Error -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        }
                        Button(
                            onClick = primaryButton.onClick,
                            enabled = primaryButton.enabled,
                            colors = buttonColors
                        ) {
                            Text(
                                text = primaryButton.text,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VokabDialogContent(
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
    VokabDialogContent(
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
                ButtonState(
                    text = text,
                    onClick = onClick,
                    enabled = true,
                    type = primaryButtonType
                )
            }
        },
        secondaryButton = secondaryButtonText?.let { text ->
            secondaryButtonOnClick?.let { onClick ->
                ButtonState(
                    text = text,
                    onClick = onClick,
                    enabled = true,
                    type = ButtonType.Default
                )
            }
        }
    )
}
