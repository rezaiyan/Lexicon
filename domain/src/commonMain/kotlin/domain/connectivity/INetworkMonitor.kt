package domain.connectivity

import kotlinx.coroutines.flow.Flow

/** The device's own network connection, as the OS reports it. Says nothing about the server. */
interface INetworkMonitor {
    /** Emits the current state on collection, then every change. */
    val isConnected: Flow<Boolean>
}
