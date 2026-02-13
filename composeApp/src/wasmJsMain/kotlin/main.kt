import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.ComposeViewport
import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import auth.jsFirebaseInit
import config.AppConfig
import config.FirebaseWebConfig
import data.core.database.LexiconDatabase
import data.notification.remote.model.Platform
import di.appModule
import di.wasmJsPlatformModule
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import lexicon.design_system.generated.resources.Res
import lexicon.design_system.generated.resources.noto_sans_medium
import lexicon.design_system.generated.resources.noto_sans_regular
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.preloadFont
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class, ExperimentalResourceApi::class, kotlin.js.ExperimentalWasmJsInterop::class)
fun main() {
    jsFirebaseInit(
        apiKey = FirebaseWebConfig.API_KEY,
        authDomain = FirebaseWebConfig.AUTH_DOMAIN,
        projectId = FirebaseWebConfig.PROJECT_ID
    )

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
            val fontRegular by preloadFont(Res.font.noto_sans_regular, FontWeight.Normal)
            val fontMedium by preloadFont(Res.font.noto_sans_medium, FontWeight.Medium)
            val fontsReady = fontRegular != null && fontMedium != null

            if (fontsReady) {
                presentation.ui.LexiconApp()
                LaunchedEffect(Unit) { jsHideLoader() }
            }
        }
    }
}

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun jsHideLoader(): JsAny? =
    js("window._hideLoader && window._hideLoader()")
