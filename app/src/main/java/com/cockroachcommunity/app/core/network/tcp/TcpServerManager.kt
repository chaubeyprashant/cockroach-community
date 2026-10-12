package com.cockroachcommunity.app.core.network.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpServerManager @Inject constructor() {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    private val _incomingConnections = MutableSharedFlow<LocalTcpConnection>()
    val incomingConnections: SharedFlow<LocalTcpConnection> = _incomingConnections.asSharedFlow()

    suspend fun startServer(port: Int = 8888) {
        android.util.Log.e("CockroachCommunity", "TcpServerManager: starting server on port $port")
        withContext(Dispatchers.IO) {
            if (isRunning) return@withContext
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                android.util.Log.e("CockroachCommunity", "TcpServerManager: server started successfully. waiting for connections...")
                while (isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    android.util.Log.e("CockroachCommunity", "TcpServerManager: accepted connection from ${socket.inetAddress.hostAddress}")
                    _incomingConnections.emit(LocalTcpConnection(socket))
                }
            } catch (e: Exception) {
                android.util.Log.e("CockroachCommunity", "TcpServerManager: server loop exception: ${e.message}")
                e.printStackTrace()
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
    }
}
