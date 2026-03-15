package com.alirezaiyan.vokab.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.alirezaiyan.vokab.MainActivity
import domain.widget.model.DailyWidgetData

// Brand colors from the app's design system (AppColors + AppTheme)
private val Primary = ColorProvider(Color(0xFF7F5AF0), Color(0xFFA78BFA))
private val OnSurface = ColorProvider(Color(0xFF1E1E1E), Color(0xFFFFFFFE))
private val OnSurfaceVariant = ColorProvider(Color(0xFF6B7280), Color(0xFFCCCCCC))
private val Surface = ColorProvider(Color(0xFFFFFFFF), Color(0xFF1E1E1E))
private val SurfaceContainer = ColorProvider(Color(0xFFF5F4F1), Color(0xFF2C2C2C))
private val Success = ColorProvider(Color(0xFF2CB67D), Color(0xFF4ADE80))

class DailyWordWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetData = fetchWidgetData(context)

        provideContent {
            GlanceTheme {
                DailyWordWidgetContent(widgetData)
            }
        }
    }

    private fun fetchWidgetData(context: Context): DailyWidgetData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val word = prefs.getString(KEY_WORD, null) ?: return null
        val translation = prefs.getString(KEY_TRANSLATION, null) ?: return null
        return DailyWidgetData(
            wordId = prefs.getInt(KEY_WORD_ID, -1),
            word = word,
            translation = translation,
            streakCount = prefs.getInt(KEY_STREAK, 0),
            dueCardCount = prefs.getInt(KEY_DUE_COUNT, 0),
        )
    }

    companion object {
        internal const val PREFS_NAME = "lexicon_widget"
        internal const val KEY_WORD_ID = "displayed_word_id"
        internal const val KEY_WORD = "displayed_word"
        internal const val KEY_TRANSLATION = "displayed_translation"
        internal const val KEY_STREAK = "displayed_streak"
        internal const val KEY_DUE_COUNT = "displayed_due_count"
    }
}

@Composable
private fun DailyWordWidgetContent(data: DailyWidgetData?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Surface)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
            .padding(20.dp),
        verticalAlignment = Alignment.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        if (data != null) {
            // Overline label
            Text(
                text = "WORD OF THE DAY",
                style = TextStyle(
                    color = OnSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            // The word
            Text(
                text = data.word,
                style = TextStyle(
                    color = OnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Translation
            Text(
                text = data.translation,
                style = TextStyle(
                    color = OnSurfaceVariant,
                    fontSize = 15.sp,
                ),
                maxLines = 1,
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Footer chips
            FooterChips(data)
        } else {
            EmptyContent()
        }
    }
}

@Composable
private fun FooterChips(data: DailyWidgetData) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(
            text = "${data.streakCount} day streak",
            textColor = Primary,
            backgroundColor = SurfaceContainer,
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        if (data.dueCardCount > 0) {
            Chip(
                text = "${data.dueCardCount} due",
                textColor = Primary,
                backgroundColor = SurfaceContainer,
            )
        } else {
            Chip(
                text = "All done",
                textColor = Success,
                backgroundColor = SurfaceContainer,
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Lexicon",
            style = TextStyle(
                color = Primary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "Open to set up your words",
            style = TextStyle(
                color = OnSurfaceVariant,
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun Chip(
    text: String,
    textColor: GlanceColorProvider,
    backgroundColor: GlanceColorProvider,
) {
    Row(
        modifier = GlanceModifier
            .background(backgroundColor)
            .cornerRadius(20.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
