package expects

import android.content.Context
import android.content.ContextWrapper
import android.view.View
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
            activity.enableEdgeToEdge(
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
