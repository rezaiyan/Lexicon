package expects

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

actual fun shareTextAsFile(
    title: String,
    text: String,
    filename: String
) {
    val context = object : KoinComponent {
        val context: Context by inject()
    }.context
    
    try {
        val cacheDir = File(context.cacheDir, "shared_files")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        val file = File(cacheDir, filename)
        file.writeText(text, Charsets.UTF_8)
        
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(
            Intent.createChooser(shareIntent, "Share $filename").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    } catch (e: Exception) {
        println(" Failed to share file: ${e.message}")
    }
}



