package components.toast

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** Android's own Toast, so it looks and behaves like every other app's. */
@Composable
actual fun ToastHost(state: ToastState, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val message = state.current
    LaunchedEffect(message) {
        message ?: return@LaunchedEffect
        Toast.makeText(context, message.text, Toast.LENGTH_LONG).show()
        state.dismiss(message)
    }
}
