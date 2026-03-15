package widget

import domain.widget.model.DailyWidgetData
import platform.Foundation.NSUserDefaults
import platform.WidgetKit.WidgetCenter

/**
 * Writes widget data into the shared App Group UserDefaults so the
 * WidgetKit extension can read it.
 *
 * Must be called from the main app whenever word or streak data changes
 * (e.g., after sync, after a study session, on app launch).
 *
 * Requires the App Group "group.com.alirezaiyan.vokab" to be configured
 * on both the main app target and the widget extension target.
 */
object IosWidgetDataWriter {

    private const val SUITE_NAME = "group.com.alirezaiyan.vokab"
    private const val KEY_WORD = "widget_word"
    private const val KEY_TRANSLATION = "widget_translation"
    private const val KEY_STREAK = "widget_streak"
    private const val KEY_DUE_COUNT = "widget_due_count"

    fun write(data: DailyWidgetData) {
        val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return
        defaults.setObject(data.word, forKey = KEY_WORD)
        defaults.setObject(data.translation, forKey = KEY_TRANSLATION)
        defaults.setInteger(data.streakCount.toLong(), forKey = KEY_STREAK)
        defaults.setInteger(data.dueCardCount.toLong(), forKey = KEY_DUE_COUNT)
        defaults.synchronize()

        // Tell WidgetKit to refresh the widget timeline
        WidgetCenter.shared.reloadAllTimelines()
    }
}
