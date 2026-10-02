package feature.study.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.tts.model.TtsState
import domain.word.model.Word
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.consolidating
import lexicon.resources.generated.resources.edit
import lexicon.resources.generated.resources.familiar
import lexicon.resources.generated.resources.learning
import lexicon.resources.generated.resources.mastered
import lexicon.resources.generated.resources.mature
import lexicon.resources.generated.resources.new
import lexicon.resources.generated.resources.repeat_pronunciation
import lexicon.resources.generated.resources.tap_card_to_reveal
import lexicon.resources.generated.resources.unknown
import lexicon.resources.generated.resources.young
import org.jetbrains.compose.resources.stringResource
import theme.Theme

private val CardRadius = 28.dp
private const val FlipDurationMs = 420

/**
 * Review flash card. Fills the size it is given; the header (level chip, language, edit) stays at the top,
 * the word sits centred, and the front carries a "tap to reveal" hint at the bottom. The card flips in 3D;
 * the back shows the original word small, then the translation large with its description.
 */
@Composable
fun FlashCard(
    word: Word,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
    dragFeedbackY: Float = 0f,
    ttsState: TtsState = TtsState.Idle,
    onSpeakClick: (text: String, langCode: String) -> Unit = { _, _ -> },
    onEdit: (() -> Unit)? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = FlipDurationMs, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )
    val showingBack = rotation > 90f
    // Fades the face content in after the edge-on moment of the flip so text never appears mirrored.
    val faceAlpha = (kotlin.math.abs(rotation - 90f) / 90f).coerceIn(0f, 1f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    val colors = MaterialTheme.colorScheme
    val isDark = colors.surface.luminance() < 0.5f
    val cardColor = if (isDark) colors.surfaceContainerHigh else colors.surfaceContainerLowest

    Card(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 14f * density
                scaleX = cardScale
                scaleY = cardScale
                translationY = dragFeedbackY
            },
        shape = RoundedCornerShape(CardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = Theme.opacity.overlay)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = if (isFlipped) {
                        "${word.translation}. Tap to show original word"
                    } else {
                        "${word.originalWord}. Tap to reveal translation"
                    }
                }
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClickLabel = if (isFlipped) "Show original word" else "Reveal translation",
                    onClick = onFlip,
                )
                // Un-mirror the back face and fade content around the edge-on moment.
                .graphicsLayer {
                    rotationY = if (showingBack) 180f else 0f
                    alpha = faceAlpha
                },
        ) {
            CardHeader(
                level = word.level,
                languageLabel = word.targetLanguage.displayName,
                onEdit = onEdit,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Theme.spacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                if (showingBack) {
                    BackFace(word = word, ttsState = ttsState, onSpeakClick = onSpeakClick)
                } else {
                    FrontFace(word = word, ttsState = ttsState, onSpeakClick = onSpeakClick)
                }
            }
            if (showingBack) {
                Spacer(Modifier.height(Theme.spacing.lg))
            } else {
                RevealHint()
            }
        }
    }
}

@Composable
private fun CardHeader(level: Int, languageLabel: String, onEdit: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = Theme.spacing.lg, end = Theme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        MasteryLevelBadge(level = level)
        Text(
            text = languageLabel.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
                )
            }
        }
    }
}

@Composable
private fun FrontFace(word: Word, ttsState: TtsState, onSpeakClick: (String, String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.lg),
    ) {
        HeadlineWord(text = word.originalWord, maxSize = 40)
        SpeakerButton(
            ttsState = ttsState,
            size = 52.dp,
            onClick = { onSpeakClick(word.originalWord, word.targetLanguage.code) },
        )
    }
}

