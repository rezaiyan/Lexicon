package domain.passkey.model

/** A passkey the learner created for their account; lets them sign in without Google or Apple. */
data class Passkey(
    val id: Long,
    /** Base64url WebAuthn credential id: how the device's password manager knows this passkey. */
    val credentialId: String,
    val createdAtMillis: Long,
    /** Null until it is first used to sign in. */
    val lastUsedAtMillis: Long?,
    /** Backed up by the password manager (iCloud Keychain, Google Password Manager), so it survives a lost phone. */
    val synced: Boolean,
)

/** The account's passkeys, with what the device's password manager needs to drop deleted ones. */
data class AccountPasskeys(
    /** The domain the passkeys belong to. */
    val rpId: String,
    /** Base64url user handle shared by all of the account's passkeys; null when there are none. */
    val userHandle: String?,
    val passkeys: List<Passkey>,
)

/** What the device's passkey sheet returned for a sign-in. */
data class PasskeySignIn(
    /** The response for the server to verify. */
    val responseJson: String,
    /** Base64url id of the passkey the user picked. */
    val credentialId: String,
    /** The domain the sign-in was for. */
    val rpId: String,
)
