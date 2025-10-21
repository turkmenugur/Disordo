package com.disordo

import android.app.Application
import com.disordo.data.local.database.AppDatabase
import com.disordo.data.repository.AuthRepository
import com.disordo.data.repository.ImageRepository
import com.disordo.data.repository.StorageRepository
import com.disordo.data.repository.UserPreferencesRepository

class DisordoApplication : Application() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val authRepository by lazy { AuthRepository(this) }
    val storageRepository by lazy { StorageRepository() }
    val imageRepository by lazy { ImageRepository(database.uploadedImageDao(), storageRepository, this) }
}
