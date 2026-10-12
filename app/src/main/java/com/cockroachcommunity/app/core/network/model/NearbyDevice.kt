package com.cockroachcommunity.app.core.network.model

data class NearbyDevice(
    val id: String, // Pseudo-ID, could be MAC address mapped or hashed securely
    val name: String,
    val type: ConnectionType,
    val isPaired: Boolean,
    val signalStrength: Int? = null,
    val address: String // Internal MAC or identifier used for connecting
)

enum class ConnectionType {
    BLUETOOTH,
    WIFI_DIRECT
}
