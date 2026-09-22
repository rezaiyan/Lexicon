package navigation

import platform.UIKit.UIDevice

actual object NativeTabBarBridge {
    private var tapListener: ((String) -> Unit)? = null

    /** Swift assigns this once at launch to receive tab changes driven from Compose's own nav state. */
    var onCurrentTabChanged: ((String) -> Unit)? = null

    /** Swift assigns this once at launch to know when to show/hide the native TabView chrome. */
    var onTabBarVisibilityChanged: ((Boolean) -> Unit)? = null

    actual fun onNativeTabSelected(tab: String) {
        tapListener?.invoke(tab)
    }

    actual fun reportCurrentTab(tab: String) {
        onCurrentTabChanged?.invoke(tab)
    }

    actual fun setTabTapListener(listener: (String) -> Unit) {
        tapListener = listener
    }

    actual fun reportTabBarVisible(visible: Boolean) {
        onTabBarVisibilityChanged?.invoke(visible)
    }
}

actual fun isNativeTabBarSupported(): Boolean {
    val majorVersion = UIDevice.currentDevice.systemVersion
        .substringBefore(".")
        .toIntOrNull()
        ?: 0
    return majorVersion >= 26
}
