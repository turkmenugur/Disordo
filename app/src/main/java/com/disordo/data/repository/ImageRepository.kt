package com.disordo.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.disordo.data.local.dao.UploadedImageDao
import com.disordo.data.local.entity.UploadedImage
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class ImageRepository(
    private val uploadedImageDao: UploadedImageDao,
    private val storageRepository: StorageRepository,
    private val context: Context
) {

    private val firestore: FirebaseFirestore = Firebase.firestore

    fun getAllImages(): Flow<List<UploadedImage>> = uploadedImageDao.getAllImages()


    suspend fun saveImageToLocal(bitmap: Bitmap, source: String) {
        val localUri = saveBitmapToInternalStorage(bitmap)
        if (localUri != null) {
            val image = UploadedImage(
                localImageUri = localUri.toString(),
                timestamp = Date().time,
                source = source
            )
            uploadedImageDao.insertImage(image)
        }
    }

    suspend fun syncImagesWithFirebase(userId: String) {
        val unsyncedImages = uploadedImageDao.getUnsyncedImages()
        for (image in unsyncedImages) {
            val fileUri = Uri.parse(image.localImageUri)
            val firebaseUrl = storageRepository.uploadImage(userId, fileUri)

            if (firebaseUrl != null) {
                saveImageMetadataToFirestore(userId, firebaseUrl, image)

                val updatedImage = image.copy(
                    isSynced = true,
                    userId = userId,
                    firebaseStorageUrl = firebaseUrl
                )
                uploadedImageDao.updateImage(updatedImage)
            }
        }
    }

    suspend fun deleteImage(image: UploadedImage) {
        try {
            val file = File(Uri.parse(image.localImageUri).path ?: "")
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
        }
        uploadedImageDao.deleteImageById(image.id)

        // TODO: Firebase Storage ve Firestore'dan silme işlemi eklenecek
    }

    private suspend fun saveImageMetadataToFirestore(userId: String, firebaseUrl: String, image: UploadedImage) {
        val imageMap = hashMapOf(
            "userId" to userId,
            "imageUrl" to firebaseUrl,
            "timestamp" to com.google.firebase.Timestamp(Date(image.timestamp)),
            "source" to image.source,
            "deviceInfo" to getDeviceInfo()
        )
        firestore.collection("uploaded_images").add(imageMap).await()
    }

    private fun getDeviceInfo(): Map<String, String> {
        return mapOf(
            "model" to android.os.Build.MODEL,
            "manufacturer" to android.os.Build.MANUFACTURER,
            "os_version" to android.os.Build.VERSION.RELEASE
        )
    }

    private suspend fun saveBitmapToInternalStorage(bitmap: Bitmap): Uri? {
        return withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "image_${System.currentTimeMillis()}.jpg")
            try {
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                stream.flush()
                stream.close()
                Uri.fromFile(file)
            } catch (e: Exception) {
                null
            }
        }
    }
}