@Composable
private fun BackFace(word: Word, ttsState: TtsState, onSpeakClick: (String, String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
        ) {
            Text(
                text = word.originalWord,
                style = MaterialTheme.typography.titleMedium.copy(hyphens = Hyphens.Auto),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            SpeakerButton(
                ttsState = ttsState,
                size = 36.dp,
                onClick = { onSpeakClick(word.originalWord, word.targetLanguage.code) },
            )
        }
        Box(
            modifier = Modifier
                .padding(vertical = Theme.spacing.xxs)
                .width(40.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(Theme.shapes.pill))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        Text(
            text = word.sourceLanguage.displayName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = Theme.colors.success,
        )
        HeadlineWord(text = word.translation, maxSize = 38)
        if (word.description.isNotBlank()) {
            Text(
                text = word.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Large centred word that shrinks (down to 22sp) and wraps to three lines before it would overflow. */
@Composable
private fun HeadlineWord(text: String, maxSize: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall.copy(hyphens = Hyphens.Auto),
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        maxLines = 3,
        autoSize = TextAutoSize.StepBased(minFontSize = 22.sp, maxFontSize = maxSize.sp, stepSize = 2.sp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.RevealHint() {
    Column(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = Theme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Theme.dimensions.iconSizeMedium),
        )
        Text(
            text = stringResource(Res.string.tap_card_to_reveal),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Mastery level chip ───────────────────────────────────────────────────────

@Composable
fun MasteryLevelBadge(level: Int, modifier: Modifier = Modifier) {
    val (masteryText, masteryColor) = when (level) {
        0 -> Pair(stringResource(Res.string.new), MaterialTheme.colorScheme.primary)
        1 -> Pair(stringResource(Res.string.learning), MaterialTheme.colorScheme.tertiary)
        2 -> Pair(stringResource(Res.string.familiar), Theme.colors.success)
        3 -> Pair(stringResource(Res.string.consolidating), Theme.colors.success)
        4 -> Pair(stringResource(Res.string.young), MaterialTheme.colorScheme.primary)
        5 -> Pair(stringResource(Res.string.mature), MaterialTheme.colorScheme.tertiary)
        6 -> Pair(stringResource(Res.string.mastered), Theme.colors.success)
        else -> Pair(stringResource(Res.string.unknown), MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Theme.shapes.pill))
            .background(masteryColor.copy(alpha = Theme.opacity.focus))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(masteryColor)
        )
        Text(
            text = masteryText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = masteryColor,
            maxLines = 1,
        )
    }
}

// ── Speaker button with pulse & loading ring ─────────────────────────────────

@Composable
private fun SpeakerButton(
    ttsState: TtsState,
    size: Dp,
    onClick: () -> Unit,
) {
    val isSpeaking = ttsState is TtsState.Speaking
    val isLoading = ttsState is TtsState.Loading || ttsState is TtsState.Downloading

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "speakerPress"
    )
    val pulse by rememberInfiniteTransition(label = "speakerPulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseRaw"
    )
    val pulseGate by animateFloatAsState(if (isSpeaking) 1f else 0f, tween(300), label = "pulseGate")
    val accent = MaterialTheme.colorScheme.primary
    val bgColor by animateColorAsState(
        targetValue = accent.copy(alpha = if (isSpeaking) 0.2f else Theme.opacity.focus),
        animationSpec = tween(300),
        label = "speakerBg"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                val s = pressScale * (1f + pulse * 0.08f * pulseGate)
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = "Play pronunciation",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = stringResource(Res.string.repeat_pronunciation),
            modifier = Modifier
                .size(size * 0.46f)
                .graphicsLayer { alpha = if (isLoading) 0.4f else 1f },
            tint = accent
        )
        when (ttsState) {
            is TtsState.Downloading -> CircularProgressIndicator(
                progress = { ttsState.progress },
                modifier = Modifier.size(size - 6.dp),
                strokeWidth = 2.dp,
                color = accent
            )
            is TtsState.Loading -> CircularProgressIndicator(
                modifier = Modifier.size(size - 6.dp),
                strokeWidth = 2.dp,
                color = accent
            )
            else -> Unit
        }
    }
}
