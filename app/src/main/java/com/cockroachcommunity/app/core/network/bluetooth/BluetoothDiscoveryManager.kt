package com.cockroachcommunity.app.core.network.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.cockroachcommunity.app.core.network.model.ConnectionType
import com.cockroachcommunity.app.core.network.model.NearbyDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothDiscoveryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val _discoveredDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<NearbyDevice>> = _discoveredDevices.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)

                    device?.let {
                        addDevice(it, rssi.toInt())
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isDiscovering.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        _discoveredDevices.value = emptyList() // Clear previous discovery
        
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(receiver, filter)

        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        _isDiscovering.value = false
    }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, rssi: Int) {
        val address = device.address ?: return
        val name = device.name ?: "Unknown Device"
        val isPaired = device.bondState == BluetoothDevice.BOND_BONDED
        
        val hashedId = hashMacAddress(address)

        val nearbyDevice = NearbyDevice(
            id = hashedId,
            name = name,
            type = ConnectionType.BLUETOOTH,
            isPaired = isPaired,
            signalStrength = rssi,
            address = address
        )

        _discoveredDevices.update { currentList ->
            val existingIndex = currentList.indexOfFirst { it.address == address }
            if (existingIndex != -1) {
                val updatedList = currentList.toMutableList()
                updatedList[existingIndex] = nearbyDevice
                updatedList
            } else {
                currentList + nearbyDevice
            }
        }
    }

    private fun hashMacAddress(mac: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(mac.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16) // Use first 16 chars as a pseudo-ID
    }
}
