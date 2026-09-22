package components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

/** Backing layer for [glassBackdropSource] — captures screen content so a glass surface elsewhere can redraw a blurred copy of it. */
@Composable
fun rememberGlassBackdropLayer(): GraphicsLayer = rememberGraphicsLayer()

/** Records this composable's drawn content into [layer] every frame, in addition to drawing normally. */
fun Modifier.glassBackdropSource(layer: GraphicsLayer): Modifier = drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawContent()
}
