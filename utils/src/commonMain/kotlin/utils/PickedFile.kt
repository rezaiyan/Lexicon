package utils

import androidx.compose.runtime.Composable

/** Bytes of a picked file, read up to `maxBytes + 1` so callers can tell an oversized file apart. */
class PickedFile(val name: String?, val bytes: ByteArray)

/**
 * Picker for vocabulary files: any text type (.txt, .csv, .tsv, …). Decoding and format detection happen
 * in the domain layer. [onPicked] receives null when the user cancels or the file cannot be read.
 */
@Composable
expect fun rememberWordFilePickerLauncher(maxBytes: Int, onPicked: (PickedFile?) -> Unit): () -> Unit
