package data.passkey

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import core.common.Try
import data.core.database.LexiconQueries
import domain.passkey.model.PasskeyAccountName
import domain.passkey.model.PasskeySignalsSent
import domain.passkey.repository.IPasskeySignalRepository

class PasskeySignalRepositoryImpl(
    private val queries: LexiconQueries,
) : IPasskeySignalRepository {

    override suspend fun lastSent(): Try<PasskeySignalsSent?> = Try {
        queries.getPasskeySignalState().awaitAsOneOrNull()?.let { row ->
            PasskeySignalsSent(
                rpId = row.rpId,
                userHandle = row.userHandle,
                credentialIds = row.credentialIds?.split(ID_SEPARATOR)?.filter { it.isNotEmpty() }?.toSet(),
                accountName = row.accountName?.let { PasskeyAccountName(it, row.accountDisplayName ?: it) },
            )
        }
    }

    override suspend fun save(sent: PasskeySignalsSent): Try<Unit> = Try {
        queries.savePasskeySignalState(
            rpId = sent.rpId,
            userHandle = sent.userHandle,
            // Base64url ids never contain a comma
            credentialIds = sent.credentialIds?.sorted()?.joinToString(ID_SEPARATOR),
            accountName = sent.accountName?.name,
            accountDisplayName = sent.accountName?.displayName,
        )
    }

    private companion object {
        const val ID_SEPARATOR = ","
    }
}
