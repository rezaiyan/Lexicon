@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package expects

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual fun shareTextAsFile(
    title: String,
    text: String,
    filename: String
) {
    try {
        val tempDir = NSTemporaryDirectory()
        val filePath = "$tempDir$filename"
        
        (text as NSString).writeToFile(
            path = filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        
        val fileURL = NSURL.fileURLWithPath(filePath)
        val activityItems = listOf(fileURL)
        
        val activityViewController = UIActivityViewController(
            activityItems = activityItems,
            applicationActivities = null
        )
        
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        rootViewController?.presentViewController(
            activityViewController,
            animated = true,
            completion = null
        )
    } catch (e: Exception) {
        println(" Failed to share file: ${e.message}")
    }
}

