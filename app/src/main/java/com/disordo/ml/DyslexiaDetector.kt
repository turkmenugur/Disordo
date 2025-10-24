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
    
    // Model spesifik ayarlar (modelinize göre değiştirin)
    private val INPUT_SIZE = 640 // Model girdi boyutu
    private val USE_FLOAT_INPUT = true // true: float32 (0-1), false: uint8 (0-255)
    private val NUM_CLASSES = 2 // Çıktı sınıf sayısı
    
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
            
            // Model girdi boyutunu logla
            interpreter?.getInputTensor(0)?.let { tensor ->
                val shape = tensor.shape()
                Log.d(TAG, "Model girdi boyutu: ${shape.contentToString()}")
            }
            
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
            // 1. Model girdi boyutunu logla
            val inputTensor = interpreter?.getInputTensor(0)
            val inputShape = inputTensor?.shape()
            val inputDataType = inputTensor?.dataType()
            
            Log.d(TAG, "Model girdi shape: ${inputShape?.contentToString()}")
            Log.d(TAG, "Model girdi data type: $inputDataType")
            Log.d(TAG, "Bitmap orijinal boyut: ${bitmap.width}x${bitmap.height}")
            
            // 2. Görüntüyü sabit 640x640 boyutuna getir
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            Log.d(TAG, "Resize edilmiş boyut: ${resizedBitmap.width}x${resizedBitmap.height}")
            
            // 3. Bitmap'i ByteBuffer'a dönüştür
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            Log.d(TAG, "Input buffer boyutu: ${inputBuffer.capacity()} bytes")
            
            // 4. Çıktı tensörünü hazırla
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape()
            Log.d(TAG, "Model çıktı shape: ${outputShape?.contentToString()}")
            
            if (outputShape == null || outputShape.isEmpty()) {
                throw Exception("Geçersiz model çıktı formatı")
            }
            
            // Output shape'e göre buffer oluştur
            val outputSize = outputShape.reduce { acc, i -> acc * i }
            Log.d(TAG, "Çıktı buffer boyutu: $outputSize elements")
            
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            // 5. Model inference
            Log.d(TAG, "Model inference başlıyor...")
            interpreter?.run(inputBuffer, outputBuffer)
            Log.d(TAG, "Model inference tamamlandı")
            
            // 6. Sonuçları parse et
            outputBuffer.rewind()
            val scores = FloatArray(NUM_CLASSES) { 
                outputBuffer.float 
            }
            
            Log.d(TAG, "Model çıktı skorları: ${scores.contentToString()}")
            
            // 7. Sonuçları yorumla
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
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = INPUT_SIZE
        val height = INPUT_SIZE
        
        // Bitmap'in kesinlikle doğru boyutta olduğundan emin ol
        val scaledBitmap = if (bitmap.width != width || bitmap.height != height) {
            Log.w(TAG, "Bitmap boyutu uyuşmuyor, tekrar resize ediliyor: ${bitmap.width}x${bitmap.height} -> ${width}x${height}")
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }
        
        // Buffer boyutunu hesapla
        val numElements = width * height * 3
        val bytesPerElement = if (USE_FLOAT_INPUT) 4 else 1 // float32 = 4 bytes, uint8 = 1 byte
        val bufferSize = numElements * bytesPerElement
        
        Log.d(TAG, "ByteBuffer oluşturuluyor: $bufferSize bytes ($width x $height x 3 x $bytesPerElement)")
        
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        // Tüm pixelleri bir kerede al
        val pixelCount = width * height
        val intValues = IntArray(pixelCount)
        scaledBitmap.getPixels(intValues, 0, width, 0, 0, width, height)
        
        Log.d(TAG, "Pixel array boyutu: ${intValues.size}, Beklenen: $pixelCount")
        
        // Pixelleri ByteBuffer'a yaz
        for (pixelIndex in 0 until pixelCount) {
            val pixel = intValues[pixelIndex]
            
            // RGB değerlerini çıkar
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            
            if (USE_FLOAT_INPUT) {
                // Float32 format (0.0 - 1.0 normalizasyon)
                byteBuffer.putFloat(r / 255.0f)
                byteBuffer.putFloat(g / 255.0f)
                byteBuffer.putFloat(b / 255.0f)
            } else {
                // UInt8 format (0 - 255)
                byteBuffer.put(r.toByte())
                byteBuffer.put(g.toByte())
                byteBuffer.put(b.toByte())
            }
        }
        
        Log.d(TAG, "ByteBuffer dolduruldu, position: ${byteBuffer.position()}, capacity: ${byteBuffer.capacity()}")
        
        // Buffer'ı başa sar
        byteBuffer.rewind()
        
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

