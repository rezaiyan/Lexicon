package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import domain.tag.model.Tag
import theme.Theme
import utils.Language

/** Photo import: source picker until an image is chosen, then its preview. */
@Composable
internal fun ImageImportContent(
    imageTab: ImportTabV2.Image,
    isEnabled: Boolean,
    isLoading: Boolean,
    imageQuality: Float,
    sourceLanguage: Language,
    targetLanguage: Language,
    tags: List<Tag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit,
    onCreateTag: () -> Unit,
    onChangeLanguage: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onImportImage: () -> Unit,
    onClearSelectedImage: () -> Unit,
    onQualityChange: (Float) -> Unit,
) {
    val motion = Theme.motion
    AnimatedContent(
        targetState = imageTab.selectedImage,
        contentKey = { it != null },
        transitionSpec = {
            fadeIn(tween(motion.durationMedium)) togetherWith fadeOut(tween(motion.durationShort))
        },
        label = "PhotoSourceToPreview",
    ) { selectedImage ->
        if (selectedImage != null) {
            PhotoPreviewPage(
                imageBytes = selectedImage,
                isLoading = isLoading,
                isEnabled = isEnabled,
                imageQuality = imageQuality,
                onQualityChange = onQualityChange,
                onConfirm = onImportImage,
                onRetake = onClearSelectedImage,
            )
        } else {
            PhotoSourcePage(
                isEnabled = isEnabled,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                tags = tags,
                selectedTagId = selectedTagId,
                onTagSelected = onTagSelected,
                onCreateTag = onCreateTag,
                onChangeLanguage = onChangeLanguage,
                onCameraClick = onCameraClick,
                onGalleryClick = onGalleryClick,
            )
        }
    }
}
