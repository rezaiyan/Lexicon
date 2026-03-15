package com.alirezaiyan.vokab.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import domain.widget.IWidgetRefresher
import domain.widget.model.DailyWidgetData

class AndroidWidgetRefresher(
    private val context: Context
) : IWidgetRefresher {

    override suspend fun getDisplayedWordId(): Int? {
        val id = prefs().getInt(DailyWordWidget.KEY_WORD_ID, -1)
        return if (id == -1) null else id
    }

    override suspend fun push(data: DailyWidgetData) {
        prefs().edit()
            .putInt(DailyWordWidget.KEY_WORD_ID, data.wordId)
            .putString(DailyWordWidget.KEY_WORD, data.word)
            .putString(DailyWordWidget.KEY_TRANSLATION, data.translation)
            .putInt(DailyWordWidget.KEY_STREAK, data.streakCount)
            .putInt(DailyWordWidget.KEY_DUE_COUNT, data.dueCardCount)
            .apply()
        DailyWordWidget().updateAll(context)
    }

    private fun prefs() =
        context.getSharedPreferences(DailyWordWidget.PREFS_NAME, Context.MODE_PRIVATE)
}
