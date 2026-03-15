package widget

import core.common.getOrNull
import domain.widget.usecase.GetDailyWidgetDataUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.Koin

/**
 * Fetches the daily widget data via the shared use case and writes it
 * to the App Group UserDefaults for the WidgetKit extension to consume.
 *
 * Called from [MainViewController] on app launch.
 */
object IosWidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun update(koin: Koin) {
        scope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                val useCase: GetDailyWidgetDataUseCase = koin.get()
                val data = useCase(Unit).getOrNull() ?: return@launch
                IosWidgetDataWriter.write(data)
            } catch (_: Exception) {
                // Silently fail - widget will show empty/stale state
            }
        }
    }
}
