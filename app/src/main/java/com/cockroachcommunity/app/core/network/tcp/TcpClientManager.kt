package com.cockroachcommunity.app.core.network.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpClientManager @Inject constructor() {

    suspend fun connectToServer(ipAddress: String, port: Int = 8888, maxRetries: Int = 5): LocalTcpConnection? {
        android.util.Log.e("CockroachCommunity", "TcpClientManager: attempting to connect to $ipAddress:$port")
        return withContext(Dispatchers.IO) {
            var attempt = 0
            while (attempt < maxRetries) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress(ipAddress, port), 5000)
                    android.util.Log.e("CockroachCommunity", "TcpClientManager: connected successfully on attempt ${attempt + 1}")
                    return@withContext LocalTcpConnection(socket)
                } catch (e: Exception) {
                    attempt++
                    android.util.Log.e("CockroachCommunity", "TcpClientManager: connect attempt $attempt failed: ${e.message}")
                    e.printStackTrace()
                    if (attempt < maxRetries) {
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }
            android.util.Log.e("CockroachCommunity", "TcpClientManager: all $maxRetries attempts failed")
            null
        }
    }
}
