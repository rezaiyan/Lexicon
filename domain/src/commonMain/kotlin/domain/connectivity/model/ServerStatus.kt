package domain.connectivity.model

/**
 * Whether Lexicon's server can be reached right now. The app keeps working offline either way
 * (words and reviews live on the device); only server-backed features wait for [Online].
 */
enum class ServerStatus {
    /** Not known yet: the first check is still running. */
    Checking,

    Online,

    /** The device has no network connection. */
    Offline,

    /** The device is online but the server isn't answering, or answers that it isn't ready. */
    Unavailable,
}
