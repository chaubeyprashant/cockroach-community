package com.cockroachcommunity.app.data.complaints

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

const val MaxUploadBytes = 20L * 1024 * 1024

@Singleton
class UploadRepository @Inject constructor(private val storage: FirebaseStorage) {

    /** Same path scheme as the website: complaints/{id}/{timestamp}-{sanitizedName}. */
    suspend fun upload(complaintId: String, file: PickedFile, onProgress: (Int) -> Unit = {}): MediaItem {
        require(file.size <= MaxUploadBytes) { "${file.name} is larger than 20MB." }
        val path = "complaints/$complaintId/${System.currentTimeMillis()}-${file.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
        val ref = storage.reference.child(path)
        val task = ref.putFile(Uri.parse(file.uri))
        task.addOnProgressListener { snap ->
            if (snap.totalByteCount > 0) onProgress((snap.bytesTransferred * 100 / snap.totalByteCount).toInt())
        }
        task.await()
        val url = ref.downloadUrl.await().toString()
        return MediaItem(file.name, path, file.mimeType, file.size, url)
    }
}
