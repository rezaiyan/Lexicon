package tts

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun createModelFileManager(): IModelFileManager {
    return object : KoinComponent {
        val context: Context by inject()
    }.run {
        AndroidModelFileManager(context)
    }
}
