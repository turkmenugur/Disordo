package com.disordo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disordo.data.repository.ImageRepository
import com.disordo.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}

class SettingsViewModel(
    private val imageRepository: ImageRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun startSync() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            try {
                val userId = userPreferencesRepository.userIdFlow.first()
                if (userId != null) {
                    imageRepository.syncImagesWithFirebase(userId)
                    _syncStatus.value = SyncStatus.SUCCESS
                } else {
                    _syncStatus.value = SyncStatus.ERROR
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}
