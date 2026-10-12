package com.cockroachcommunity.app.core.network.nearby

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class NearbyDevice(
    val endpointId: String,
    val endpointName: String,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false
)

@Singleton
class NearbyConnectionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identityManager: com.cockroachcommunity.app.core.security.IdentityManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedDeviceId: String = "UnknownDevice"
    private var cachedDisplayName: String = "Nearby Friend"
    
    init {
        scope.launch {
            cachedDeviceId = identityManager.getOrCreateDeviceId()
            cachedDisplayName = identityManager.getDisplayName()
        }
        scope.launch {
            identityManager.displayNameFlow.collect {
                if (it.isNotBlank()) cachedDisplayName = it
            }
        }
    }
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER // Mesh-like

    private val _discoveredEndpoints = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    val discoveredEndpoints: StateFlow<Map<String, NearbyDevice>> = _discoveredEndpoints.asStateFlow()

    private val _connectedEndpointId = MutableStateFlow<String?>(null)
    val connectedEndpointId: StateFlow<String?> = _connectedEndpointId.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<ByteArray>(extraBufferCapacity = 100)
    val incomingPayloads: SharedFlow<ByteArray> = _incomingPayloads.asSharedFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    // Store incoming payload streams temporarily if using Stream payloads
    // But since we use bytes (up to 32KB limit, or we can use Stream for large data).
    // Let's assume we stick to Byte payloads for JSON and we shrink chunks to 16KB.

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.BYTES -> {
                    payload.asBytes()?.let { bytes ->
                        _incomingPayloads.tryEmit(bytes)
                    }
                }
                Payload.Type.STREAM -> {
                    // Handled if we use streams, but we rely on chunked byte payloads
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Auto accept for now
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            updateEndpointStatus(endpointId, isConnecting = true)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                _connectedEndpointId.value = endpointId
                updateEndpointStatus(endpointId, isConnecting = false, isConnected = true)
                stopDiscovery()
                stopAdvertising()
            } else {
                updateEndpointStatus(endpointId, isConnecting = false, isConnected = false)
            }
        }

        override fun onDisconnected(endpointId: String) {
            if (_connectedEndpointId.value == endpointId) {
                _connectedEndpointId.value = null
            }
            updateEndpointStatus(endpointId, isConnecting = false, isConnected = false)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val friendlyName = com.cockroachcommunity.app.core.common.SocialPersonaManager.formatPeerDisplayName(info.endpointName)
            val device = NearbyDevice(endpointId, friendlyName)
            _discoveredEndpoints.value = _discoveredEndpoints.value + (endpointId to device)
        }

        override fun onEndpointLost(endpointId: String) {
            _discoveredEndpoints.value = _discoveredEndpoints.value - endpointId
        }
    }

    private fun updateEndpointStatus(endpointId: String, isConnecting: Boolean? = null, isConnected: Boolean? = null) {
        val current = _discoveredEndpoints.value[endpointId] ?: return
        val updated = current.copy(
            isConnecting = isConnecting ?: current.isConnecting,
            isConnected = isConnected ?: current.isConnected
        )
        _discoveredEndpoints.value = _discoveredEndpoints.value + (endpointId to updated)
    }

    fun startAdvertising() {
        scope.launch {
            val name = try {
                identityManager.getDisplayName().ifBlank { cachedDisplayName }.take(40)
            } catch (e: Exception) {
                cachedDisplayName.take(40)
            }
            cachedDisplayName = name
            val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
            connectionsClient.startAdvertising(name, context.packageName, connectionLifecycleCallback, options)
                .addOnSuccessListener { Log.d("Nearby", "Advertising started: $name") }
                .addOnFailureListener { Log.e("Nearby", "Advertising failed", it) }
        }
    }

    fun startDiscovery() {
        _isDiscovering.value = true
        _discoveredEndpoints.value = emptyMap()
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(context.packageName, endpointDiscoveryCallback, options)
            .addOnSuccessListener { Log.d("Nearby", "Discovery started") }
            .addOnFailureListener { 
                Log.e("Nearby", "Discovery failed", it) 
                _isDiscovering.value = false
            }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
    }

    fun connectToEndpoint(endpointId: String) {
        val myName = cachedDisplayName.take(40)
        connectionsClient.requestConnection(myName, endpointId, connectionLifecycleCallback)
            .addOnFailureListener { Log.e("Nearby", "Request connection failed", it) }
    }

    fun disconnect() {
        _connectedEndpointId.value?.let { endpointId ->
            connectionsClient.disconnectFromEndpoint(endpointId)
        }
        connectionsClient.stopAllEndpoints()
        _connectedEndpointId.value = null
    }

    fun sendPayload(bytes: ByteArray) {
        _connectedEndpointId.value?.let { endpointId ->
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
        }
    }
}
