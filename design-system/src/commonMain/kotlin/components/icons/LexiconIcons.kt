package components.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object LexiconIcons {

    val Diamond: ImageVector by lazy {
        ImageVector.Builder(
            name = "Diamond",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            val gap = 0.1f

            // Top left facet
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f + gap, 4f + gap)
                lineTo(11.5f - gap, 4f + gap)
                lineTo(9.5f, 9f - gap)
                lineTo(4f + gap, 9f - gap)
                close()
            }

            // Top right facet
            path(fill = SolidColor(Color.Black)) {
                moveTo(12.5f + gap, 4f + gap)
                lineTo(18f - gap, 4f + gap)
                lineTo(20f - gap, 9f - gap)
                lineTo(14.5f, 9f - gap)
                close()
            }

            // Center top facet
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.5f + gap, 4f + gap)
                lineTo(12.5f - gap, 4f + gap)
                lineTo(14f - gap, 9f - gap)
                lineTo(10f + gap, 9f - gap)
                close()
            }

            // Middle left facet
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f + gap, 10f + gap)
                lineTo(11.5f - gap, 10f + gap)
                lineTo(12f - gap, 18f - gap)
                close()
            }

            // Middle right facet
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f - gap, 10f + gap)
                lineTo(12.5f + gap, 10f + gap)
                lineTo(12f + gap, 18f - gap)
                close()
            }
        }.build()
    }
}