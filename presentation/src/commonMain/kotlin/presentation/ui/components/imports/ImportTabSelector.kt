package presentation.ui.components.imports

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.TabHost
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.processing_image_with_ai
import org.jetbrains.compose.resources.stringResource
import presentation.model.ImageImportState
import theme.Theme
import utils.rememberCameraLauncher
import utils.rememberImagePickerLauncher

@Composable
internal fun ImportTabSelector(
    modifier: Modifier = Modifier,
    tabs: List<ImportTabV2>,
    selectedTab: ImportTabV2,
    onTabSelected: (ImportTabV2) -> Unit,
) {
    val selectedTabIndex = tabs.indexOfFirst { it::class == selectedTab::class }
        .coerceAtLeast(0)

    TabHost(
        modifier = modifier,
        tabs = tabs,
        selectedIndex = selectedTabIndex,
        onTabSelected = { _, tab -> onTabSelected(tab) },
    ) { tab, _ ->
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            modifier = Modifier.size(Theme.dimensions.iconSizeSmall),
        )
        Text(
            text = stringResource(tab.title),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
internal fun ImportTabContent(
    state: ImportUiState,
    viewModel: ImportViewModel,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onShowSourceLanguage: () -> Unit,
    onShowTargetLanguage: () -> Unit,
) {
    val isImageLoading = state.imageImportState is ImageImportState.Loading

    Box(modifier = modifier) {
        AnimatedContent(
            targetState = state.selectedTab::class,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                val animDuration = 350
                (fadeIn(tween(animDuration, easing = EaseInOut)) +
                    slideInVertically(tween(animDuration, easing = EaseInOut)) { it / 16 })
                    .togetherWith(
                        fadeOut(tween(animDuration / 2, easing = EaseInOut)) +
                            slideOutVertically(tween(animDuration / 2, easing = EaseInOut)) { -it / 16 }
                    ).using(
                        SizeTransform(clip = false) { _, _ ->
                            tween(animDuration, easing = EaseInOut)
                        }
                    )
            },
        ) { _ ->
            when (val tab = state.selectedTab) {
                is ImportTabV2.Text -> TextImportContent(
                    textInputState = state.textInputState,
                    sourceLanguage = state.sourceLanguage,
                    targetLanguage = state.targetLanguage,
                    onWordChange = viewModel::updateWord,
                    onTranslationChange = viewModel::updateTranslation,
                    onDescriptionChange = viewModel::updateDescription,
                    onAddWord = viewModel::addWord,
                    onShowSourceLanguage = onShowSourceLanguage,
                    onShowTargetLanguage = onShowTargetLanguage,
                )

                is ImportTabV2.File -> FileImportContent(
                    importFile = viewModel::importFile,
                    onDismiss = onDismiss,
                    isEnabled = state.fileImportState !is ImportFileState.Loading,
                    isLoading = state.fileImportState is ImportFileState.Loading,
                )

                is ImportTabV2.Image -> {
                    val imagePickerLauncher = rememberImagePickerLauncher { bytes ->
                        if (bytes != null) viewModel.selectImage(bytes)
                    }
                    val cameraLauncher = rememberCameraLauncher { bytes ->
                        if (bytes != null) viewModel.selectImage(bytes)
                    }
                    ImageImportContent(
                        imageTab = tab,
                        isEnabled = !isImageLoading && state.fileImportState !is ImportFileState.Loading,
                        onCameraClick = cameraLauncher,
                        onGalleryClick = imagePickerLauncher,
                        onUpdateExtractionOptions = viewModel::updateExtractionOptions,
                        onImportImage = viewModel::importImage,
                        onClearSelectedImage = viewModel::clearSelectedImage,
                        onDismiss = onDismiss,
                    )
                }
            }
        }

        if (isImageLoading) {
            ImageProcessingOverlay()
        }
    }
}

@Composable
private fun ImageProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(Theme.shapes.large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    Theme.spacing.xxs,
                    Alignment.CenterVertically
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Theme.dimensions.touchTarget),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    stringResource(Res.string.processing_image_with_ai),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
