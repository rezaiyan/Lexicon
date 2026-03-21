package presentation.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.dialog.ButtonState
import components.dialog.ButtonType
import components.dialog.DialogIconState
import components.dialog.DialogProgressState
import components.dialog.LexiconDialogContent
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsSettings
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.tts_all_languages
import lexicon.resources.generated.resources.tts_model_delete
import lexicon.resources.generated.resources.tts_model_delete_message
import lexicon.resources.generated.resources.tts_model_delete_title
import lexicon.resources.generated.resources.tts_model_download
import lexicon.resources.generated.resources.tts_model_downloading
import lexicon.resources.generated.resources.tts_models
import lexicon.resources.generated.resources.tts_models_downloaded_count
import lexicon.resources.generated.resources.tts_models_none_downloaded
import lexicon.resources.generated.resources.tts_models_total_size
import lexicon.resources.generated.resources.tts_playback_settings
import lexicon.resources.generated.resources.tts_playback_speed
import lexicon.resources.generated.resources.tts_playback_speed_value
import lexicon.resources.generated.resources.tts_voice_selection
import lexicon.resources.generated.resources.tts_voice_speaker
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme

@Composable
fun TtsVoiceManagerContent(
    models: List<TtsModelInfo>,
    isLoading: Boolean,
    totalSizeBytes: Long,
    downloadProgress: Map<String, Float>,
    ttsSettings: TtsSettings,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onVoiceSelected: (String, Int) -> Unit,
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
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TtsSpeedSection(
                    currentRate = ttsSettings.speechRate,
                    onRateChanged = onSpeechRateChanged,
                )

                if (!isLoading && models.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Theme.spacing.sm))

                    Text(
                        text = stringResource(Res.string.tts_all_languages),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Theme.spacing.xs),
                    )

                    models.forEach { model ->
                        TtsLanguageRow(
                            model = model,
                            progress = downloadProgress[model.languageCode],
                            onDownload = { onDownloadModel(model.languageCode) },
                            onDelete = { onDeleteModel(model.languageCode) },
                            onVoiceSelected = { speakerId -> onVoiceSelected(model.languageCode, speakerId) },
                        )
                        Spacer(modifier = Modifier.height(Theme.spacing.xs))
                    }
                }
            }
        },
    )
}

@Composable
private fun TtsSpeedSection(
    currentRate: Float,
    onRateChanged: (Float) -> Unit,
) {
    var sliderValue by remember(currentRate) { mutableFloatStateOf(currentRate) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.tts_playback_settings),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.xs),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Theme.dimensions.iconSize),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.tts_playback_speed),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(Res.string.tts_playback_speed_value, formatSpeed(sliderValue)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onRateChanged(sliderValue) },
                    valueRange = TtsSettings.MIN_SPEECH_RATE..TtsSettings.MAX_SPEECH_RATE,
                    steps = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TtsLanguageRow(
    model: TtsModelInfo,
    progress: Float?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onVoiceSelected: (Int) -> Unit,
) {
    val isDownloading = progress != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.shapes.medium))
            .padding(horizontal = Theme.spacing.xs, vertical = Theme.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.languageDisplayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                when {
                    isDownloading -> Text(
                        text = stringResource(Res.string.tts_model_downloading),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    model.isDownloaded -> Text(
                        text = formatFileSize(model.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    else -> Text(
                        text = "Not downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            when {
                isDownloading -> {
                    // No action button during download
                }
                model.isDownloaded -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AppColors.settingsTtsIcon,
                            modifier = Modifier.size(Theme.dimensions.iconSize),
                        )
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = stringResource(Res.string.tts_model_delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(Theme.dimensions.iconSize),
                            )
                        }
                    }
                }
                else -> {
                    OutlinedButton(
                        onClick = onDownload,
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = stringResource(Res.string.tts_model_download),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        // Download progress bar
        AnimatedVisibility(
            visible = isDownloading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = Theme.spacing.xs)) {
                LinearProgressIndicator(
                    progress = { progress ?: 0f },
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.settingsTtsIcon,
                )
                Text(
                    text = "${((progress ?: 0f) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    textAlign = TextAlign.End,
                )
            }
        }

        // Voice selector — only shown when model is downloaded and has multiple speakers
        AnimatedVisibility(
            visible = model.isDownloaded && model.numSpeakers > 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            TtsVoiceSelectorRow(
                numSpeakers = model.numSpeakers,
                selectedSpeakerId = model.selectedSpeakerId,
                onVoiceSelected = onVoiceSelected,
            )
        }
    }
}

@Composable
private fun TtsVoiceSelectorRow(
    numSpeakers: Int,
    selectedSpeakerId: Int,
    onVoiceSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Theme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.tts_voice_selection),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Theme.spacing.xs),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            (0 until numSpeakers).forEach { speakerId ->
                FilterChip(
                    selected = speakerId == selectedSpeakerId,
                    onClick = { onVoiceSelected(speakerId) },
                    label = {
                        Text(
                            text = stringResource(Res.string.tts_voice_speaker, speakerId + 1),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppColors.settingsTtsIcon.copy(alpha = 0.15f),
                        selectedLabelColor = AppColors.settingsTtsIcon,
                    ),
                )
            }
        }
    }
}

@Composable
fun TtsDeleteConfirmationContent(
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

private fun formatSpeed(speed: Float): String {
    val rounded = kotlin.math.round(speed * 10) / 10.0
    val whole = rounded.toLong()
    val decimal = kotlin.math.round((rounded - whole) * 10).toInt()
    return "$whole.$decimal"
}
