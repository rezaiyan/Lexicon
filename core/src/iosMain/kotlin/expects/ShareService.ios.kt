package expects

import platform.Foundation.NSString
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IOSShareService : ShareService {
    override fun share(title: String, text: String) {
        val fullText = "$title\n\n$text"
        val activityItems = listOf<Any>(fullText as NSString)
        
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
    }
}

actual fun getShareService(): ShareService = IOSShareService()



