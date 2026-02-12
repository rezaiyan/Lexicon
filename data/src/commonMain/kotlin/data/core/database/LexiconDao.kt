@file:OptIn(ExperimentalTime::class)

package data.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Dao
interface LexiconDao {

    @Insert
    suspend fun insert(item: WordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<WordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: WordEntity)

    @Query("SELECT * FROM WordEntity")
    fun getAllAsFlow(): Flow<List<WordEntity>>

    @Query("SELECT * FROM WordEntity")
    suspend fun getAllAsync(): List<WordEntity>

    @Query("SELECT * FROM WordEntity")
    fun getAll(): Flow<List<WordEntity>>

    @Query("SELECT * FROM WordEntity")
    suspend fun getAllOnce(): List<WordEntity>

    @Query(
        """
        SELECT * FROM WordEntity 
        WHERE nextReviewDate <= :currentTime 
        ORDER BY 
            CASE WHEN lastReviewDate = 0 THEN 0 ELSE 1 END,
            lastReviewDate ASC
    """
    )
    fun getDueCards(currentTime: Long = Clock.System.now().toEpochMilliseconds()): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM WordEntity 
        WHERE level = :level 
        ORDER BY 
            CASE WHEN nextReviewDate <= :currentTime THEN 0 ELSE 1 END,
            CASE 
                WHEN nextReviewDate <= :currentTime THEN nextReviewDate 
                ELSE -lastReviewDate 
            END ASC
    """
    )
    fun getWordsByLevel(level: Int, currentTime: Long = Clock.System.now().toEpochMilliseconds()): Flow<List<WordEntity>>

    @Query(
        """
        SELECT * FROM WordEntity 
        WHERE level >= :minLevel AND level <= :maxLevel 
        ORDER BY 
            CASE WHEN nextReviewDate <= :currentTime THEN 0 ELSE 1 END,
            CASE 
                WHEN nextReviewDate <= :currentTime THEN nextReviewDate 
                ELSE -lastReviewDate 
            END ASC
    """
    )
    fun getWordsByLevelRange(
        minLevel: Int,
        maxLevel: Int,
        currentTime: Long = Clock.System.now().toEpochMilliseconds()
    ): Flow<List<WordEntity>>

    @Query("SELECT * FROM WordEntity WHERE id = :id")
    suspend fun getWordById(id: Long): WordEntity?

    @Query("SELECT * FROM WordEntity WHERE LOWER(TRIM(originalWord)) = LOWER(TRIM(:originalWord)) AND LOWER(TRIM(translation)) = LOWER(TRIM(:translation))")
    suspend fun findWordByContent(originalWord: String, translation: String): WordEntity?

    @Query("DELETE FROM WordEntity WHERE id = :id")
    suspend fun deleteWord(id: Long)

    @Query("DELETE FROM WordEntity WHERE id IN (:ids)")
    suspend fun deleteWords(ids: List<Long>): Int

    @Query("UPDATE WordEntity SET originalWord = :originalWord, translation = :translation, description = :description WHERE id = :id")
    suspend fun updateWordContent(id: Long, originalWord: String, translation: String, description: String)

    @Query("SELECT count(*) FROM WordEntity")
    suspend fun count(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE nextReviewDate <= :currentTime")
    suspend fun countDueCards(currentTime: Long = Clock.System.now().toEpochMilliseconds()): Int

    // 7-Level System: Individual count for each level (0-6)
    @Query("SELECT count(*) FROM WordEntity WHERE level = 0")
    suspend fun countLevel0(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE level = 1")
    suspend fun countLevel1(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE level = 2")
    suspend fun countLevel2(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE level = 3")
    suspend fun countLevel3(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE level = 4")
    suspend fun countLevel4(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE level = 5")
    suspend fun countLevel5(): Int

    @Query("SELECT count(*) FROM WordEntity WHERE level = 6")
    suspend fun countLevel6(): Int

    @Query(
        "UPDATE WordEntity SET level = :level," +
            "easeFactor = :easeFactor," +
            "interval = :interval," +
            "repetitions = :repetitions," +
            "lastReviewDate = :lastReviewDate," +
            "nextReviewDate = :nextReviewDate WHERE id = :id"
    )
    suspend fun updateProgress(
        id: Int,
        level: Int,
        easeFactor: Float,
        interval: Int,
        repetitions: Int,
        lastReviewDate: Long,
        nextReviewDate: Long
    )

    // Settings
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM SettingsEntity WHERE id = 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM SettingsEntity WHERE id = 1")
    suspend fun getSettingsOnce(): SettingsEntity?

    @Query("UPDATE SettingsEntity SET lastInsightDate = :date, cachedInsight = :insight WHERE id = 1")
    suspend fun updateDailyInsight(date: String, insight: String)

    @Query("SELECT lastInsightDate FROM SettingsEntity WHERE id = 1")
    suspend fun getLastInsightDate(): String?

    @Query("SELECT cachedInsight FROM SettingsEntity WHERE id = 1")
    suspend fun getCachedInsight(): String?

    // Clear local data
    @Query("DELETE FROM WordEntity")
    suspend fun deleteAllWords()

    @Query("DELETE FROM SettingsEntity")
    suspend fun clearSettings()

    // Downloaded Collections
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedCollection(collection: DownloadedCollectionEntity)

    @Query("SELECT * FROM DownloadedCollectionEntity")
    fun getAllDownloadedCollections(): Flow<List<DownloadedCollectionEntity>>

    @Query("SELECT * FROM DownloadedCollectionEntity WHERE targetLanguage = :targetLanguage AND originLanguage = :originLanguage AND fileName = :fileName")
    suspend fun getDownloadedCollection(
        targetLanguage: String,
        originLanguage: String,
        fileName: String
    ): DownloadedCollectionEntity?

    @Query("DELETE FROM DownloadedCollectionEntity")
    suspend fun clearAllDownloadedCollections()

    @Query(
        """
        SELECT
          SUM(CASE WHEN level = 0 THEN 1 ELSE 0 END) AS level0Count,
          SUM(CASE WHEN level = 1 THEN 1 ELSE 0 END) AS level1Count,
          SUM(CASE WHEN level = 2 THEN 1 ELSE 0 END) AS level2Count,
          SUM(CASE WHEN level = 3 THEN 1 ELSE 0 END) AS level3Count,
          SUM(CASE WHEN level = 4 THEN 1 ELSE 0 END) AS level4Count,
          SUM(CASE WHEN level = 5 THEN 1 ELSE 0 END) AS level5Count,
          SUM(CASE WHEN level = 6 THEN 1 ELSE 0 END) AS level6Count,
          COUNT(*)                                     AS totalWords,
          SUM(CASE WHEN nextReviewDate <= :currentTime THEN 1 ELSE 0 END) AS dueCards
        FROM WordEntity
    """
    )
    fun progressRowFlow(currentTime: Long): Flow<ProgressRow>

}

@Entity
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val originalWord: String,
    val translation: String,
    val description: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val level: Int = 0, // Mastery level
    val easeFactor: Float = 2.5f, // Ease factor for spacing
    val interval: Int = 0, // Days between reviews
    val repetitions: Int = 0, // Successful review count
    val lastReviewDate: Long = 0L, // Last review timestamp
    val nextReviewDate: Long = Clock.System.now().toEpochMilliseconds() - 1000, // Next review due date - set to 1 second ago to ensure immediate due status
    val dateAdded: Long = Clock.System.now().toEpochMilliseconds() // Timestamp when word was added
)

@Entity
data class SettingsEntity(
    @PrimaryKey val id: Int = 1, // Only one settings row
    val languageCode: String = "en",
    val themeMode: String = "AUTO", // AUTO, LIGHT, DARK
    val lastInsightDate: String? = null,
    val cachedInsight: String? = null,
    val lastInsightDismissedTime: Long = 0L, // Timestamp when user dismissed insight
    // Notification settings
    val notificationsEnabled: Boolean = true,
    val reviewReminders: Boolean = true,
    val motivationalMessages: Boolean = true,
    val dailyReminderTime: String = "18:00",
    val minimumDueCards: Int = 5,
    // Review/Learning settings
    val successesToAdvance: Int = 1, // How many consecutive successes needed to advance (1-3)
    val forgotPenalty: Int = 2 // How many levels to drop when forgetting (1-3)
)

@Entity
data class DownloadedCollectionEntity(
    @PrimaryKey val id: String, // Composite: targetLanguage_originLanguage_fileName
    val targetLanguage: String,
    val originLanguage: String,
    val title: String,
    val fileName: String,
    val path: String,
    val downloadedAt: Long = Clock.System.now().toEpochMilliseconds()
)

// Data class for optimized progress stats query results
data class ProgressRow(
    val level0Count: Int = 0,
    val level1Count: Int = 0,
    val level2Count: Int = 0,
    val level3Count: Int = 0,
    val level4Count: Int = 0,
    val level5Count: Int = 0,
    val level6Count: Int = 0,
    val totalWords: Int = 0,
    val dueCards: Int = 0
)

