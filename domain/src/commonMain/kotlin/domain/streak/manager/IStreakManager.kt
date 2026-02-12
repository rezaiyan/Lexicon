package domain.streak.manager

import domain.streak.model.StreakData
import kotlinx.coroutines.flow.Flow

interface IStreakManager {
    fun getStreak(): Flow<StreakState>
    suspend fun recordActivity(): Result<StreakData>
    
    sealed interface StreakState {
        data object Loading : StreakState
        data class Error(val message: String) : StreakState
        data class Loaded(val data: StreakData) : StreakState
    }
}
