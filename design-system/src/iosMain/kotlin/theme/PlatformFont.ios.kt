package theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

@Composable
actual fun platformFontFamily(): FontFamily? = dmSansFontFamily()
