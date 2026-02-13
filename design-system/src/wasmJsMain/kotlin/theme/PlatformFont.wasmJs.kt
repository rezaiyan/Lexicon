package theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import lexicon.design_system.generated.resources.Res
import lexicon.design_system.generated.resources.noto_sans_medium
import lexicon.design_system.generated.resources.noto_sans_regular
import org.jetbrains.compose.resources.Font

@Composable
actual fun platformFontFamily(): FontFamily? =
    FontFamily(
        Font(Res.font.noto_sans_regular, weight = FontWeight.Normal),
        Font(Res.font.noto_sans_medium, weight = FontWeight.Medium),
        Font(Res.font.noto_sans_medium, weight = FontWeight.Bold),
    )
