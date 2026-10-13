package expects

import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.insets.ProtectionLayout

@Composable
actual fun SetSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            // Inside a dialog (e.g. ModalBottomSheet): set colors on the dialog window
            val dialogWindow = view.findDialogWindow()
            if (dialogWindow != null) {
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                @Suppress("DEPRECATION")
                dialogWindow.statusBarColor = statusBarColor.toArgb()
                @Suppress("DEPRECATION")
                dialogWindow.navigationBarColor = navigationBarColor.toArgb()
                WindowCompat.getInsetsController(dialogWindow, view).apply {
                    isAppearanceLightStatusBars = darkIcons
                    isAppearanceLightNavigationBars = darkIcons
                }
                return@SideEffect
            }

            // Activity window
            val activity = view.context.findActivity() ?: return@SideEffect
            activity.reapplyEdgeToEdge(
                statusBarStyle = if (darkIcons) {
                    SystemBarStyle.light(statusBarColor.toArgb(), statusBarColor.toArgb())
                } else {
                    SystemBarStyle.dark(statusBarColor.toArgb())
                },
                navigationBarStyle = if (darkIcons) {
                    SystemBarStyle.light(navigationBarColor.toArgb(), navigationBarColor.toArgb())
                } else {
                    SystemBarStyle.dark(navigationBarColor.toArgb())
                }
            )
        }
    }
}

/**
 * [enableEdgeToEdge] on API 35+ paints the bar colours with a [ProtectionLayout] it adds to the
 * decor view on every call and never removes. Called per colour change, the layers pile up: an
 * older opaque bar shows through a newer transparent one, and the decor view grows with each
 * sheet. Dropping the earlier layers first keeps exactly one, the latest.
 */
private fun ComponentActivity.reapplyEdgeToEdge(statusBarStyle: SystemBarStyle, navigationBarStyle: SystemBarStyle) {
    val decor = window.decorView as ViewGroup
    for (index in decor.childCount - 1 downTo 0) {
        if (decor.getChildAt(index) is ProtectionLayout) decor.removeViewAt(index)
    }
    enableEdgeToEdge(statusBarStyle, navigationBarStyle)
}

private fun View.findDialogWindow(): Window? {
    var currentParent = parent
    while (currentParent is View) {
        if (currentParent is DialogWindowProvider) {
            return currentParent.window
        }
        currentParent = currentParent.parent
    }
    return null
}

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
