package com.disordo.data.repository

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StorageRepository {

    private val storage: FirebaseStorage = Firebase.storage
    suspend fun uploadImage(userId: String, fileUri: Uri): String? {
        return try {
            val imageId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("images/$userId/$imageId")

            storageRef.putFile(fileUri).await()

            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }
}
