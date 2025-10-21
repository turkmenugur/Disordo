package com.disordo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.disordo.data.local.entity.UploadedImage
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadedImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: UploadedImage)

    @Update
    suspend fun updateImage(image: UploadedImage)

    @Query("DELETE FROM uploaded_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: Int): Int

    @Query("SELECT * FROM uploaded_images ORDER BY timestamp DESC")
    fun getAllImages(): Flow<List<UploadedImage>>

    @Query("SELECT * FROM uploaded_images WHERE isSynced = false")
    suspend fun getUnsyncedImages(): List<UploadedImage>
}
