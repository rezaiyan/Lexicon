package com.alirezaiyan.vokab.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import android.graphics.Color
import androidx.glance.appwidget.cornerRadius
import com.alirezaiyan.vokab.MainActivity
import core.common.getOrNull
import domain.widget.usecase.GetDailyWidgetDataUseCase
import domain.widget.model.DailyWidgetData
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.getKoin

/**
 * Glance-based Android home screen widget that displays:
 * - A daily vocabulary word with its translation
 * - The user's current study streak
 * - Number of due cards
 *
 * Tapping the widget opens the main app.
 */
class DailyWordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = fetchWidgetData()

        provideContent {
            GlanceTheme {
                DailyWordWidgetContent(widgetData)
            }
        }
    }

    private fun fetchWidgetData(): DailyWidgetData? {
        return runBlocking {
            @Suppress("TooGenericExceptionCaught")
            try {
                val useCase: GetDailyWidgetDataUseCase = getKoin().get()
                useCase(Unit).getOrNull()
            } catch (_: Exception) {
                // Koin may not be initialized if the app hasn't been opened yet
                null
            }
        }
    }
}

@Composable
private fun DailyWordWidgetContent(data: DailyWidgetData?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color.WHITE, Color.parseColor("#1C1B1F")))
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start
    ) {
        if (data != null) {
            // Header row: app name + streak
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lexicon",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.parseColor("#6C21DC"),
                            Color.parseColor("#B388FF")
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "\uD83D\uDD25 ${data.streakCount}",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.parseColor("#333333"),
                            Color.parseColor("#E0E0E0")
                        ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // Daily word label
            Text(
                text = "Word of the Day",
                style = TextStyle(
                    color = ColorProvider(
                        Color.parseColor("#888888"),
                        Color.parseColor("#9E9E9E")
                    ),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // The word itself
            Text(
                text = data.word,
                style = TextStyle(
                    color = ColorProvider(Color.BLACK, Color.WHITE),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Translation
            Text(
                text = data.translation,
                style = TextStyle(
                    color = ColorProvider(
                        Color.parseColor("#555555"),
                        Color.parseColor("#BDBDBD")
                    ),
                    fontSize = 16.sp
                ),
                maxLines = 1
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Due cards footer
            if (data.dueCardCount > 0) {
                Text(
                    text = "${data.dueCardCount} cards due for review",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.parseColor("#6C21DC"),
                            Color.parseColor("#B388FF")
                        ),
                        fontSize = 12.sp
                    )
                )
            } else {
                Text(
                    text = "All caught up!",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.parseColor("#4CAF50"),
                            Color.parseColor("#81C784")
                        ),
                        fontSize = 12.sp
                    )
                )
            }
        } else {
            // Fallback when no data is available
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Lexicon",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.parseColor("#6C21DC"),
                            Color.parseColor("#B388FF")
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Text(
                    text = "Open app to set up your words",
                    style = TextStyle(
                        color = ColorProvider(
                            Color.parseColor("#888888"),
                            Color.parseColor("#9E9E9E")
                        ),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}
