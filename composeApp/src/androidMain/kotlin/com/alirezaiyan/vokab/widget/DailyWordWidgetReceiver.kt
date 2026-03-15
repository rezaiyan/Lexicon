package com.alirezaiyan.vokab.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Broadcast receiver that serves the [DailyWordWidget] to the Android launcher.
 * Registered in AndroidManifest.xml with the widget metadata.
 */
class DailyWordWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWordWidget()
}
