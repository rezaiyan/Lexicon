package feature.profile.model


data class ProfileUserUiModel(
    val name: String,
    val email: String,
    val displayAlias: String? = null
) {
    /** The alias the user picked, else their name; `null` when they have neither (Apple shared none). */
    val shownName: String?
        get() = displayAlias?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() }

    /** A Sign in with Apple relay address, or the backend's stand-in when Apple shared none: not one the user knows. */
    val isEmailHiddenByApple: Boolean
        get() = email.substringAfterLast('@').lowercase() in HIDDEN_APPLE_EMAIL_DOMAINS
}

// Apple's relay domains (the original, and the one it moved new addresses to in 2026) and the backend's stand-in
private val HIDDEN_APPLE_EMAIL_DOMAINS = setOf("privaterelay.appleid.com", "private.icloud.com", "apple.hidden")
