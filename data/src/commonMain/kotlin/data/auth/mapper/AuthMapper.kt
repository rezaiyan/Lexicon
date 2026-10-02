package data.auth.mapper

import data.auth.remote.model.UserDto
import domain.auth.model.AuthUser
import domain.auth.model.SubscriptionStatus

internal fun UserDto.toDomain(): AuthUser {
    return AuthUser(
        id = this.id,
        email = this.email,
        name = this.name,
        // Unknown values from a newer server must not crash login
        subscriptionStatus = SubscriptionStatus.entries.firstOrNull { it.name == this.subscriptionStatus }
            ?: SubscriptionStatus.FREE,
        subscriptionExpiresAt = this.subscriptionExpiresAt,
        currentStreak = this.currentStreak,
        displayAlias = this.displayAlias,
        profileImageUrl = this.profileImageUrl
    )
}

