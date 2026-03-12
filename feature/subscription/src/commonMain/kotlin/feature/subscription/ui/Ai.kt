package feature.subscription.ui

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Icons.Outlined.AiBook: ImageVector
    get() {
        _aiBook?.let { return it }

        _aiBook = ImageVector.Builder(
            name = "AiBook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {

            // --- Outer rounded book cover ---
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 3f)
                lineTo(16f, 3f)

                // top-right corner
                quadTo(19f, 3f, 19f, 6f)

                lineTo(19f, 15f)

                // right-bottom corner
                quadTo(19f, 17f, 17f, 17f)

                lineTo(8f, 17f)

                // bottom-left corner
                quadTo(5f, 17f, 5f, 14f)

                lineTo(5f, 6f)

                // top-left corner
                quadTo(5f, 3f, 8f, 3f)
            }

            // --- Bottom curved book pages ---
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 14f)
                quadTo(5f, 20f, 11f, 20f)

                // Rounded bottom-right page curve
                arcToRelative(
                    2f, 2f,
                    0f,
                    false,
                    true,
                    4f, 0f
                )
            }

            // --- Letter A ---
            path(
                stroke = SolidColor(Color.Black),
                fill = SolidColor(Color.Transparent),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(10f, 9f)
                lineTo(9f, 13f)
                moveTo(10f, 9f)
                lineTo(11f, 13f)
                moveTo(9.4f, 12f)
                lineTo(10.6f, 12f)
            }

            // --- Letter I ---
            path(
                stroke = SolidColor(Color.Black),
                fill = SolidColor(Color.Transparent),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(13f, 9f)
                lineTo(13f, 13f)
            }

        }.build()

        return requireNotNull(_aiBook) { "AiBook icon was not built" }
    }

private var _aiBook: ImageVector? = null
