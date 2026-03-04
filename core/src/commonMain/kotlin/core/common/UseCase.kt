package core.common

import kotlinx.coroutines.flow.Flow

/**
 * Base contract for one-shot suspend use cases.
 * Every fallible operation returns [Try] — never bare types for ops that can fail.
 * Use cases must be stateless: no mutable fields.
 *
 * @param P input parameter type (use [Unit] for parameterless use cases via [NoParamUseCase])
 * @param R success value type wrapped in [Try]
 */
fun interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Try<R>
}

/**
 * Base contract for reactive stream use cases.
 * Returns a cold [Flow] — error handling is done downstream (e.g. `catch` in the ViewModel).
 * Use cases must be stateless: no mutable fields.
 *
 * @param P input parameter type (use [Unit] for parameterless use cases via [NoParamFlowUseCase])
 * @param R element type emitted by the [Flow]
 */
fun interface FlowUseCase<in P, out R> {
    operator fun invoke(params: P): Flow<R>
}

/** Convenience alias for suspend use cases that take no parameters. */
typealias NoParamUseCase<R> = UseCase<Unit, R>

/** Convenience alias for flow use cases that take no parameters. */
typealias NoParamFlowUseCase<R> = FlowUseCase<Unit, R>
