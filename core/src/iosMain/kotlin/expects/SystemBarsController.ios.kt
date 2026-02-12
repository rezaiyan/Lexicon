package expects

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UINavigationBar
import platform.UIKit.UINavigationBarAppearance

@Composable
actual fun SetSystemBarsColor(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkIcons: Boolean
) {
    SideEffect {
        val window = UIApplication.sharedApplication.keyWindow
        window?.rootViewController?.let { rootVC ->
            rootVC.setNeedsStatusBarAppearanceUpdate()
            
            val appearance = UINavigationBarAppearance().apply {
                configureWithOpaqueBackground()
                backgroundColor = UIColor.colorWithRed(
                    red = navigationBarColor.red.toDouble(),
                    green = navigationBarColor.green.toDouble(),
                    blue = navigationBarColor.blue.toDouble(),
                    alpha = navigationBarColor.alpha.toDouble()
                )
                shadowColor = null
            }
            
            UINavigationBar.appearance().standardAppearance = appearance
            UINavigationBar.appearance().scrollEdgeAppearance = appearance
            UINavigationBar.appearance().compactAppearance = appearance
            UINavigationBar.appearance().compactScrollEdgeAppearance = appearance
        }
    }
}



