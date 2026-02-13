package domain.common

import kotlinx.coroutines.CancellationException

/**
 * Try is a replacement for kotlin.Result with some major changes:
 * - catching operators except side effects
 * - rethrows CancellationException to support coroutine cancellation
 * - rethrows java.lang.Error (JVM fatal errors) and should be rarely caught
 */
sealed class Try<out T> {

    data class Success<out T>(val value: T) : Try<T>()
    data class Failure(val throwable: Throwable) : Try<Nothing>()

    companion object {

        /**
         * Executes the given [block] and encapsulates the value into [Try.Success] if it runs successfully,
         * or encapsulates the caught [Throwable] into [Try.Failure].
         *
         * It rethrows [CancellationException] and [Error].
         */
        inline operator fun <T> invoke(block: () -> T): Try<T> {
            return try {
                success(item = block())
            } catch (error: Error) {
                throw error
            } catch (ce: CancellationException) {
                throw ce
            } catch (throwable: Throwable) {
                failure(throwable)
            }
        }

        fun <T> success(item: T): Try<T> = Success(value = item)

        fun <T> failure(throwable: Throwable): Try<T> = Failure(throwable)

        /**
         * If any item is [Failure], returns the first failure, otherwise [Success(Unit)].
         */
        fun zipUnit(vararg tries: Try<Any?>): Try<Unit> {
            for (t in tries) {
                if (t is Failure) return Failure(t.throwable)
            }
            return Success(Unit)
        }
    }

    val isFailure: Boolean
        get() = this is Failure

    val isSuccess: Boolean
        get() = this is Success
}

fun <T> Try<T>.getOrNull(): T? = if (this is Try.Success<T>) value else null

fun <T> Try<T>.getOrDefault(default: T): T = if (this is Try.Success<T>) value else default

/**
 * Returns the encapsulated value if this is [Try.Success], or throws the encapsulated throwable if this is [Try.Failure].
 */
fun <T> Try<T>.getOrThrow(): T =
    when (this) {
        is Try.Success -> value
        is Try.Failure -> throw throwable
    }

/**
 * Returns the encapsulated value if this is [Try.Success], or the result of [defaultValue] if this is [Try.Failure].
 */
inline fun <T> Try<T>.getOrElse(defaultValue: (Throwable) -> T): T =
    when (this) {
        is Try.Success -> value
        is Try.Failure -> defaultValue(throwable)
    }

fun <T> Try<T>.exceptionOrNull(): Throwable? = if (this is Try.Failure) throwable else null

/**
 * Applies [transform] to the encapsulated value if this is [Try.Success], otherwise returns the original [Try.Failure].
 *
 * This function catches any throwable thrown by [transform], except [CancellationException] and [Error].
 */
inline fun <T, R> Try<T>.map(transform: (T) -> R): Try<R> {
    return when (this) {
        is Try.Success -> try {
            Try.success(transform(value))
        } catch (error: Error) {
            throw error
        } catch (ce: CancellationException) {
            throw ce
        } catch (throwable: Throwable) {
            Try.failure(throwable)
        }

        is Try.Failure -> Try.failure(throwable)
    }
}

/**
 * Applies [transform] to the encapsulated value if this is [Try.Success], otherwise returns the original [Try.Failure].
 *
 * This function catches any throwable thrown by [transform], except [CancellationException] and [Error].
 */
inline fun <T, R> Try<T>.flatMap(transform: (T) -> Try<R>): Try<R> {
    return when (this) {
        is Try.Success -> try {
            transform(value)
        } catch (error: Error) {
            throw error
        } catch (ce: CancellationException) {
            throw ce
        } catch (throwable: Throwable) {
            Try.failure(throwable)
        }

        is Try.Failure -> Try.failure(throwable)
    }
}

inline fun <T, R> Try<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
): R =
    when (this) {
        is Try.Success -> onSuccess(value)
        is Try.Failure -> onFailure(throwable)
    }

/**
 * Performs [action] if this is [Try.Success]. Returns the original [Try] unchanged.
 *
 * This is a side effect and does not catch exceptions.
 */
inline fun <T> Try<T>.doOnSuccess(action: (T) -> Unit): Try<T> {
    if (this is Try.Success) {
        action(value)
    }
    return this
}

/**
 * Performs [action] if this is [Try.Failure]. Returns the original [Try] unchanged.
 *
 * This is a side effect and does not catch exceptions.
 */
inline fun <T> Try<T>.doOnFailure(action: (Throwable) -> Unit): Try<T> {
    if (this is Try.Failure) {
        action(throwable)
    }
    return this
}

/**
 * Alias for [doOnSuccess].
 */
inline fun <T> Try<T>.onSuccess(action: (T) -> Unit): Try<T> = doOnSuccess(action)

/**
 * Alias for [doOnFailure].
 */
inline fun <T> Try<T>.onFailure(action: (Throwable) -> Unit): Try<T> = doOnFailure(action)

/**
 * Returns the encapsulated result of the given [transform] applied to the encapsulated [Throwable]
 * if this is [Try.Failure], or the original [Try.Success] value unchanged.
 *
 * This function catches any throwable thrown by [transform], except [CancellationException] and [Error].
 */
inline fun <T> Try<T>.recover(transform: (Throwable) -> T): Try<T> {
    return when (this) {
        is Try.Success -> this
        is Try.Failure -> try {
            Try.success(transform(throwable))
        } catch (error: Error) {
            throw error
        } catch (ce: CancellationException) {
            throw ce
        } catch (throwable: Throwable) {
            Try.failure(throwable)
        }
    }
}

/**
 * Transforms the encapsulated [Throwable] if this is [Try.Failure], keeping [Try.Success] unchanged.
 */
inline fun <T> Try<T>.mapFailure(transform: (Throwable) -> Throwable): Try<T> {
    return when (this) {
        is Try.Success -> this
        is Try.Failure -> Try.failure(transform(throwable))
    }
}

/**
 * Combines two [Try] values:
 * - If both are [Try.Success], returns [Try.Success] of [combiner] result.
 * - If either is [Try.Failure], returns a [Try.Failure] (left-first).
 *
 * This function catches any throwable thrown by [combiner], except [CancellationException] and [Error].
 */
inline fun <T, R, S> Try<T>.zipWith(
    other: Try<R>,
    combine: (T, R) -> S,
): Try<S> {
    return when {
        this is Try.Success && other is Try.Success -> try {
            Try.success(combine(this.value, other.value))
        } catch (error: Error) {
            throw error
        } catch (ce: CancellationException) {
            throw ce
        } catch (throwable: Throwable) {
            Try.failure(throwable)
        }

        this is Try.Failure -> Try.failure(this.throwable)
        other is Try.Failure -> Try.failure(other.throwable)
        else -> error("Not possible, here just for exhaustiveness")
    }
}
