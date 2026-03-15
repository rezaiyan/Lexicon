package widget

import domain.widget.IWidgetRefresher
import domain.widget.model.DailyWidgetData

class IosWidgetRefresher : IWidgetRefresher {

    override suspend fun getDisplayedWordId(): Int? {
        val id = IosWidgetDataWriter.readWordId()
        return if (id == -1) null else id
    }

    override suspend fun push(data: DailyWidgetData) {
        IosWidgetDataWriter.write(data)
    }
}
