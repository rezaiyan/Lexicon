package widget

import domain.widget.model.DailyWidgetData
import platform.Foundation.NSUserDefaults

object IosWidgetDataWriter {

    private const val SUITE_NAME = "group.com.alirezaiyan.vokab"
    private const val KEY_WORD_ID = "widget_word_id"
    private const val KEY_WORD = "widget_word"
    private const val KEY_TRANSLATION = "widget_translation"
    private const val KEY_STREAK = "widget_streak"
    private const val KEY_DUE_COUNT = "widget_due_count"

    fun write(data: DailyWidgetData) {
        val defaults = NSUserDefaults(suiteName = SUITE_NAME)
        defaults.setInteger(data.wordId.toLong(), forKey = KEY_WORD_ID)
        defaults.setObject(data.word, forKey = KEY_WORD)
        defaults.setObject(data.translation, forKey = KEY_TRANSLATION)
        defaults.setInteger(data.streakCount.toLong(), forKey = KEY_STREAK)
        defaults.setInteger(data.dueCardCount.toLong(), forKey = KEY_DUE_COUNT)
        defaults.synchronize()
    }

    fun readWordId(): Int {
        val defaults = NSUserDefaults(suiteName = SUITE_NAME)
        return defaults.integerForKey(KEY_WORD_ID).toInt()
    }
}
