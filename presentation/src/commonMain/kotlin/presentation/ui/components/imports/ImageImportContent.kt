package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun ImageImportContent(
    imageTab: ImportTabV2.Image,
    isEnabled: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onUpdateExtractionOptions: (List<ExtractionOption>) -> Unit,
    onImportImage: () -> Unit,
    onClearSelectedImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    val extractWords = imageTab.extractionOption.contains(ExtractionOption.Word)
    val extractSentences = imageTab.extractionOption.contains(ExtractionOption.Sentence)
    val hasImage = imageTab.selectedImage != null

    AnimatedContent(
        targetState = hasImage,
        transitionSpec = {
            val enterOffset = if (targetState) { i: Int -> i / 3 } else { i: Int -> -i / 3 }
            val exitOffset = if (targetState) { i: Int -> -i / 3 } else { i: Int -> i / 3 }
            (slideInVertically(
                initialOffsetY = enterOffset,
                animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
            ) + fadeIn(tween(400))).togetherWith(
                slideOutVertically(targetOffsetY = exitOffset, animationSpec = tween(300))
                        + fadeOut(tween(300))
            )
        },
        label = "ImagePreviewTransition"
    ) { showPreview ->
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (showPreview && imageTab.selectedImage != null) {
                    ImagePreviewCard(
                        imageBytes = imageTab.selectedImage,
                        onConfirm = onImportImage,
                        onCancel = onClearSelectedImage,
                        isEnabled = isEnabled
                    )
                } else {
                    ImageSelectionContent(
                        extractWords = extractWords,
                        extractSentences = extractSentences,
                        onExtractWordsChange = { checked ->
                            val options = imageTab.extractionOption.toMutableList()
                            if (checked) options.addIfAbsent(ExtractionOption.Word)
                            else options.remove(ExtractionOption.Word)
                            onUpdateExtractionOptions(options)
                        },
                        onExtractSentencesChange = { checked ->
                            val options = imageTab.extractionOption.toMutableList()
                            if (checked) options.addIfAbsent(ExtractionOption.Sentence)
                            else options.remove(ExtractionOption.Sentence)
                            onUpdateExtractionOptions(options)
                        },
                        onCameraClick = onCameraClick,
                        onGalleryClick = onGalleryClick,
                        isEnabled = isEnabled,
                    )
                }
            }

            if (!showPreview) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(Theme.dimensions.buttonHeight)
                        .imePadding(),
                    enabled = isEnabled,
                    shape = RoundedCornerShape(Theme.shapes.medium)
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ImageSelectionContent(
    extractWords: Boolean,
    extractSentences: Boolean,
    onExtractWordsChange: (Boolean) -> Unit,
    onExtractSentencesChange: (Boolean) -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isEnabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
        AiExtractionInfoCard()

        ExtractionOptionsCard(
            extractWords = extractWords,
            extractSentences = extractSentences,
            onExtractWordsChange = onExtractWordsChange,
            onExtractSentencesChange = onExtractSentencesChange,
            isEnabled = isEnabled
        )

        CaptureButtons(
            onCameraClick = onCameraClick,
            onGalleryClick = onGalleryClick,
            isEnabled = (extractWords || extractSentences) && isEnabled,
        )
    }
}

private fun <T> MutableList<T>.addIfAbsent(element: T) {
    if (!contains(element)) add(element)
}
