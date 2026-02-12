package expects

import android.util.Log

actual fun logNetwork(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun logPlatform(tag: String, message: String) {
    Log.d(tag, message)
}



