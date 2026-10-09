package feature.study.ui.listening

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import components.GradientProgressBar
import components.sheet.SheetBadge
import components.sheet.SheetGroup
import components.sheet.SheetPrimaryButton
import components.sheet.SheetTextButton
import components.sheet.SheetTonalButton
import domain.listening.model.LanguageVoice
import domain.listening.model.VoiceStatus
import feature.study.listening.ListeningScreenState
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.listening_done
import lexicon.resources.generated.resources.listening_download_failed
import lexicon.resources.generated.resources.listening_download_voices
import lexicon.resources.generated.resources.listening_downloading
import lexicon.resources.generated.resources.listening_finished_title
import lexicon.resources.generated.resources.listening_listen_again
import lexicon.resources.generated.resources.listening_start_without
import lexicon.resources.generated.resources.listening_stat_minutes
import lexicon.resources.generated.resources.listening_stat_words
import lexicon.resources.generated.resources.listening_voice_missing
import lexicon.resources.generated.resources.listening_voice_ready
import lexicon.resources.generated.resources.listening_voice_unsupported
import lexicon.resources.generated.resources.listening_voices_body
import lexicon.resources.generated.resources.listening_voices_title
import org.jetbrains.compose.resources.stringResource
import theme.AppColors
import theme.Theme
import utils.Language

private const val MS_PER_MINUTE = 60_000L
private const val PERCENT = 100
private val HERO_SIZE = 88.dp
private val DOWNLOAD_PROGRESS_HEIGHT = 6.dp

// ---------------------------------------------------------------------------
// Pre-flight: voices
// ---------------------------------------------------------------------------

@Composable
internal fun VoicesContent(needs: ListeningScreenState.NeedsVoices, actions: ListeningActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = Theme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroIcon()
            Spacer(Modifier.height(Theme.spacing.lg))
            Text(
                text = stringResource(Res.string.listening_voices_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Theme.spacing.xs))
            Text(
                text = stringResource(Res.string.listening_voices_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Theme.spacing.md),
            )
            Spacer(Modifier.height(Theme.spacing.lg))
            SheetGroup {
                needs.check.voices.forEachIndexed { index, voice ->
                    VoiceRow(voice)
                    if (index < needs.check.voices.lastIndex) {
                        HorizontalDivider(
                            thickness = Theme.dimensions.hairlineThickness,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }

        VoicesFooter(needs, actions)
    }
}

@Composable
private fun VoicesFooter(needs: ListeningScreenState.NeedsVoices, actions: ListeningActions) {
    val download = needs.download
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Theme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (download != null) {
            Text(
                text = stringResource(
                    Res.string.listening_downloading,
                    Language.fromCode(download.languageCode).displayName,
                    download.position,
                    download.total,
                    (download.overallProgress * PERCENT).toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            GradientProgressBar(
                progress = download.overallProgress,
                gradientColors = listOf(MaterialTheme.colorScheme.primary, AppColors.tertiary),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                height = DOWNLOAD_PROGRESS_HEIGHT,
            )
        } else {
            if (needs.downloadFailed) {
                Text(
                    text = stringResource(Res.string.listening_download_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            SheetPrimaryButton(
                text = stringResource(Res.string.listening_download_voices),
                onClick = actions.onDownloadVoices,
                icon = Icons.Rounded.Download,
                modifier = Modifier.fillMaxWidth(),
            )
            SheetTextButton(
                text = stringResource(Res.string.listening_start_without),
                onClick = actions.onStartWithoutVoices,
            )
        }
    }
}

@Composable
private fun VoiceRow(voice: LanguageVoice) {
    val semantic = Theme.colors
    val (label, container, content) = when (voice.status) {
        VoiceStatus.READY -> Triple(
            Res.string.listening_voice_ready,
            semantic.successContainer,
            semantic.onSuccessContainer,
        )
        VoiceStatus.MISSING -> Triple(
            Res.string.listening_voice_missing,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VoiceStatus.UNSUPPORTED -> Triple(
            Res.string.listening_voice_unsupported,
            semantic.warningContainer,
            semantic.onWarningContainer,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Theme.spacing.md, vertical = Theme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Text(
            text = Language.fromCode(voice.languageCode).displayName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        SheetBadge(text = stringResource(label), containerColor = container, contentColor = content)
    }
}

// ---------------------------------------------------------------------------
// Finished
// ---------------------------------------------------------------------------

@Composable
internal fun FinishedContent(finished: ListeningScreenState.Finished, actions: ListeningActions) {
    val minutes = ((finished.durationMs + MS_PER_MINUTE / 2) / MS_PER_MINUTE).toInt().coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HeroIcon()
            Spacer(Modifier.height(Theme.spacing.lg))
            Text(
                text = stringResource(Res.string.listening_finished_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(Theme.spacing.lg))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
            ) {
                StatTile(
                    value = finished.wordsHeard.toString(),
                    label = stringResource(Res.string.listening_stat_words),
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    value = minutes.toString(),
                    label = stringResource(Res.string.listening_stat_minutes),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
        ) {
            SheetPrimaryButton(
                text = stringResource(Res.string.listening_done),
                onClick = actions.onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            SheetTonalButton(
                text = stringResource(Res.string.listening_listen_again),
                onClick = actions.onRestart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Theme.elevation.low),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Theme.spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroIcon() {
    Box(
        modifier = Modifier
            .size(HERO_SIZE)
            .background(Theme.gradients.primaryWash, CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = Theme.opacity.focus), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
        )
    }
}
