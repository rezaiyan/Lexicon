package presentation.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.DialogIconState
import components.dialog.DialogProgressState
import components.dialog.LexiconDialogContent
import domain.tts.model.TtsModelInfo
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.tts_model_delete
import lexicon.resources.generated.resources.tts_model_delete_message
import lexicon.resources.generated.resources.tts_model_delete_title
import lexicon.resources.generated.resources.tts_models
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
    onDeleteModel: (String) -> Unit,
) {
    val downloadedModels = models.filter { it.isDownloaded }

    val summaryMessage = if (downloadedModels.isNotEmpty()) {
        stringResource(Res.string.tts_models_downloaded_count, downloadedModels.size) +
            " \u2022 " +
            stringResource(Res.string.tts_models_total_size, formatFileSize(totalSizeBytes))
    } else {
        stringResource(Res.string.tts_models_none_downloaded)
    }

    LexiconDialogContent(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        iconState = DialogIconState.Icon(
            imageVector = Icons.Default.RecordVoiceOver,
            tint = AppColors.settingsTtsIcon,
        ),
        title = stringResource(Res.string.tts_models),
        message = summaryMessage,
        progressState = if (isLoading) DialogProgressState.Circular else DialogProgressState.None,
        content = if (!isLoading && downloadedModels.isNotEmpty()) {
            {
                Column(modifier = Modifier.fillMaxWidth()) {
                    downloadedModels.forEach { model ->
                        TtsModelRow(
                            model = model,
                            onClick = { onDeleteModel(model.languageCode) },
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun TtsModelRow(
    model: TtsModelInfo,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.small))
            .clickable(onClick = onClick)
            .padding(horizontal = Theme.spacing.sm, vertical = Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.languageDisplayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = formatFileSize(model.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = stringResource(Res.string.tts_model_delete),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(Theme.dimensions.iconSize),
        )
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
