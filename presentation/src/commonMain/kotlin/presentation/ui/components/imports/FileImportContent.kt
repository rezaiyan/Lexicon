package presentation.ui.components.imports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lexicon.resources.generated.resources.Res
import lexicon.resources.generated.resources.cancel
import lexicon.resources.generated.resources.import_from_file
import lexicon.resources.generated.resources.choose_file
import lexicon.resources.generated.resources.format_example_1
import lexicon.resources.generated.resources.format_example_2
import lexicon.resources.generated.resources.format_example_3
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.md)
        ) {
            ImportInfoCard(
                title = stringResource(Res.string.import_from_file),
                description = stringResource(Res.string.select_txt_file_description),
                icon = Icons.Filled.UploadFile,
            )

            FilePickerButton(
                onClick = filePickerLauncher,
                isEnabled = isEnabled && !isLoading,
                isLoading = isLoading,
            )

            SupportedFormatsCard()
        }

        Spacer(modifier = Modifier.height(Theme.spacing.sm))

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

@Composable
private fun FilePickerButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    isLoading: Boolean,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(Theme.shapes.large),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    strokeWidth = 3.dp
                )
                Text(
                    stringResource(Res.string.processing_file),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    Icons.Filled.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    stringResource(Res.string.choose_file),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(Res.string.txt_format),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SupportedFormatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(Theme.shapes.medium)
    ) {
        Column(
            modifier = Modifier.padding(Theme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.spacing.xs)
        ) {
            Text(
                stringResource(Res.string.supported_format),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(Theme.spacing.xxs)) {
                FormatExample(Res.string.format_example_1)
                FormatExample(Res.string.format_example_2)
                FormatExample(Res.string.format_example_3)
            }
        }
    }
}

@Composable
private fun FormatExample(res: org.jetbrains.compose.resources.StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
