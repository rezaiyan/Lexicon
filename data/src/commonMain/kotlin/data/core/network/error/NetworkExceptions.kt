package data.core.network.error

class AuthenticationException(message: String, val statusCode: Int = 401) : Exception(message)
class ServerException(message: String, val statusCode: Int = 500) : Exception(message)
open class NetworkException(message: String) : Exception(message)
class TimeoutException(message: String) : NetworkException(message)

/** HTTP 402: the feature needs premium. Not an auth error — the session stays valid. */
class PremiumRequiredException(message: String) : Exception(message)

