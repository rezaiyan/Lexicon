package utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeText
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberTextFilePickerLauncher(
    onFilePicked: (content: String?, fileName: String?) -> Unit
): () -> Unit {
    val picker = remember { IOSFilePicker() }
    
    return remember {
        {
            picker.pickFile { content, fileName ->
                onFilePicked(content, fileName)
            }
        }
    }
}

private class IOSFilePicker {
    private var onFilePicked: ((String?, String?) -> Unit)? = null
    
    fun pickFile(callback: (String?, String?) -> Unit) {
        onFilePicked = callback
        
        dispatch_async(dispatch_get_main_queue()) {
            val documentPicker = UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(UTTypeText),
                asCopy = true
            )
            
            val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                override fun documentPicker(
                    controller: UIDocumentPickerViewController,
                    didPickDocumentsAtURLs: List<*>
                ) {
                    if (didPickDocumentsAtURLs.isEmpty()) {
                        onFilePicked?.invoke(null, null)
                        return
                    }
                    
                    val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                    if (url == null) {
                        onFilePicked?.invoke(null, null)
                        return
                    }
                    
                    val fileName = url.lastPathComponent
                    val hasAccess = url.startAccessingSecurityScopedResource()
                    
                    try {
                        val content = try {
                            NSString.stringWithContentsOfURL(
                                url,
                                encoding = NSUTF8StringEncoding,
                                error = null
                            ) as? String
                        } catch (e: Exception) {
                            null
                        }
                        
                        onFilePicked?.invoke(content, fileName)
                    } finally {
                        if (hasAccess) {
                            url.stopAccessingSecurityScopedResource()
                        }
                    }
                }
                
                override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                    onFilePicked?.invoke(null, null)
                }
            }
            
            documentPicker.delegate = delegate
            
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(documentPicker, true, null)
        }
    }
}



