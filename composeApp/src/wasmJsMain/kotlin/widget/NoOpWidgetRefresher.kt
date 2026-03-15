package widget

import domain.widget.IWidgetRefresher
import domain.widget.model.DailyWidgetData

object NoOpWidgetRefresher : IWidgetRefresher {
    override suspend fun getDisplayedWordId(): Int? = null
    override suspend fun push(data: DailyWidgetData) = Unit
}
