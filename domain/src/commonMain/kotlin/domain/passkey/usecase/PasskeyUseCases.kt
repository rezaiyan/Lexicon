package domain.passkey.usecase

import core.common.Try
import core.common.UseCase
import core.common.flatMap
import core.common.mapFailure
import core.common.onSuccess
import core.error.DomainError
import domain.auth.model.AuthUser
import domain.auth.repository.IAuthRepository
import domain.passkey.IPasskeyAuthenticator
import domain.passkey.model.AccountPasskeys
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

/**
 * The account's passkeys. Also tells this device's password manager which ones still exist, so
 * passkeys deleted on another device stop showing up in its sign-in sheet.
 */
class GetPasskeysUseCase(
    private val repository: IPasskeyRepository,
    private val authenticator: IPasskeyAuthenticator,
) : UseCase<Unit, AccountPasskeys> {
    override suspend fun invoke(params: Unit): Try<AccountPasskeys> =
        repository.getPasskeys().onSuccess { account ->
            val userHandle = account.userHandle ?: return@onSuccess
            // Best effort: the list is right whether or not the password manager listens
            authenticator.keepOnly(account.rpId, userHandle, account.passkeys.map { it.credentialId })
        }
}

/** Deletes the passkey from the account, then asks this device's password manager to forget it. */
class DeletePasskeyUseCase(
    private val repository: IPasskeyRepository,
    private val authenticator: IPasskeyAuthenticator,
) : UseCase<DeletePasskeyUseCase.Params, Unit> {
    /** [rpId] from [AccountPasskeys]; null skips the password manager (the server delete still happens). */
    data class Params(val passkey: Passkey, val rpId: String?)

    override suspend fun invoke(params: Params): Try<Unit> =
        repository.delete(params.passkey.id).onSuccess {
            // Best effort: where it can't, the user removes it in the password manager
            params.rpId?.let { authenticator.forget(it, params.passkey.credentialId) }
        }
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
            .flatMap { signIn ->
                authRepository.loginWithPasskey(signIn.responseJson).mapFailure { error ->
                    when {
                        // The server doesn't know the passkey (deleted): stop the device offering it
                        error is DomainError.Network.ServerError && error.code == HTTP_NOT_FOUND -> {
                            authenticator.forget(signIn.rpId, signIn.credentialId)
                            DomainError.Passkey.Removed
                        }
                        // A 401 here is the server refusing the passkey, not a missing session
                        error is DomainError.Auth -> DomainError.Passkey.Rejected
                        else -> error
                    }
                }
            }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
