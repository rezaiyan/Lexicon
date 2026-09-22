package navigation

/**
 * Bridges the two-tab bottom bar between Compose navigation and a platform's native tab chrome
 * (iOS 26+ SwiftUI `TabView`, which renders genuine OS Liquid Glass). No-op on platforms without
 * native tab chrome — Compose keeps driving its own bottom bar there.
 */
expect object NativeTabBarBridge {
    /** Called by the native side when the user taps a native tab item. */
    fun onNativeTabSelected(tab: String)

    /** Called by Compose whenever its own nav state settles on a tab, to keep native selection in sync. */
    fun reportCurrentTab(tab: String)

    /** Registered once by Compose to receive native tab taps. */
    fun setTabTapListener(listener: (String) -> Unit)

    /**
     * Called by Compose whenever the app enters/leaves the tabbed main experience — true only once
     * the user is past auth/onboarding/splash and looking at Study or Settings. The native tab
     * chrome must stay hidden outside that window.
     */
    fun reportTabBarVisible(visible: Boolean)
}

/** True when the current platform/OS renders its own native tab bar chrome (e.g. iOS 26+). */
expect fun isNativeTabBarSupported(): Boolean
