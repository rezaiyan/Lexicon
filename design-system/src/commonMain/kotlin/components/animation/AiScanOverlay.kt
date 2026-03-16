package components.animation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import lexicon.resources.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * AI-style scanning overlay using a Lottie scanner animation.
 * Designed to overlay image thumbnails during AI processing.
 *
 * @param label Text shown at the bottom of the overlay.
 * @param modifier Modifier for the root Box.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun AiScanOverlay(
    label: String,
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

        // Label badge at the bottom
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            shadowElevation = 4.dp,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
