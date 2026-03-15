package com.alirezaiyan.vokab.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that triggers a daily refresh of the [DailyWordWidget].
 *
 * Scheduled as a periodic work request that runs once every 24 hours.
 * Each execution tells Glance to re-compose all widget instances, which
 * causes [DailyWordWidget.provideGlance] to fetch fresh data.
 */
class DailyWordWidgetWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        DailyWordWidget().updateAll(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily_word_widget_update"

        /**
         * Enqueue a periodic work request to refresh the widget every 24 hours.
         * Uses [ExistingPeriodicWorkPolicy.KEEP] so re-calling this method is safe
         * and won't reset the schedule.
         */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyWordWidgetWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
