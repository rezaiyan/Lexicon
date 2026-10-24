package data.passkey

import core.common.Try
import core.common.map
import data.passkey.remote.IPasskeyRemoteDataSource
import data.passkey.remote.PasskeyResponse
import domain.passkey.model.Passkey
import domain.passkey.repository.IPasskeyRepository

class PasskeyRepositoryImpl(
    private val remote: IPasskeyRemoteDataSource,
) : IPasskeyRepository {

    override suspend fun registrationOptions(): Try<String> = remote.registrationOptions()

    override suspend fun register(responseJson: String): Try<Passkey> =
        remote.register(responseJson).map { it.toDomain() }

    override suspend fun getPasskeys(): Try<List<Passkey>> =
        remote.getPasskeys().map { passkeys -> passkeys.map { it.toDomain() } }

    override suspend fun delete(id: Long): Try<Unit> = remote.delete(id)

    override suspend fun signInOptions(): Try<String> = remote.signInOptions()
}

internal fun PasskeyResponse.toDomain() = Passkey(
    id = id,
    createdAtMillis = createdAt,
    lastUsedAtMillis = lastUsedAt,
    synced = synced,
)
