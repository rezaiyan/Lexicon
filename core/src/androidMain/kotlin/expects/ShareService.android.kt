package expects

import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AndroidShareService(
    private val context: Context
) : ShareService {
    override fun share(title: String, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        
        context.startActivity(
            Intent.createChooser(shareIntent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

actual fun getShareService(): ShareService {
    return object : KoinComponent {
        val context: Context by inject()
    }.let { AndroidShareService(it.context) }
}



