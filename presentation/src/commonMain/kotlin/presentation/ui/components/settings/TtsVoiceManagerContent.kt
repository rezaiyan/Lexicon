package presentation.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import components.sheet.ConfirmSheetContent
import components.sheet.ConfirmTone
import components.sheet.IconTile
import components.sheet.SheetBadge
import components.sheet.SheetGroup
import components.sheet.SheetPage
import components.sheet.SheetSectionLabel
import domain.tts.model.TtsModelInfo
import domain.tts.model.TtsSettings
import domain.tts.model.TtsVoice
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.tts_all_languages
import lexicon.resources.generated.resources.tts_expressiveness
import lexicon.resources.generated.resources.tts_expressiveness_balanced
import lexicon.resources.generated.resources.tts_expressiveness_expressive
import lexicon.resources.generated.resources.tts_expressiveness_hint
import lexicon.resources.generated.resources.tts_expressiveness_steady
import lexicon.resources.generated.resources.tts_model_delete
import lexicon.resources.generated.resources.tts_model_delete_message
import lexicon.resources.generated.resources.tts_model_delete_title
import lexicon.resources.generated.resources.tts_model_download
import lexicon.resources.generated.resources.tts_model_downloaded
import lexicon.resources.generated.resources.tts_model_downloading
import lexicon.resources.generated.resources.tts_model_not_downloaded
import lexicon.resources.generated.resources.tts_models
import lexicon.resources.generated.resources.tts_models_downloaded_count
import lexicon.resources.generated.resources.tts_models_none_downloaded
import lexicon.resources.generated.resources.tts_models_total_size
import lexicon.resources.generated.resources.tts_playback_speed
import lexicon.resources.generated.resources.tts_playback_speed_value
import lexicon.resources.generated.resources.tts_voice_preview
import lexicon.resources.generated.resources.tts_voice_preview_stop
import lexicon.resources.generated.resources.tts_voice_selection
import lexicon.resources.generated.resources.tts_voice_speaker
import lexicon.resources.generated.resources.tts_voice_switch_hint
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.LexiconFormatters

private const val SpeedSteps = 5
private const val ExpressivenessSteps = 7
private const val PercentScale = 100
private const val SteadyBelow = 0.45f
private const val ExpressiveAbove = 0.8f

/** Offline voices: playback speed, then downloaded / downloading voices, then everything else. */
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
    previewLanguage: String? = null,
    onExpressivenessChanged: (Float) -> Unit = {},
    onVoiceModelSelected: (String, String) -> Unit = { _, _ -> },
    onPreview: (String) -> Unit = {},
    onStopPreview: () -> Unit = {},
    onClose: (() -> Unit)? = null,
) {
    val (installed, available) = remember(models, downloadProgress) {
        models.partition { it.isDownloaded || downloadProgress.containsKey(it.languageCode) }
    }
    val summary = if (models.any { it.isDownloaded }) {
        stringResource(Res.string.tts_models_downloaded_count, models.count { it.isDownloaded }) +
            " • " +
            stringResource(Res.string.tts_models_total_size, LexiconFormatters.fileSize(totalSizeBytes))
    } else {
        stringResource(Res.string.tts_models_none_downloaded)
    }

    SheetPage(
        title = stringResource(Res.string.tts_models),
        subtitle = summary,
        onClose = onClose,
    ) {
        SpeedCard(currentRate = ttsSettings.speechRate, onRateChanged = onSpeechRateChanged)
        ExpressivenessCard(current = ttsSettings.expressiveness, onChanged = onExpressivenessChanged)

        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            VoiceSection(
                label = stringResource(Res.string.tts_model_downloaded),
                models = installed,
                downloadProgress = downloadProgress,
                onDownloadModel = onDownloadModel,
                onDeleteModel = onDeleteModel,
                onVoiceSelected = onVoiceSelected,
                previewLanguage = previewLanguage,
                onVoiceModelSelected = onVoiceModelSelected,
                onPreview = onPreview,
                onStopPreview = onStopPreview,
            )
            VoiceSection(
                label = stringResource(Res.string.tts_all_languages),
                models = available,
                downloadProgress = downloadProgress,
                onDownloadModel = onDownloadModel,
                onDeleteModel = onDeleteModel,
                onVoiceSelected = onVoiceSelected,
                previewLanguage = previewLanguage,
                onVoiceModelSelected = onVoiceModelSelected,
                onPreview = onPreview,
                onStopPreview = onStopPreview,
            )
        }
    }
}

