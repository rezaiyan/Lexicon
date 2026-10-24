package domain.passkey.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.common.mapFailure
import core.error.DomainError
import domain.auth.model.AuthUser
import domain.auth.repository.IAuthRepository
import domain.passkey.IPasskeyAuthenticator
import domain.passkey.model.Passkey
import domain.passkey.repository.IPasskeyRepository

class IsPasskeyAvailableUseCase(
    private val authenticator: IPasskeyAuthenticator,
) : UseCase<Unit, Boolean> {
    override suspend fun invoke(params: Unit): Try<Boolean> = Try.success(authenticator.isAvailable())
}

/** Server options → device passkey sheet → server stores the new passkey. */
class CreatePasskeyUseCase(
    private val repository: IPasskeyRepository,
    private val authenticator: IPasskeyAuthenticator,
) : UseCase<Unit, Passkey> {
    override suspend fun invoke(params: Unit): Try<Passkey> =
        repository.registrationOptions()
            .flatMap { options -> authenticator.create(options) }
            .flatMap { response -> repository.register(response) }
}

class GetPasskeysUseCase(
    private val repository: IPasskeyRepository,
) : UseCase<Unit, List<Passkey>> {
    override suspend fun invoke(params: Unit): Try<List<Passkey>> = repository.getPasskeys()
}

/** Takes the passkey id. The copy in the password manager stays until the user removes it there. */
class DeletePasskeyUseCase(
    private val repository: IPasskeyRepository,
) : UseCase<Long, Unit> {
    override suspend fun invoke(params: Long): Try<Unit> = repository.delete(params)
}

/** Server options → device passkey sheet → server verifies and starts a session. */
class SignInWithPasskeyUseCase(
    private val repository: IPasskeyRepository,
    private val authenticator: IPasskeyAuthenticator,
    private val authRepository: IAuthRepository,
) : UseCase<Unit, AuthUser> {
    override suspend fun invoke(params: Unit): Try<AuthUser> =
        repository.signInOptions()
            // Options need no session, so a 401 means the server can't do passkeys (not "sign in first")
            .mapFailure { error ->
                if (error is DomainError.Auth) DomainError.Passkey.Failed("No sign-in options") else error
            }
            .flatMap { options -> authenticator.signIn(options) }
            .flatMap { response ->
                // A 401 here is the server refusing the passkey, not a missing session
                authRepository.loginWithPasskey(response)
                    .mapFailure { error -> if (error is DomainError.Auth) DomainError.Passkey.Rejected else error }
            }
}
