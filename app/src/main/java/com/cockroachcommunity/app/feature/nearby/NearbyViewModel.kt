package com.cockroachcommunity.app.feature.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cockroachcommunity.app.core.network.NetworkTransportManager
import com.cockroachcommunity.app.core.network.nearby.NearbyConnectionsManager
import com.cockroachcommunity.app.core.network.model.ConnectionType
import com.cockroachcommunity.app.core.network.model.NearbyDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val nearbyConnectionsManager: NearbyConnectionsManager,
    private val transportManager: NetworkTransportManager
) : ViewModel() {

    private val _connectingDeviceAddress = MutableStateFlow<String?>(null)
    private val _connectionError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<NearbyUiState> = combine(
        nearbyConnectionsManager.discoveredEndpoints,
        nearbyConnectionsManager.isDiscovering,
        _connectingDeviceAddress,
        nearbyConnectionsManager.connectedEndpointId,
        _connectionError
    ) { endpoints: Map<String, com.cockroachcommunity.app.core.network.nearby.NearbyDevice>, isDiscovering: Boolean, connectingAddress: String?, connectedEndpointId: String?, error: String? ->
        val mappedDevices = endpoints.values.map { 
            NearbyDevice(
                id = it.endpointId,
                name = it.endpointName,
                type = ConnectionType.WIFI_DIRECT,
                isPaired = false,
                address = it.endpointId
            )
        }
        
        val isConnected = connectedEndpointId != null
        val finalConnectingAddress = if (isConnected) null else connectingAddress

        NearbyUiState.Active(
            devices = mappedDevices,
            isDiscovering = isDiscovering,
            connectingDeviceAddress = finalConnectingAddress,
            connectedDeviceAddress = connectedEndpointId,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NearbyUiState.Loading
    )

    init {
        startDiscoveryAndAdvertising()
    }

    private fun startDiscoveryAndAdvertising() {
        nearbyConnectionsManager.stopDiscovery()
        nearbyConnectionsManager.stopAdvertising()
        
        nearbyConnectionsManager.startAdvertising()
        nearbyConnectionsManager.startDiscovery()
    }

    fun startDiscovery() {
        _connectionError.value = null
        startDiscoveryAndAdvertising()
    }

    fun stopDiscovery() {
        nearbyConnectionsManager.stopDiscovery()
        nearbyConnectionsManager.stopAdvertising()
    }

    fun connectToDevice(device: NearbyDevice) {
        viewModelScope.launch {
            _connectingDeviceAddress.value = device.address
            _connectionError.value = null
            
            transportManager.connectToPeer(device.address)
            
            delay(15000)
            if (nearbyConnectionsManager.connectedEndpointId.value == null) {
                _connectingDeviceAddress.value = null
                _connectionError.value = "Connection timeout"
            }
        }
    }

    fun disconnect() {
        transportManager.disconnect()
        _connectingDeviceAddress.value = null
        startDiscoveryAndAdvertising()
    }

    fun clearError() {
        _connectionError.value = null
    }
}

sealed interface NearbyUiState {
    object Loading : NearbyUiState
    data class Active(
        val devices: List<NearbyDevice> = emptyList(),
        val isDiscovering: Boolean = false,
        val connectingDeviceAddress: String? = null,
        val connectedDeviceAddress: String? = null,
        val error: String? = null
    ) : NearbyUiState
}
