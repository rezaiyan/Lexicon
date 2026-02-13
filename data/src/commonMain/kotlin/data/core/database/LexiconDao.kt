@file:OptIn(ExperimentalTime::class)

package data.core.database

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Data mapping types used between the SQLDelight-generated types and the domain layer.
 * These mirror the SQLDelight-generated types but with Kotlin-friendly types (Int, Float, Boolean).
 */

data class WordEntityData(
    val id: Int = 0,
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: Int = 0,
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val lastReviewDate: Long = 0L,
    val nextReviewDate: Long = Clock.System.now().toEpochMilliseconds() - 1000,
    val dateAdded: Long = Clock.System.now().toEpochMilliseconds()
)

data class SettingsEntityData(
    val id: Int = 1,
    val languageCode: String = "en",
    val themeMode: String = "AUTO",
    val lastInsightDate: String? = null,
    val cachedInsight: String? = null,
    val lastInsightDismissedTime: Long = 0L,
    val notificationsEnabled: Boolean = true,
    val reviewReminders: Boolean = true,
    val motivationalMessages: Boolean = true,
    val dailyReminderTime: String = "18:00",
    val minimumDueCards: Int = 5
)