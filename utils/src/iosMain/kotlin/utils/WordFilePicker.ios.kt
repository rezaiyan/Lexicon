package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject
import platform.posix.memcpy

@Composable
actual fun rememberWordFilePickerLauncher(maxBytes: Int, onPicked: (PickedFile?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    val picker = remember { WordFileDocumentPicker() }
    return remember { { picker.present(maxBytes) { callback.value(it) } } }
}

private class WordFileDocumentPicker {
    // Strong reference: UIDocumentPickerViewController holds its delegate weakly.
    private var delegate: NSObject? = null

    fun present(maxBytes: Int, onPicked: (PickedFile?) -> Unit) {
        // UTTypeText covers plain text, CSV and TSV (they conform to public.text).
        val controller = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeText), asCopy = true)
        val pickerDelegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
                onPicked((didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.readPickedFile(maxBytes))
                delegate = null
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                onPicked(null)
                delegate = null
            }
        }
        delegate = pickerDelegate
        controller.delegate = pickerDelegate
        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(controller, true, null)
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun NSURL.readPickedFile(maxBytes: Int): PickedFile? {
    val access = startAccessingSecurityScopedResource()
    try {
        val data: NSData = NSData.dataWithContentsOfURL(this) ?: return null
        val length = minOf(data.length.toInt(), maxBytes + 1)
        val bytes = ByteArray(length)
        if (length > 0) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, length.toULong()) }
        return PickedFile(lastPathComponent, bytes)
    } finally {
        if (access) stopAccessingSecurityScopedResource()
    }
}
