package data.core.network.error

class AuthenticationException(message: String, val statusCode: Int = 401) : Exception(message)
class ServerException(message: String, val statusCode: Int = 500) : Exception(message)
open class NetworkException(message: String) : Exception(message)
class TimeoutException(message: String) : NetworkException(message)

