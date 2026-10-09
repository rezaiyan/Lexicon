package utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.io.IOException

// Some providers (e.g. Google Drive) label .csv as application/vnd.ms-excel; binary files are rejected later by content.
private val WordFileMimeTypes = arrayOf("text/*", "application/csv", "application/vnd.ms-excel")

@Composable
actual fun rememberWordFilePickerLauncher(maxBytes: Int, onPicked: (PickedFile?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        callback.value(uri?.let { context.readPickedFile(it, maxBytes) })
    }
    return { launcher.launch(WordFileMimeTypes) }
}

@Suppress("SwallowedException")
private fun Context.readPickedFile(uri: Uri, maxBytes: Int): PickedFile? = try {
    val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
    contentResolver.openInputStream(uri)?.use { input ->
        // One byte past the limit is enough to report "too large" without reading a huge file into memory.
        PickedFile(name, input.readNBytesCompat(maxBytes + 1))
    }
} catch (e: IOException) {
    null
} catch (e: SecurityException) {
    null
}

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8_192)
    while (buffer.size() < limit) {
        val read = read(chunk, 0, minOf(chunk.size, limit - buffer.size()))
        if (read < 0) break
        buffer.write(chunk, 0, read)
    }
    return buffer.toByteArray()
}
