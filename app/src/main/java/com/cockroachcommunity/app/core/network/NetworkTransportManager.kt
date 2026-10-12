package com.cockroachcommunity.app.core.network

import com.cockroachcommunity.app.core.network.nearby.NearbyConnectionsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkTransportManager @Inject constructor(
    val nearbyConnectionsManager: NearbyConnectionsManager
) {
    // Instead of activeConnection, we expose flows directly from nearbyConnectionsManager
    // MessageRepository will collect `incomingPayloads` when connected
    
    val incomingData: SharedFlow<ByteArray> = nearbyConnectionsManager.incomingPayloads
    val connectedEndpointId: StateFlow<String?> = nearbyConnectionsManager.connectedEndpointId

    fun connectToPeer(endpointId: String) {
        nearbyConnectionsManager.connectToEndpoint(endpointId)
    }

    fun getConnectedPeerMac(): String? {
        // Nearby API uses Endpoint IDs instead of MAC addresses.
        return nearbyConnectionsManager.connectedEndpointId.value
    }

    suspend fun sendData(data: ByteArray) {
        nearbyConnectionsManager.sendPayload(data)
    }

    fun disconnect() {
        nearbyConnectionsManager.disconnect()
    }
}
