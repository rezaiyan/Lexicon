package domain.widget

import domain.widget.model.DailyWidgetData

/**
 * Platform abstraction for pushing data to the home screen widget
 * and tracking which word is currently displayed.
 */
interface IWidgetRefresher {
    suspend fun getDisplayedWordId(): Int?
    suspend fun push(data: DailyWidgetData)
}
