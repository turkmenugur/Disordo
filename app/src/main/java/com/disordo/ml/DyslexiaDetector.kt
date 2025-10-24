package com.disordo.ml

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Disleksi tespiti için TensorFlow Lite model wrapper sınıfı
 * Model girdi ve çıktı formatını projenize göre ayarlayın
 */
class DyslexiaDetector(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelFileName = "best_float32.tflite"
    private var isModelLoaded = false
    
    /**
     * Modeli lazy loading ile yükle
     */
    private fun loadModel() {
        if (isModelLoaded) return
        
        try {
            val model = FileUtil.loadMappedFile(context, modelFileName)
            val options = Interpreter.Options().apply {
                // GPU desteği eklemek isterseniz:
                // addDelegate(GpuDelegate())
                numThreads = 4
            }
            interpreter = Interpreter(model, options)
            isModelLoaded = true
            Log.d(TAG, "Model başarıyla yüklendi: $modelFileName")
        } catch (e: Exception) {
            Log.e(TAG, "Model yüklenirken hata oluştu: ${e.message}", e)
            isModelLoaded = false
        }
    }
    
    /**
     * Görüntüyü analiz eder ve disleksi riski skorunu döndürür
     * @param bitmap Analiz edilecek el yazısı görüntüsü
     * @return DyslexiaResult nesnesi (risk skoru ve güven seviyesi)
     */
    fun detectDyslexia(bitmap: Bitmap): DyslexiaResult {
        // Modeli ilk kullanımda yükle
        if (!isModelLoaded) {
            loadModel()
        }
        
        if (interpreter == null) {
            return DyslexiaResult(
                riskScore = 0f,
                confidence = 0f,
                isDyslexiaDetected = false,
                errorMessage = "Model yüklenemedi. Lütfen model dosyasının app/src/main/assets/$modelFileName konumunda olduğundan emin olun."
            )
        }
        
        try {
            // 1. Görüntüyü model için uygun boyuta getir
            val inputSize = 224 // Modelinizin girdi boyutuna göre ayarlayın
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            
            // 2. Bitmap'i ByteBuffer'a dönüştür
            val inputBuffer = bitmapToByteBuffer(resizedBitmap, inputSize)
            
            // 3. Çıktı tensörünü hazırla
            // Model çıktı boyutunu modelinize göre ayarlayın
            val outputSize = 2 // Örnek: [disleksi_yok, disleksi_var] için 2 sınıf
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // 4. Model inference
            interpreter?.run(inputBuffer, outputBuffer)
            
            // 5. Sonuçları parse et
            outputBuffer.rewind()
            val scores = FloatArray(outputSize) { 
                outputBuffer.float 
            }
            
            // 6. Sonuçları yorumla
            val dyslexiaScore = scores.getOrNull(1) ?: 0f
            val confidence = scores.maxOrNull() ?: 0f
            val isDyslexiaDetected = dyslexiaScore > 0.5f
            
            Log.d(TAG, "Analiz tamamlandı - Risk Skoru: $dyslexiaScore, Güven: $confidence")
            
            return DyslexiaResult(
                riskScore = dyslexiaScore,
                confidence = confidence,
                isDyslexiaDetected = isDyslexiaDetected,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Görüntü analiz edilirken hata oluştu: ${e.message}", e)
            return DyslexiaResult(
                riskScore = 0f,
                confidence = 0f,
                isDyslexiaDetected = false,
                errorMessage = "Analiz sırasında hata: ${e.message}"
            )
        }
    }
    
    /**
     * Bitmap'i TensorFlow Lite için uygun ByteBuffer formatına dönüştürür
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap, inputSize: Int): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(inputSize * inputSize)
        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var pixel = 0
        for (i in 0 until inputSize) {
            for (j in 0 until inputSize) {
                val value = intValues[pixel++]
                
                // RGB değerlerini normalize et (0-255 -> 0-1)
                val r = ((value shr 16) and 0xFF) / 255.0f
                val g = ((value shr 8) and 0xFF) / 255.0f
                val b = (value and 0xFF) / 255.0f
                
                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }
        
        return byteBuffer
    }
    
    /**
     * Kaynakları temizle
     */
    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "Model kaynakları temizlendi")
    }
    
    companion object {
        private const val TAG = "DyslexiaDetector"
    }
}

/**
 * Disleksi analiz sonuç data class'ı
 */
data class DyslexiaResult(
    val riskScore: Float,        // 0.0 - 1.0 arası risk skoru
    val confidence: Float,       // Model güven seviyesi
    val isDyslexiaDetected: Boolean, // Disleksi tespit edildi mi?
    val errorMessage: String?    // Hata mesajı (varsa)
) {
    /**
     * Risk seviyesini metin olarak döndürür
     */
    fun getRiskLevelText(): String {
        return when {
            riskScore < 0.3f -> "Düşük Risk"
            riskScore < 0.6f -> "Orta Risk"
            else -> "Yüksek Risk"
        }
    }
    
    /**
     * Yüzde olarak risk skorunu döndürür
     */
    fun getRiskPercentage(): Int {
        return (riskScore * 100).toInt()
    }
}

