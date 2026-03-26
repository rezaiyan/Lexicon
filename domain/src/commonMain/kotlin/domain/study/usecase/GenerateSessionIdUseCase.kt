package domain.study.usecase

import kotlin.random.Random
import kotlin.time.Clock

class GenerateSessionIdUseCase(private val random: Random = Random.Default) {

    operator fun invoke(): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val suffix = random.nextInt(0, 1_000_000).toString().padStart(6, '0')
        return "$timestamp-$suffix"
    }
}
