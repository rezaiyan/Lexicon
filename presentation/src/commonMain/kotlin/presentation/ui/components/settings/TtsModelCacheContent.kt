package presentation.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.DialogIconState
import components.dialog.LexiconDialogContent
import domain.tts.model.TtsModelInfo
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.tts_model_delete
import lexicon.resources.generated.resources.tts_model_delete_message
import lexicon.resources.generated.resources.tts_model_delete_title
import lexicon.resources.generated.resources.tts_model_downloaded
import lexicon.resources.generated.resources.tts_model_not_downloaded
import lexicon.resources.generated.resources.tts_models
import lexicon.resources.generated.resources.tts_models_delete_all
import lexicon.resources.generated.resources.tts_models_delete_all_message
import lexicon.resources.generated.resources.tts_models_delete_all_title
import lexicon.resources.generated.resources.tts_models_downloaded_count
import lexicon.resources.generated.resources.tts_models_none_downloaded
import lexicon.resources.generated.resources.tts_models_total_size
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun TtsModelCacheContent(
    models: List<TtsModelInfo>,
    isLoading: Boolean,
    totalSizeBytes: Long,
    downloadedCount: Int,
    onDeleteModel: (String) -> Unit,
    onDeleteAllModels: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = Theme.spacing.lg, vertical = Theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header icon
        Box(
            modifier = Modifier
                .size(Theme.dimensions.buttonHeight)
                .clip(CircleShape)
                .background(AppColors.settingsTtsIcon.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = AppColors.settingsTtsIcon,
                modifier = Modifier.size(Theme.dimensions.iconSizeLarge),
            )
        }

        Spacer(Modifier.height(Theme.spacing.small))

        Text(
            text = stringResource(Res.string.tts_models),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Theme.spacing.xs))

        // Summary
        if (downloadedCount > 0) {
            Text(
                text = stringResource(
                    Res.string.tts_models_downloaded_count,
                    downloadedCount,
                ) + " \u2022 " + stringResource(
                    Res.string.tts_models_total_size,
                    formatFileSize(totalSizeBytes),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = stringResource(Res.string.tts_models_none_downloaded),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(Theme.spacing.md))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(vertical = Theme.spacing.lg),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
            ) {
                models.forEach { model ->
                    TtsModelRow(
                        model = model,
                        onDelete = { onDeleteModel(model.languageCode) },
                    )
                }
            }

            if (downloadedCount > 0) {
                Spacer(Modifier.height(Theme.spacing.sm))

                TextButton(
                    onClick = onDeleteAllModels,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(Theme.dimensions.iconSize),
                    )
                    Spacer(Modifier.width(Theme.spacing.xs))
                    Text(
                        text = stringResource(Res.string.tts_models_delete_all),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        Spacer(Modifier.height(Theme.spacing.sm))
    }
}

@Composable
private fun TtsModelRow(
    model: TtsModelInfo,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Theme.shapes.small),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.languageDisplayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (model.isDownloaded) {
                    Text(
                        text = stringResource(Res.string.tts_model_downloaded) +
                            " \u2022 " + formatFileSize(model.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.tts_model_not_downloaded),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (model.isDownloaded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AppColors.secondary,
                        modifier = Modifier.size(Theme.dimensions.iconSize),
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(Res.string.tts_model_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TtsModelDeleteConfirmationContent(
    languageDisplayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LexiconDialogContent(
        iconState = DialogIconState.Icon(
            imageVector = Icons.Default.Warning,
            tint = MaterialTheme.colorScheme.error,
        ),
        title = stringResource(Res.string.tts_model_delete_title),
        content = {
            Text(
                text = stringResource(Res.string.tts_model_delete_message, languageDisplayName),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.tts_model_delete),
            onClick = onConfirm,
            type = ButtonType.Error,
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss,
        ),
    )
}

@Composable
fun TtsModelDeleteAllConfirmationContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LexiconDialogContent(
        iconState = DialogIconState.Icon(
            imageVector = Icons.Default.Warning,
            tint = MaterialTheme.colorScheme.error,
        ),
        title = stringResource(Res.string.tts_models_delete_all_title),
        content = {
            Text(
                text = stringResource(Res.string.tts_models_delete_all_message),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        primaryButton = ButtonState(
            text = stringResource(Res.string.tts_models_delete_all),
            onClick = onConfirm,
            type = ButtonType.Error,
        ),
        secondaryButton = ButtonState(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss,
        ),
    )
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "${roundToOneDecimal(gb)} GB"
        mb >= 1.0 -> "${roundToOneDecimal(mb)} MB"
        kb >= 1.0 -> "${roundToOneDecimal(kb)} KB"
        else -> "$bytes B"
    }
}

private fun roundToOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10) / 10.0
    val whole = rounded.toLong()
    val decimal = kotlin.math.round((rounded - whole) * 10).toInt()
    return "$whole.$decimal"
}
