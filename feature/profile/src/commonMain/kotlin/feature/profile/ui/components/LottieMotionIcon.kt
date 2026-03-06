package feature.profile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.Url
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter

@Composable
fun LottieMotionIcon(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    iterations: Int = Compottie.IterateForever
) {
    val composition by rememberLottieComposition {
        LottieCompositionSpec.Url(url)
    }

    if (composition != null) {
        Image(
            painter = rememberLottiePainter(
                composition = composition,
                iterations = iterations
            ),
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}
