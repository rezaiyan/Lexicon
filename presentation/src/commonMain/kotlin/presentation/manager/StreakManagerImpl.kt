package presentation.manager

import domain.common.Try
import domain.common.fold
import domain.common.onSuccess
import domain.streak.model.StreakData
import domain.streak.manager.IStreakManager
import domain.streak.repository.IStreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

class StreakManagerImpl(
    private val streakRepository: IStreakRepository
) : IStreakManager {

    private val _cachedStreak = MutableStateFlow<StreakData?>(null)

    override fun getStreak(): Flow<IStreakManager.StreakState> = flow {
        emit(IStreakManager.StreakState.Loading)
        
        val result = streakRepository.getStreak()
        
        result.fold(
            onSuccess = { streakData ->
                _cachedStreak.value = streakData
                emit(IStreakManager.StreakState.Loaded(streakData))
            },
            onFailure = { error ->
                emit(IStreakManager.StreakState.Error(error.message ?: "Failed to load streak"))
            }
        )
    }

    override suspend fun recordActivity(): Try<StreakData> {
        return streakRepository.recordActivity().onSuccess { streakData ->
            _cachedStreak.value = streakData
        }
    }
}