@Composable
private fun VoiceSection(
    label: String,
    models: List<TtsModelInfo>,
    downloadProgress: Map<String, Float>,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onVoiceSelected: (String, Int) -> Unit,
    previewLanguage: String?,
    onVoiceModelSelected: (String, String) -> Unit,
    onPreview: (String) -> Unit,
    onStopPreview: () -> Unit,
) {
    if (models.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)) {
        SheetSectionLabel(label)
        SheetGroup {
            models.forEachIndexed { index, model ->
                VoiceRow(
                    model = model,
                    progress = downloadProgress[model.languageCode],
                    showDivider = index < models.lastIndex,
                    onDownload = { onDownloadModel(model.languageCode) },
                    onDelete = { onDeleteModel(model.languageCode) },
                    onVoiceSelected = { speakerId -> onVoiceSelected(model.languageCode, speakerId) },
                    isPreviewing = previewLanguage == model.languageCode,
                    onVoiceModelSelected = { voiceId -> onVoiceModelSelected(model.languageCode, voiceId) },
                    onPreview = { onPreview(model.languageCode) },
                    onStopPreview = onStopPreview,
                )
            }
        }
    }
}

@Composable
private fun SpeedCard(currentRate: Float, onRateChanged: (Float) -> Unit) {
    var sliderValue by remember(currentRate) { mutableFloatStateOf(currentRate) }
    SheetGroup {
        Column(
            modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                IconTile(icon = Icons.Default.Speed, tinted = true)
                Text(
                    text = stringResource(Res.string.tts_playback_speed),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                SheetBadge(
                    text = stringResource(Res.string.tts_playback_speed_value, LexiconFormatters.speed(sliderValue)),
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onRateChanged(sliderValue) },
                valueRange = TtsSettings.MIN_SPEECH_RATE..TtsSettings.MAX_SPEECH_RATE,
                steps = SpeedSteps,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ExpressivenessCard(current: Float, onChanged: (Float) -> Unit) {
    var sliderValue by remember(current) { mutableFloatStateOf(current) }
    val label = when {
        sliderValue < SteadyBelow -> stringResource(Res.string.tts_expressiveness_steady)
        sliderValue > ExpressiveAbove -> stringResource(Res.string.tts_expressiveness_expressive)
        else -> stringResource(Res.string.tts_expressiveness_balanced)
    }
    SheetGroup {
        Column(
            modifier = Modifier.padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                IconTile(icon = Icons.Default.GraphicEq, tinted = true)
                Text(
                    text = stringResource(Res.string.tts_expressiveness),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                SheetBadge(
                    text = label,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus),
                    contentColor = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onChanged(sliderValue) },
                valueRange = TtsSettings.MIN_EXPRESSIVENESS..TtsSettings.MAX_EXPRESSIVENESS,
                steps = ExpressivenessSteps,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(Res.string.tts_expressiveness_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VoiceRow(
    model: TtsModelInfo,
    progress: Float?,
    showDivider: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onVoiceSelected: (Int) -> Unit,
    isPreviewing: Boolean,
    onVoiceModelSelected: (String) -> Unit,
    onPreview: () -> Unit,
    onStopPreview: () -> Unit,
) {
    val isDownloading = progress != null
    val selectedVoice = model.voices.firstOrNull { it.id == model.selectedVoiceId }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Theme.dimensions.touchTarget + Theme.spacing.md)
                .padding(start = Theme.spacing.md, end = Theme.spacing.xs)
                .padding(vertical = Theme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
                Text(
                    text = model.languageDisplayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = when {
                        isDownloading -> stringResource(Res.string.tts_model_downloading) +
                            " ${((progress ?: 0f) * PercentScale).toInt()}%"
                        model.isDownloaded -> listOfNotNull(
                            selectedVoice?.takeIf { model.voices.size > 1 }?.label(),
                            LexiconFormatters.fileSize(model.sizeBytes),
                        ).joinToString(" • ")
                        else -> stringResource(Res.string.tts_model_not_downloaded)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDownloading) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                isDownloading -> DownloadRing(progress = progress ?: 0f)
                model.isDownloaded -> Row {
                    PreviewButton(isPreviewing = isPreviewing, onPreview = onPreview, onStop = onStopPreview)
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(Res.string.tts_model_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> GetButton(onClick = onDownload)
            }
        }
        AnimatedVisibility(
            visible = (model.isDownloaded || isDownloading) && model.voices.size > 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            VoiceModelSelectorRow(
                voices = model.voices,
                selectedVoiceId = model.selectedVoiceId,
                enabled = !isDownloading,
                onVoiceSelected = onVoiceModelSelected,
            )
        }
        AnimatedVisibility(
            visible = model.isDownloaded && model.numSpeakers > 1,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            VoiceSelectorRow(
                numSpeakers = model.numSpeakers,
                selectedSpeakerId = model.selectedSpeakerId,
                onVoiceSelected = onVoiceSelected,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = Theme.opacity.overlay),
                modifier = Modifier.padding(start = Theme.spacing.md),
            )
        }
    }
}

@Composable
private fun DownloadRing(progress: Float) {
    Box(modifier = Modifier.size(Theme.dimensions.touchTarget), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(Theme.dimensions.iconSizeLarge),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeWidth = Theme.spacing.xxxs,
        )
    }
}

@Composable
private fun GetButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(Theme.shapes.pill),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
        modifier = Modifier.heightIn(min = Theme.dimensions.touchTargetSmall),
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
        Text(
            text = stringResource(Res.string.tts_model_download),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = Theme.spacing.xxs + Theme.spacing.xxxs),
        )
    }
}

@Composable
private fun PreviewButton(isPreviewing: Boolean, onPreview: () -> Unit, onStop: () -> Unit) {
    IconButton(onClick = if (isPreviewing) onStop else onPreview) {
        Icon(
            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = stringResource(
                if (isPreviewing) Res.string.tts_voice_preview_stop else Res.string.tts_voice_preview
            ),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun TtsVoice.label(): String = region?.let { "$name · $it" } ?: name

/** Voice models for one language; only one is on disk, so picking another re-downloads. */
@Composable
private fun VoiceModelSelectorRow(
    voices: List<TtsVoice>,
    selectedVoiceId: String?,
    enabled: Boolean,
    onVoiceSelected: (String) -> Unit,
) {
    val voiceLabel = stringResource(Res.string.tts_voice_selection)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Theme.spacing.sm)
            .semantics { contentDescription = voiceLabel },
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Theme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            voices.forEach { voice ->
                FilterChip(
                    selected = voice.id == selectedVoiceId,
                    onClick = { onVoiceSelected(voice.id) },
                    enabled = enabled,
                    label = { Text(voice.label()) },
                )
            }
        }
        Text(
            text = stringResource(Res.string.tts_voice_switch_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Theme.spacing.md),
        )
    }
}

@Composable
private fun VoiceSelectorRow(
    numSpeakers: Int,
    selectedSpeakerId: Int,
    onVoiceSelected: (Int) -> Unit,
) {
    val voiceLabel = stringResource(Res.string.tts_voice_selection)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Theme.spacing.md, end = Theme.spacing.md, bottom = Theme.spacing.sm)
            .semantics { contentDescription = voiceLabel },
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        for (speakerId in 0 until numSpeakers) {
            FilterChip(
                selected = speakerId == selectedSpeakerId,
                onClick = { onVoiceSelected(speakerId) },
                label = { Text(stringResource(Res.string.tts_voice_speaker, speakerId + 1)) },
            )
        }
    }
}

@Composable
fun TtsDeleteConfirmationContent(
    languageDisplayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmSheetContent(
        icon = Icons.Default.DeleteOutline,
        title = stringResource(Res.string.tts_model_delete_title),
        message = stringResource(Res.string.tts_model_delete_message, languageDisplayName),
        confirmText = stringResource(Res.string.tts_model_delete),
        onConfirm = onConfirm,
        dismissText = stringResource(Res.string.cancel),
        onDismiss = onDismiss,
        tone = ConfirmTone.Danger,
    )
}
