@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package expects

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.coroutines.resume

actual class ImagePicker {
    private var currentDelegate: NSObject? = null

    actual suspend fun pickImage(): ByteArray? = suspendCancellableCoroutine { continuation ->
        dispatch_async(dispatch_get_main_queue()) {
            val pickerController = UIImagePickerController()
            pickerController.sourceType =
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
            pickerController.allowsEditing = false

            val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol,
                UINavigationControllerDelegateProtocol {
                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>
                ) {
                    currentDelegate = null
                    val image =
                        didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                    if (image == null) {
                        picker.dismissViewControllerAnimated(true, null)
                        continuation.resume(null)
                        return
                    }

                    val imageData = UIImageJPEGRepresentation(image, 0.8)

                    if (imageData == null) {
                        picker.dismissViewControllerAnimated(true, null)
                        continuation.resume(null)
                        return
                    }

                    val bytes = ByteArray(imageData.length.toInt()).apply {
                        usePinned { pinned ->
                            memcpy(pinned.addressOf(0), imageData.bytes, imageData.length)
                        }
                    }

                    picker.dismissViewControllerAnimated(true, null)
                    continuation.resume(bytes)
                }

                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                    currentDelegate = null
                    picker.dismissViewControllerAnimated(true, null)
                    continuation.resume(null)
                }
            }

            currentDelegate = delegate
            pickerController.delegate = delegate

            val rootViewController = UIApplication.sharedApplication.windows
                .map { it as? UIWindow }
                .firstOrNull { it?.isKeyWindow() == true }
                ?.rootViewController

            if (rootViewController == null) {
                continuation.resume(null)
                return@dispatch_async
            }

            rootViewController.presentViewController(pickerController, true, null)
        }
    }

    actual suspend fun takePhoto(): ByteArray? = suspendCancellableCoroutine { continuation ->
        dispatch_async(dispatch_get_main_queue()) {
            if (!UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            )) {
                continuation.resume(null)
                return@dispatch_async
            }

            val pickerController = UIImagePickerController()
            pickerController.sourceType =
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            pickerController.allowsEditing = false

            val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol,
                UINavigationControllerDelegateProtocol {
                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>
                ) {
                    currentDelegate = null
                    val image =
                        didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                    if (image == null) {
                        picker.dismissViewControllerAnimated(true, null)
                        continuation.resume(null)
                        return
                    }

                    val imageData = UIImageJPEGRepresentation(image, 0.8)

                    if (imageData == null) {
                        picker.dismissViewControllerAnimated(true, null)
                        continuation.resume(null)
                        return
                    }

                    val bytes = ByteArray(imageData.length.toInt()).apply {
                        usePinned { pinned ->
                            memcpy(pinned.addressOf(0), imageData.bytes, imageData.length)
                        }
                    }

                    picker.dismissViewControllerAnimated(true, null)
                    continuation.resume(bytes)
                }

                override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                    currentDelegate = null
                    picker.dismissViewControllerAnimated(true, null)
                    continuation.resume(null)
                }
            }

            currentDelegate = delegate
            pickerController.delegate = delegate

            val rootViewController = UIApplication.sharedApplication.windows
                .map { it as? UIWindow }
                .firstOrNull { it?.isKeyWindow() == true }
                ?.rootViewController

            if (rootViewController == null) {
                continuation.resume(null)
                return@dispatch_async
            }

            rootViewController.presentViewController(pickerController, animated = true, completion = null)
        }
    }
}

