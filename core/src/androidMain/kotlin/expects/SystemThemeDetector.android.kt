package expects

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Composable
actual fun isSystemInDarkTheme(): Boolean {
    val context = object : KoinComponent {
        val context: android.content.Context by inject()
    }.context
    
    val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
}

