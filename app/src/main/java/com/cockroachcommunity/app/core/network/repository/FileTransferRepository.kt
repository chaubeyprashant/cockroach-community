package com.cockroachcommunity.app.core.network.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cockroachcommunity.app.core.database.dao.CockroachCommunityDao
import com.cockroachcommunity.app.core.database.entity.TransferEntity
import com.cockroachcommunity.app.core.network.NetworkTransportManager
import com.cockroachcommunity.app.core.network.model.NetworkPayload
import com.cockroachcommunity.app.core.security.IdentityManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.cockroachcommunity.app.core.security.CryptoManager
import android.util.Base64

@Singleton
class FileTransferRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transportManager: NetworkTransportManager,
    private val dao: CockroachCommunityDao,
    private val identityManager: IdentityManager,
    private val cryptoManager: CryptoManager
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val CHUNK_SIZE = 8 * 1024 // 8 KB

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                dao.markStuckTransfersAsFailed()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getActiveTransfers(): Flow<List<TransferEntity>> {
        return dao.getActiveTransfers()
    }

    fun getTransfersForPeer(peerId: String): Flow<List<TransferEntity>> {
        return dao.getTransfersForPeer(peerId)
    }

    suspend fun sendFile(peerId: String, fileUri: Uri) {
        withContext(Dispatchers.IO) {
            val (fileName, fileSize) = getFileInfo(fileUri)
            if (fileName == null || fileSize == 0L) return@withContext

            val transferId = UUID.randomUUID().toString()
            
            val transferEntity = TransferEntity(
                transferId = transferId,
                peerDeviceId = peerId,
                fileName = fileName,
                fileType = "application/octet-stream",
                totalSize = fileSize,
                bytesTransferred = 0,
                direction = 1, // Outgoing
                status = 1 // Transferring
            )
            dao.saveTransfer(transferEntity)

            val connectionId = transportManager.connectedEndpointId.value
            if (connectionId == null) {
                dao.saveTransfer(transferEntity.copy(status = 3)) // Failed
                return@withContext
            }

            val myId = identityManager.getOrCreateDeviceId()
            val startPayload = NetworkPayload.FileTransferStart(
                transferId = transferId,
                senderId = myId,
                fileName = fileName,
                fileSize = fileSize
            )
            sendEncryptedPayload(startPayload)

            // Start sending chunks
            try {
                context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    var bytesRead: Int
                    var chunkIndex = 0
                    var totalTransferred = 0L
                    
                    val digest = MessageDigest.getInstance("SHA-256")

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        val chunkData = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)
                        digest.update(chunkData)
                        
                        val chunkPayload = NetworkPayload.FileChunk(
                            transferId = transferId,
                            chunkIndex = chunkIndex,
                            dataBase64 = Base64.encodeToString(chunkData, Base64.NO_WRAP)
                        )
                        sendEncryptedPayload(chunkPayload)

                        totalTransferred += bytesRead
                        chunkIndex++

                        // Update progress in DB periodically (e.g. every 10 chunks to reduce DB writes)
                        if (chunkIndex % 10 == 0) {
                            dao.saveTransfer(transferEntity.copy(bytesTransferred = totalTransferred))
                        }
                        kotlinx.coroutines.yield()
                    }
                    
                    val finalHash = digest.digest().joinToString("") { "%02x".format(it) }
                    sendEncryptedPayload(NetworkPayload.FileTransferComplete(transferId, finalHash))
                    
                    // Mark as completed
                    dao.saveTransfer(transferEntity.copy(bytesTransferred = fileSize, status = 2))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dao.saveTransfer(transferEntity.copy(status = 3)) // Failed
            }
        }
    }

    private fun getFileInfo(uri: Uri): Pair<String?, Long> {
        var name: String? = null
        var size: Long = 0
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                name = if (nameIndex != -1) cursor.getString(nameIndex) else "unknown_file"
                size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
            }
        }
        return Pair(name, size)
    }

    private suspend fun sendEncryptedPayload(payload: NetworkPayload) {
        val connectionId = transportManager.connectedEndpointId.value ?: return
        val finalPayload = if (cryptoManager.isReadyForEncryption()) {
            val jsonStr = json.encodeToString<NetworkPayload>(payload)
            val (iv, ciphertext) = cryptoManager.encrypt(jsonStr.toByteArray(StandardCharsets.UTF_8))
            NetworkPayload.EncryptedPayload(
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
                ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
            )
        } else {
            payload
        }
        val payloadJson = json.encodeToString<NetworkPayload>(finalPayload)
        transportManager.sendData(payloadJson.toByteArray(StandardCharsets.UTF_8))
    }
}
