package theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import lexicon.design_system.generated.resources.Res
import lexicon.design_system.generated.resources.dm_sans_bold
import lexicon.design_system.generated.resources.dm_sans_medium
import lexicon.design_system.generated.resources.dm_sans_regular
import lexicon.design_system.generated.resources.dm_sans_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun dmSansFontFamily(): FontFamily =
    FontFamily(
        Font(Res.font.dm_sans_regular, weight = FontWeight.Normal),
        Font(Res.font.dm_sans_medium, weight = FontWeight.Medium),
        Font(Res.font.dm_sans_semibold, weight = FontWeight.SemiBold),
        Font(Res.font.dm_sans_bold, weight = FontWeight.Bold),
    )

@Composable
expect fun platformFontFamily(): FontFamily?
