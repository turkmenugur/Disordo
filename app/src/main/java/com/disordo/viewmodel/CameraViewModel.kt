package com.disordo.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disordo.data.repository.ImageRepository
import kotlinx.coroutines.launch

class CameraViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    fun saveCapturedImage(bitmap: Bitmap) {
        viewModelScope.launch {
            imageRepository.saveImageToLocal(bitmap, "CAMERA")
        }
    }

    fun saveSelectedImage(bitmap: Bitmap) {
        viewModelScope.launch {
            imageRepository.saveImageToLocal(bitmap, "GALLERY")
        }
    }
}
