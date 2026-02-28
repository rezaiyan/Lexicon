package expects

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual fun openUrl(url: String) {
    val context = object : KoinComponent {
        val context: Context by inject()
    }.context
    
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        println(" Failed to open URL: ${e.message}")
    }
}

