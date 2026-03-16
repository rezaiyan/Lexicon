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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import components.animation.staggeredFadeSlide
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource
import theme.Theme

@Composable
internal fun ImageImportContent(
    imageTab: ImportTabV2.Image,
    isEnabled: Boolean,
    isLoading: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onImportImage: () -> Unit,
    onClearSelectedImage: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                        isLoading = isLoading,
                        onConfirm = onImportImage,
                        onCancel = onClearSelectedImage,
                        isEnabled = isEnabled,
                    )
                } else {
                    ImageSelectionContent(
                        onCameraClick = onCameraClick,
                        onGalleryClick = onGalleryClick,
                        isEnabled = isEnabled,
                    )
                }
            }

            if (!showPreview) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    enabled = isEnabled,
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ImageSelectionContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    isEnabled: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)) {
        ImageSourceHeader(
            modifier = Modifier.staggeredFadeSlide(0),
        )

        ImageSourcePicker(
            onCameraClick = onCameraClick,
            onGalleryClick = onGalleryClick,
            isEnabled = isEnabled,
            modifier = Modifier.staggeredFadeSlide(1),
        )
    }
}
