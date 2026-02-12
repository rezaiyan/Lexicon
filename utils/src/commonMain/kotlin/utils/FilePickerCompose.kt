package utils

import androidx.compose.runtime.Composable

/**
 * Common file picker launcher for text files
 * Returns content and fileName of picked file
 * Callback: (content: String?, fileName: String?) -> Unit
 */
@Composable
expect fun rememberTextFilePickerLauncher(
    onFilePicked: (content: String?, fileName: String?) -> Unit
): () -> Unit


