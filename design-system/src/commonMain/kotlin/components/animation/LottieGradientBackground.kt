package components.animation

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import lexicon.resources.generated.resources.Res
/**
 * Ambient gradient background using a Lottie animation.
 *
 * @param alpha Overall opacity — use < 1f to blend softly over the background.
 * @param tint Optional color tinted over the animation via [BlendMode.SrcAtop].
 *             Pass [Color.Unspecified] (default) for no tint.
 */
@Composable
fun LottieGradientBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    tint: Color = Color.Unspecified,
) {
    var animationJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        animationJson = Res.readBytes("files/login_gradient.json").decodeToString()
    }

    val json = animationJson ?: return

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(json)
    }

    if (composition != null) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
            ),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.FillBounds,
            alpha = alpha,
            colorFilter = if (tint != Color.Unspecified) ColorFilter.tint(tint, BlendMode.SrcAtop) else null,
        )
    }
}
