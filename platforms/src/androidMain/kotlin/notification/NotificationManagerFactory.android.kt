package notification

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createNotificationManager(): INotificationManager {
    return object : KoinComponent {
        val context: Context by inject()
    }.run {
        AndroidNotificationManager(context)
    }
}

