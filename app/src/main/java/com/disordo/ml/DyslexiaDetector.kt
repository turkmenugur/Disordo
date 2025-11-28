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


import android.graphics.RectF
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.max
import kotlin.math.min
import java.lang.Math

class DyslexiaDetector(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelFileName = "model3_float32.tflite"
    private var isModelLoaded = false

    private val INPUT_SIZE = 640
    private val USE_FLOAT_INPUT = true
    private val NUM_CLASSES = 2
    private val NUM_ANCHORS = 8400
    private val CONFIDENCE_THRESHOLD = 0.5f
    private val IOU_THRESHOLD = 0.45f
    

    private fun loadModel() {
        if (isModelLoaded) return
        
        try {
            val model = FileUtil.loadMappedFile(context, modelFileName)
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            interpreter = Interpreter(model, options)
            isModelLoaded = true
            
            interpreter?.getInputTensor(0)?.let { tensor ->
                val shape = tensor.shape()
                Log.d(TAG, "Model Loaded - Input Shape: ${shape.contentToString()}, Type: ${tensor.dataType()}")
            }
            
            Log.d(TAG, "Model successfully loaded: $modelFileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: ${e.message}", e)
            isModelLoaded = false
        }
    }
    
    fun detectDyslexia(bitmap: Bitmap): DyslexiaResult {
        if (!isModelLoaded) {
            loadModel()
        }
        
        if (interpreter == null) {
            return DyslexiaResult(
                riskScore = 0f,
                confidence = 0f,
                isDyslexiaDetected = false,
                detections = emptyList(),
                errorMessage = "Model could not be loaded."
            )
        }
        
        try {
            Log.d(TAG, "--- Starting YOLOv8 Analysis ---")
            Log.d(TAG, "Original Bitmap: ${bitmap.width}x${bitmap.height}")
            
            val originalWidth = bitmap.width.toFloat()
            val originalHeight = bitmap.height.toFloat()
            
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val inputBuffer = bitmapToByteBuffer(resizedBitmap)
            
            val outputTensor = interpreter?.getOutputTensor(0)
            val outputShape = outputTensor?.shape()
            Log.d(TAG, "Output Shape: ${outputShape?.contentToString()}")
            
            // YOLOv8 output: [1, 4 + num_classes, 8400] veya [1, channels, 8400]
            val channels = outputShape?.getOrNull(1) ?: (4 + NUM_CLASSES)
            val anchors = outputShape?.getOrNull(2) ?: NUM_ANCHORS
            val outputSize = channels * anchors
            
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4).apply {
                order(ByteOrder.nativeOrder())
            }
            
            Log.d(TAG, "Running Inference...")
            val startTime = System.currentTimeMillis()
            interpreter?.run(inputBuffer, outputBuffer)
            val endTime = System.currentTimeMillis()
            Log.d(TAG, "Inference Time: ${endTime - startTime}ms")
            
            // Parse YOLOv8 output
            val detections = parseYOLOv8Output(outputBuffer, channels, anchors, originalWidth, originalHeight)
            Log.d(TAG, "Detected ${detections.size} objects after NMS")
            
            // Filter for "Letter" class (assuming class 0 is background, class 1 is letter/error)
            val letterDetections = detections.filter { it.classIndex == 1 }
            Log.d(TAG, "Letter detections: ${letterDetections.size}")
            
            // Calculate risk score based on detections
            val riskScore = calculateRiskScore(letterDetections)
            val confidence = letterDetections.maxOfOrNull { it.score } ?: 0f
            val isDyslexiaDetected = riskScore > 0.5f
            
            // Analyze line errors if we have letter detections
            val lineAnalysis = if (letterDetections.isNotEmpty()) {
                val boxes = letterDetections.map { it.boundingBox }
                analyzeLine(boxes)
            } else {
                JSONObject().apply {
                    put("line_angle", 0.0)
                    put("letter_spacing", 0.0)
                    put("alignment_error", "No")
                }
            }
            
            Log.d(TAG, "Result -> Score: $riskScore, Confidence: $confidence, Detected: $isDyslexiaDetected")
            Log.d(TAG, "--- Analysis Complete ---")
            
            return DyslexiaResult(
                riskScore = riskScore,
                confidence = confidence,
                isDyslexiaDetected = isDyslexiaDetected,
                detections = letterDetections,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Analysis Error: ${e.message}", e)
            return DyslexiaResult(
                riskScore = 0f,
                confidence = 0f,
                isDyslexiaDetected = false,
                detections = emptyList(),
                errorMessage = e.message
            )
        }
    }
    
    /**
     * YOLOv8 output'unu parse eder ve DetectionResult listesi döndürür
     * Output format: [1, channels, anchors] veya [1, anchors, channels]
     * channels: [cx, cy, w, h, class0_score, class1_score, ...]
     */
    private fun parseYOLOv8Output(
        outputBuffer: ByteBuffer,
        channels: Int,
        anchors: Int,
        originalWidth: Float,
        originalHeight: Float
    ): List<DetectionResult> {
        outputBuffer.rewind()
        
        // Convert ByteBuffer to FloatArray for easier access
        val floatArray = FloatArray(outputBuffer.remaining() / 4)
        outputBuffer.asFloatBuffer().get(floatArray)
        
        val allDetections = mutableListOf<DetectionResult>()
        
        // YOLOv8 output format: [1, anchors, channels] veya [1, channels, anchors]
        // En yaygın format: [1, anchors, channels] = [1, 8400, 6]
        // Her anchor için: [cx, cy, w, h, class0_score, class1_score, ...]
        
        // Determine layout based on array size
        val expectedSize = channels * anchors
        if (floatArray.size != expectedSize) {
            Log.w(TAG, "Output size mismatch: expected $expectedSize, got ${floatArray.size}")
        }
        
        // Try [anchors, channels] layout first (most common for YOLOv8)
        for (i in 0 until anchors) {
            val baseIndex = i * channels
            
            if (baseIndex + channels > floatArray.size) {
                Log.w(TAG, "Reached end of array at anchor $i")
                break
            }
            
            // Read coordinates (in 640x640 pixel space)
            val cx = floatArray[baseIndex + 0]
            val cy = floatArray[baseIndex + 1]
            val w = floatArray[baseIndex + 2]
            val h = floatArray[baseIndex + 3]
            
            // Read class scores and find max
            var maxScore = 0f
            var maxClassIndex = 0
            for (c in 0 until NUM_CLASSES) {
                if (baseIndex + 4 + c < floatArray.size) {
                    val score = floatArray[baseIndex + 4 + c]
                    if (score > maxScore) {
                        maxScore = score
                        maxClassIndex = c
                    }
                }
            }
            
            // Filter by confidence threshold
            if (maxScore < CONFIDENCE_THRESHOLD) {
                continue
            }
            
            // Convert from center coordinates to corner coordinates (in 640x640 space)
            val left = cx - w / 2f
            val top = cy - h / 2f
            val right = cx + w / 2f
            val bottom = cy + h / 2f
            
            // Ensure coordinates are within bounds
            val clampedLeft = left.coerceIn(0f, INPUT_SIZE.toFloat())
            val clampedTop = top.coerceIn(0f, INPUT_SIZE.toFloat())
            val clampedRight = right.coerceIn(0f, INPUT_SIZE.toFloat())
            val clampedBottom = bottom.coerceIn(0f, INPUT_SIZE.toFloat())
            
            // Scale to original image size
            val scaleX = originalWidth / INPUT_SIZE
            val scaleY = originalHeight / INPUT_SIZE
            
            val boundingBox = RectF(
                clampedLeft * scaleX,
                clampedTop * scaleY,
                clampedRight * scaleX,
                clampedBottom * scaleY
            )
            
            // Only add if box has valid dimensions
            if (boundingBox.width() > 0 && boundingBox.height() > 0) {
                allDetections.add(
                    DetectionResult(
                        boundingBox = boundingBox,
                        classIndex = maxClassIndex,
                        score = maxScore
                    )
                )
            }
        }
        
        Log.d(TAG, "Parsed ${allDetections.size} detections before NMS")
        
        // Apply Non-Maximum Suppression
        return nonMaxSuppression(allDetections)
    }
    
    /**
     * Non-Maximum Suppression (NMS) uygular
     */
    private fun nonMaxSuppression(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()
        
        // Sort by score (descending)
        val sorted = detections.sortedByDescending { it.score }
        val selected = mutableListOf<DetectionResult>()
        val suppressed = BooleanArray(sorted.size) { false }
        
        for (i in sorted.indices) {
            if (suppressed[i]) continue
            
            selected.add(sorted[i])
            
            // Suppress overlapping boxes
            for (j in (i + 1) until sorted.size) {
                if (suppressed[j]) continue
                
                val iou = calculateIoU(sorted[i].boundingBox, sorted[j].boundingBox)
                if (iou > IOU_THRESHOLD) {
                    suppressed[j] = true
                }
            }
        }
        
        return selected
    }
    
    /**
     * Intersection over Union (IoU) hesaplar
     */
    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val left = max(box1.left, box2.left)
        val top = max(box1.top, box2.top)
        val right = min(box1.right, box2.right)
        val bottom = min(box1.bottom, box2.bottom)
        
        if (left >= right || top >= bottom) {
            return 0f
        }
        
        val intersection = (right - left) * (bottom - top)
        val area1 = box1.width() * box1.height()
        val area2 = box2.width() * box2.height()
        val union = area1 + area2 - intersection
        
        return if (union > 0f) intersection / union else 0f
    }
    
    /**
     * Detection sonuçlarına göre risk skoru hesaplar
     */
    private fun calculateRiskScore(detections: List<DetectionResult>): Float {
        if (detections.isEmpty()) return 0f
        
        // Risk skoru: detection sayısı ve confidence skorlarına göre hesaplanır
        val avgConfidence = detections.map { it.score }.average().toFloat()
        val countFactor = min(detections.size / 10f, 1f) // Max 10 detection = 1.0
        
        return (avgConfidence * 0.7f + countFactor * 0.3f).coerceIn(0f, 1f)
    }
    
    private fun applySoftmax(logits: FloatArray): FloatArray {
        val max = logits.maxOrNull() ?: 0f
        val exp = logits.map { kotlin.math.exp(it - max) }
        val sum = exp.sum()
        return exp.map { (it / sum).toFloat() }.toFloatArray()
    }

    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = INPUT_SIZE
        val height = INPUT_SIZE
        
        val scaledBitmap = if (bitmap.width != width || bitmap.height != height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }
        
        val bufferSize = width * height * 3 * 4 // float32
        val byteBuffer = ByteBuffer.allocateDirect(bufferSize)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(width * height)
        scaledBitmap.getPixels(intValues, 0, width, 0, 0, width, height)
        
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE
        var sumVal = 0.0
        
        for (pixel in intValues) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
            
            // Stats for first channel (R) just to check
            if (r < minVal) minVal = r
            if (r > maxVal) maxVal = r
            sumVal += r
        }
        
        Log.d(TAG, "Input Stats (R channel) -> Min: $minVal, Max: $maxVal, Avg: ${sumVal / intValues.size}")
        
        byteBuffer.rewind()
        return byteBuffer
    }

    // --- Line Error Analysis Functions ---

    fun calculateLineAngle(boxes: List<RectF>): Double {
        if (boxes.isEmpty()) return 0.0

        val centers = boxes.map { (it.left + it.right) / 2f to (it.top + it.bottom) / 2f }

        val x = centers.map { it.first }
        val y = centers.map { it.second }

        val n = x.size
        val sumX = x.sum()
        val sumY = y.sum()
        val sumXY = x.zip(y) { xi, yi -> xi * yi }.sum()
        // FIX: Explicitly convert to Double for sumOf to avoid ambiguity
        val sumX2 = x.sumOf { (it * it).toDouble() }

        val denominator = (n * sumX2 - sumX * sumX)
        if (denominator == 0.0) return 0.0

        val slope = (n * sumXY - sumX * sumY) / denominator
        // FIX: Use Math.toDegrees
        return Math.toDegrees(atan(slope))
    }

    fun calculateLetterSpacing(boxes: List<RectF>): Float {
        if (boxes.size < 2) return 0f

        val sorted = boxes.sortedBy { it.left }
        val distances = mutableListOf<Float>()

        for (i in 0 until sorted.size - 1) {
            val dist = sorted[i + 1].left - sorted[i].right
            distances.add(dist)
        }

        return distances.average().toFloat()
    }

    fun analyzeLine(boxes: List<RectF>): JSONObject {
        val angle = calculateLineAngle(boxes)
        val spacing = calculateLetterSpacing(boxes)

        val alignmentError = if (abs(angle) > 3) "Yes" else "No"
        
        Log.d(TAG, "Line Analysis -> Angle: $angle, Spacing: $spacing, Error: $alignmentError")

        val result = JSONObject()
        result.put("line_angle", angle)
        result.put("letter_spacing", spacing)
        result.put("alignment_error", alignmentError)

        return result
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
    
    companion object {
        private const val TAG = "DyslexiaDetector"
    }
}

/**
 * Object Detection sonuç data class'ı
 */
data class DetectionResult(
    val boundingBox: RectF,      // Bounding box koordinatları
    val classIndex: Int,          // Sınıf indeksi (0: background, 1: letter/error)
    val score: Float              // Güven skoru (0.0 - 1.0)
)

/**
 * Disleksi analiz sonuç data class'ı
 */
data class DyslexiaResult(
    val riskScore: Float,        // 0.0 - 1.0 arası risk skoru
    val confidence: Float,       // Model güven seviyesi
    val isDyslexiaDetected: Boolean, // Disleksi tespit edildi mi?
    val detections: List<DetectionResult> = emptyList(), // Tespit edilen objeler
    val errorMessage: String? = null    // Hata mesajı (varsa)
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

