import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import config.AppConfig
import data.core.database.LexiconDatabase
import data.notification.remote.model.Platform
import di.appModule
import di.wasmJsPlatformModule
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(
            appModule(
                backendUrl = AppConfig.VOKAB_BACKEND_URL,
                platform = Platform.WEB
            ),
            wasmJsPlatformModule()
        )
    }

    MainScope().launch {
        val driver = GlobalContext.get().get<SqlDriver>()
        LexiconDatabase.Schema.awaitCreate(driver)

        val root = document.getElementById("root") ?: return@launch
        ComposeViewport(root) {
            presentation.ui.LexiconApp()
        }
    }
}
