package navigation

actual object NativeTabBarBridge {
    actual fun onNativeTabSelected(tab: String) = Unit
    actual fun reportCurrentTab(tab: String) = Unit
    actual fun setTabTapListener(listener: (String) -> Unit) = Unit
    actual fun reportTabBarVisible(visible: Boolean) = Unit
}

actual fun isNativeTabBarSupported(): Boolean = false
