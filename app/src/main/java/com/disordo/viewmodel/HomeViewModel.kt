package com.disordo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disordo.data.local.entity.UploadedImage
import com.disordo.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class HomeViewModel(private val imageRepository: ImageRepository) : ViewModel() {

    private val _images = MutableStateFlow<List<UploadedImage>>(emptyList())
    val images: StateFlow<List<UploadedImage>> = _images.asStateFlow()

    init {
        loadImages()
    }

    private fun loadImages() {
        viewModelScope.launch {
            imageRepository.getAllImages()
                .catch { exception ->
                }
                .collect { imageList ->
                    _images.value = imageList
                }
        }
    }

    fun deleteImage(image: UploadedImage) {
        viewModelScope.launch {
            imageRepository.deleteImage(image)
        }
    }
}
