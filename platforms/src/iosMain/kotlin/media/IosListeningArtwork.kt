package media

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.MediaPlayer.MPMediaItemArtwork
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageSymbolConfiguration
import platform.UIKit.UIRectFill

private const val ARTWORK_PT = 512.0
private const val GLYPH_RATIO = 0.44
private const val GRADIENT_STEPS = 64
private const val BRAND_PURPLE_R = 0x7F / 255.0
private const val BRAND_PURPLE_G = 0x5A / 255.0
private const val BRAND_PURPLE_B = 0xF0 / 255.0
private const val BRAND_GREEN_R = 0x2C / 255.0
private const val BRAND_GREEN_G = 0xB6 / 255.0
private const val BRAND_GREEN_B = 0x7D / 255.0

/** Brand gradient with a headphones symbol, so the lock screen never shows a blank tile. */
@OptIn(ExperimentalForeignApi::class)
internal fun listeningArtwork(): MPMediaItemArtwork {
    val image = renderArtwork()
    return MPMediaItemArtwork(boundsSize = CGSizeMake(ARTWORK_PT, ARTWORK_PT)) { _ -> image }
}

@OptIn(ExperimentalForeignApi::class)
private fun renderArtwork(): UIImage =
    UIGraphicsImageRenderer(size = CGSizeMake(ARTWORK_PT, ARTWORK_PT)).imageWithActions { _ ->
        for (step in 0 until GRADIENT_STEPS) {
            val t = step.toDouble() / (GRADIENT_STEPS - 1)
            UIColor(
                red = lerp(BRAND_PURPLE_R, BRAND_GREEN_R, t),
                green = lerp(BRAND_PURPLE_G, BRAND_GREEN_G, t),
                blue = lerp(BRAND_PURPLE_B, BRAND_GREEN_B, t),
                alpha = 1.0,
            ).setFill()
            val y = ARTWORK_PT * step / GRADIENT_STEPS
            UIRectFill(CGRectMake(0.0, y, ARTWORK_PT, ARTWORK_PT / GRADIENT_STEPS + 1))
        }
        val glyphSize = ARTWORK_PT * GLYPH_RATIO
        val symbol = UIImage.systemImageNamed(
            "headphones",
            withConfiguration = UIImageSymbolConfiguration.configurationWithPointSize(glyphSize),
        )?.imageWithTintColor(UIColor.whiteColor)
        val origin = (ARTWORK_PT - glyphSize) / 2
        symbol?.drawInRect(CGRectMake(origin, origin, glyphSize, glyphSize))
    }

private fun lerp(from: Double, to: Double, t: Double) = from + (to - from) * t
