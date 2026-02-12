package data.notification.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterPushTokenRequest(
    val token: String,
    val platform: Platform,
    val deviceId: String? = null
)

@Serializable
enum class Platform {
    ANDROID,
    IOS,
    WEB
}

