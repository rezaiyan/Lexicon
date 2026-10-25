package feature.auth

/** One-time messages from [AuthViewModel] to the sign-in screen. */
sealed interface AuthEffect {
    /** Passkey sign-in found no passkey on this device: a hint, not an error. */
    data object NoPasskeyOnDevice : AuthEffect

    /** The picked passkey was deleted from the account; the device was asked to forget it. */
    data object PasskeyRemoved : AuthEffect
}
