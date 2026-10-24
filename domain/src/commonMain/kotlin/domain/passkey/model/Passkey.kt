package domain.passkey.model

/** A passkey the learner created for their account; lets them sign in without Google or Apple. */
data class Passkey(
    val id: Long,
    val createdAtMillis: Long,
    /** Null until it is first used to sign in. */
    val lastUsedAtMillis: Long?,
    /** Backed up by the password manager (iCloud Keychain, Google Password Manager), so it survives a lost phone. */
    val synced: Boolean,
)
