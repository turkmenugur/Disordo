package com.disordo.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disordo.data.local.entity.UploadedImage
import com.disordo.data.repository.ImageRepository
import com.disordo.ml.DyslexiaDetector
import com.disordo.ml.DyslexiaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class HomeViewModel(
    private val imageRepository: ImageRepository,
    private val context: Context
) : ViewModel() {

    private val _images = MutableStateFlow<List<UploadedImage>>(emptyList())
    val images: StateFlow<List<UploadedImage>> = _images.asStateFlow()
    
    private val _analysisResult = MutableStateFlow<DyslexiaResult?>(null)
    val analysisResult: StateFlow<DyslexiaResult?> = _analysisResult.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private var dyslexiaDetector: DyslexiaDetector? = null

    init {
        loadImages()
        // Detector'ı lazy olarak başlatalım - sadece ihtiyaç olduğunda yüklenecek
        dyslexiaDetector = DyslexiaDetector(context)
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
    
    /**
     * Galeriden veya kameradan seçilen görüntüyü analiz eder
     */
    fun analyzeImage(imageUri: Uri) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = null
            
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    uriToBitmap(imageUri)
                }
                
                if (bitmap != null) {
                    val result = withContext(Dispatchers.Default) {
                        dyslexiaDetector?.detectDyslexia(bitmap)
                    }
                    _analysisResult.value = result
                } else {
                    _analysisResult.value = DyslexiaResult(
                        riskScore = 0f,
                        confidence = 0f,
                        isDyslexiaDetected = false,
                        errorMessage = "Görüntü yüklenemedi"
                    )
                }
            } catch (e: Exception) {
                _analysisResult.value = DyslexiaResult(
                    riskScore = 0f,
                    confidence = 0f,
                    isDyslexiaDetected = false,
                    errorMessage = "Hata: ${e.message}"
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * Bitmap ile doğrudan analiz
     */
    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResult.value = null
            
            try {
                val result = withContext(Dispatchers.Default) {
                    dyslexiaDetector?.detectDyslexia(bitmap)
                }
                _analysisResult.value = result
            } catch (e: Exception) {
                _analysisResult.value = DyslexiaResult(
                    riskScore = 0f,
                    confidence = 0f,
                    isDyslexiaDetected = false,
                    errorMessage = "Hata: ${e.message}"
                )
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
    
    /**
     * URI'yi Bitmap'e dönüştürür
     */
    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Analiz sonucunu temizle
     */
    fun clearAnalysisResult() {
        _analysisResult.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        dyslexiaDetector?.close()
    }
}
