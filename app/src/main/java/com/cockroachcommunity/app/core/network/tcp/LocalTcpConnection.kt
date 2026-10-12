package com.cockroachcommunity.app.core.network.tcp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

import java.io.DataInputStream
import java.io.DataOutputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalTcpConnection(private val socket: Socket) {
    private val dataInputStream = DataInputStream(socket.getInputStream())
    private val dataOutputStream = DataOutputStream(socket.getOutputStream())
    private val writeMutex = Mutex()

    // 10 MB max payload size to prevent OOM on malicious or corrupted length headers
    private val MAX_PAYLOAD_SIZE = 10 * 1024 * 1024
    
    val incomingData: Flow<ByteArray> = flow {
        while (socket.isConnected && !socket.isClosed) {
            try {
                val length = dataInputStream.readInt()
                if (length <= 0 || length > MAX_PAYLOAD_SIZE) {
                    // Protocol violation or connection close
                    break
                }
                val payload = ByteArray(length)
                dataInputStream.readFully(payload)
                emit(payload)
            } catch (e: Exception) {
                break
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun sendData(data: ByteArray) {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    dataOutputStream.writeInt(data.size)
                    dataOutputStream.write(data)
                    dataOutputStream.flush()
                } catch (e: Exception) {
                    // Handle or rethrow
                    e.printStackTrace()
                }
            }
        }
    }

    fun close() {
        try {
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
