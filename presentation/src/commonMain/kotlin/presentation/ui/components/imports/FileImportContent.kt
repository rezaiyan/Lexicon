package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import components.animation.staggeredFadeSlide
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.choose_file
import lexicon.resources.generated.resources.format_example_1
import lexicon.resources.generated.resources.format_example_2
import lexicon.resources.generated.resources.format_example_3
import lexicon.resources.generated.resources.import_from_file
import lexicon.resources.generated.resources.processing_file
import lexicon.resources.generated.resources.select_txt_file_description
import lexicon.resources.generated.resources.supported_format
import lexicon.resources.generated.resources.txt_format
import org.jetbrains.compose.resources.stringResource
import theme.Theme
import utils.rememberTextFilePickerLauncher

@Composable
internal fun FileImportContent(
    isEnabled: Boolean,
    isLoading: Boolean,
    importFile: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val filePickerLauncher = rememberTextFilePickerLauncher { fileContent, fileName ->
        if (fileContent != null) {
            importFile(fileContent, fileName)
        } else if (fileName != null) {
            importFile("", fileName)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
        ) {
            FileSourceHeader(
                modifier = Modifier.staggeredFadeSlide(0),
            )

            FileDropZone(
                onClick = filePickerLauncher,
                isEnabled = isEnabled && !isLoading,
                isLoading = isLoading,
                modifier = Modifier.staggeredFadeSlide(1),
            )

            SupportedFormatsSection(
                modifier = Modifier.staggeredFadeSlide(2),
            )
        }

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

@Composable
private fun FileSourceHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
        ) {
            Icon(
                Icons.Filled.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(Theme.dimensions.iconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.import_from_file),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            stringResource(Res.string.select_txt_file_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FileDropZone(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val cornerRadiusDp = Theme.shapes.large

    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = borderColor,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(8.dp.toPx(), 5.dp.toPx()),
                        ),
                    ),
                    cornerRadius = CornerRadius(cornerRadiusDp.toPx()),
                )
            }
            .padding(Theme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.md),
    ) {
        Surface(
            modifier = Modifier.size(Theme.dimensions.iconSizeHuge),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Theme.dimensions.iconSize),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Filled.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(Theme.dimensions.iconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        if (isLoading) {
            Text(
                stringResource(Res.string.processing_file),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
            ) {
                Button(
                    onClick = onClick,
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Theme.dimensions.buttonHeightSmall),
                    shape = RoundedCornerShape(Theme.shapes.small),
                ) {
                    Text(stringResource(Res.string.choose_file))
                }
                Text(
                    stringResource(Res.string.txt_format),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SupportedFormatsSection(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = Theme.spacing.xxs),
        verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs),
    ) {
        Text(
            stringResource(Res.string.supported_format),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxxs)) {
            FormatExample(Res.string.format_example_1)
            FormatExample(Res.string.format_example_2)
            FormatExample(Res.string.format_example_3)
        }
    }
}

@Composable
private fun FormatExample(res: org.jetbrains.compose.resources.StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}
