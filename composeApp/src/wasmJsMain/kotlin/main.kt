import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import data.notification.remote.model.Platform
import di.appModule
import kotlinx.browser.document
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(
            appModule(
                backendUrl = "https://api.lexicon.app/api/v1",
                platform = Platform.WEB
            )
        )
    }

    val root = document.getElementById("root") ?: return
    ComposeViewport(root) {
        presentation.ui.LexiconApp()
    }
}
