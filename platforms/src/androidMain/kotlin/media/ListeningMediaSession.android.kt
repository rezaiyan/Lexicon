package media

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createListeningMediaSession(): IListeningMediaSession = object : KoinComponent {
    val context: Context by inject()
}.run {
    AndroidListeningMediaSession(context)
}
