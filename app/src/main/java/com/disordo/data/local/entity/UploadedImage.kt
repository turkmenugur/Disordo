package com.disordo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uploaded_images")
data class UploadedImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val localImageUri: String,
    val timestamp: Long,
    val source: String,
    val isSynced: Boolean = false,
    val userId: String? = null,
    val firebaseStorageUrl: String? = null
)
