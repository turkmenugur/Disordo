package com.disordo.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.disordo.data.repository.ImageRepository
import com.disordo.ml.DyslexiaDetector
import com.disordo.ml.DyslexiaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraViewModel(
    private val imageRepository: ImageRepository,
    private val context: Context
) : ViewModel() {

    private val _analysisResult = MutableStateFlow<DyslexiaResult?>(null)
    val analysisResult: StateFlow<DyslexiaResult?> = _analysisResult.asStateFlow()
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()
    
    private var dyslexiaDetector: DyslexiaDetector? = null

    init {
        dyslexiaDetector = DyslexiaDetector(context)
    }

    /**
     * Kameradan çekilen fotoğrafı kaydet ve analiz et
     */
    fun saveCapturedImage(bitmap: Bitmap) {
        viewModelScope.launch {
            // Önce veritabanına kaydet
            imageRepository.saveImageToLocal(bitmap, "CAMERA")
            // Sonra analiz et
            analyzeImage(bitmap)
        }
    }

    /**
     * Galeriden seçilen fotoğrafı kaydet ve analiz et
     */
    fun saveSelectedImage(bitmap: Bitmap) {
        viewModelScope.launch {
            // Önce veritabanına kaydet
            imageRepository.saveImageToLocal(bitmap, "GALLERY")
            // Sonra analiz et
            analyzeImage(bitmap)
        }
    }
    
    /**
     * Görüntüyü analiz et
     */
    private suspend fun analyzeImage(bitmap: Bitmap) {
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
                errorMessage = "Analiz sırasında hata: ${e.message}"
            )
        } finally {
            _isAnalyzing.value = false
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
