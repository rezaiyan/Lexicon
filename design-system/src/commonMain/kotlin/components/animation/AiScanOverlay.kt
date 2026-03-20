package components.animation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import lexicon.resources.generated.resources.Res
/**
 * AI-style scanning overlay using a Lottie scanner animation.
 * Designed to overlay image thumbnails during AI processing.
 *
 * @param modifier Modifier for the root Box.
 */
@Composable
fun AiScanOverlay(
    modifier: Modifier = Modifier,
) {
    var animationJson by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        animationJson = Res.readBytes("files/scanner.json").decodeToString()
    }

    val json = animationJson ?: return

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(json)
    }

    Box(modifier = modifier) {
        if (composition != null) {
            Image(
                painter = rememberLottiePainter(
                    composition = composition,
                    iterations = Compottie.IterateForever,
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
