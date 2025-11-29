package com.disordo

import android.app.Application
import android.graphics.Bitmap
import com.disordo.data.local.database.AppDatabase
import com.disordo.data.repository.AuthRepository
import com.disordo.data.repository.ImageRepository
import com.disordo.data.repository.StorageRepository
import com.disordo.data.repository.UserPreferencesRepository
import com.disordo.ml.DetectionResult

class DisordoApplication : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val authRepository by lazy { AuthRepository(this) }
    val storageRepository by lazy { StorageRepository() }
    val imageRepository by lazy { ImageRepository(database.uploadedImageDao(), storageRepository, this) }
    
    // Geçici bitmap ve detections saklama (navigation için)
    var tempAnalyzedBitmap: Bitmap? = null
        private set
    
    var tempDetections: List<DetectionResult> = emptyList()
        private set
    
    fun setTempAnalysisData(bitmap: Bitmap?, detections: List<DetectionResult>) {
        tempAnalyzedBitmap = bitmap
        tempDetections = detections
    }
    
    fun clearTempAnalysisData() {
        tempAnalyzedBitmap?.recycle()
        tempAnalyzedBitmap = null
        tempDetections = emptyList()
    }
}
