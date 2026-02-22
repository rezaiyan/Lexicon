package expects

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView

@Composable
actual fun SetSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkIcons: Boolean
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = (view.context as? ComponentActivity) ?: return@SideEffect
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
