package com.disordo

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.disordo.ui.theme.OpenDyslexic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max

@Composable
fun TextRecognitionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(key1 = true) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        TextScanCameraView(modifier)
    } else {
        NoPermissionScreen { launcher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable
private fun NoPermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text("Metin taramak için kamera izni gereklidir.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermission) {
            androidx.compose.material3.Text("İzin Ver")
        }
    }
}

@Composable
private fun TextScanCameraView(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var recognizedText by remember { mutableStateOf<Text?>(null) }
    var imageWidth by remember { mutableIntStateOf(0) }
    var imageHeight by remember { mutableIntStateOf(0) }
    var imageRotation by remember { mutableIntStateOf(0) }
    var isFrozen by remember { mutableStateOf(false) }

    // Keep track of the last valid text to show when frozen
    var frozenText by remember { mutableStateOf<Text?>(null) }
    var frozenImageWidth by remember { mutableIntStateOf(0) }
    var frozenImageHeight by remember { mutableIntStateOf(0) }
    var frozenImageRotation by remember { mutableIntStateOf(0) }
    
    // Captured/Selected image states
    var showCapturedImage by remember { mutableStateOf(false) }
    var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedImageText by remember { mutableStateOf<Text?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember { mutableStateOf<ImageCapture?>(null) }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = it.getBitmap(context.contentResolver)
                bitmap?.let { bm ->
                isProcessing = true
                processImageForText(bm) { text ->
                    if (text != null) {
                        capturedImageBitmap = bm
                        capturedImageText = text
                        showCapturedImage = true
                    }
                    isProcessing = false
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Show captured/selected image or camera preview
        if (showCapturedImage && capturedImageBitmap != null && capturedImageText != null) {
            // Static image with text overlay
            StaticImageTextView(
                bitmap = capturedImageBitmap!!,
                text = capturedImageText!!,
                onClose = {
                    showCapturedImage = false
                    capturedImageBitmap = null
                    capturedImageText = null
                }
            )
        } else {
            // Camera preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(Size(1280, 720)) 
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, TextRecognitionAnalyzer { text, width, height, rotation ->
                                if (!isFrozen) {
                                    recognizedText = text
                                    imageWidth = width
                                    imageHeight = height
                                    imageRotation = rotation
                                }
                            })
                        }
                        
                        // ImageCapture for taking photos
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        cameraController.value = imageCapture
                        
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer, imageCapture)
                    } catch (exc: Exception) {
                        Log.e("TextScanCameraView", "Kamera bağlanamadı", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // AR Overlay
        val textToShow = if (isFrozen) frozenText else recognizedText
        val widthToShow = if (isFrozen) frozenImageWidth else imageWidth
        val heightToShow = if (isFrozen) frozenImageHeight else imageHeight
        val rotationToShow = if (isFrozen) frozenImageRotation else imageRotation

        if (textToShow != null && widthToShow > 0 && heightToShow > 0) {
            TextOverlay(
                text = textToShow,
                imageWidth = widthToShow,
                imageHeight = heightToShow,
                rotation = rotationToShow,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isFrozen) {
             Box(modifier = Modifier
                 .fillMaxSize()
                 .border(4.dp, Color.Cyan.copy(alpha = 0.5f)))
        }
        }
        
        // Processing indicator
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Bottom controls
        if (!showCapturedImage) {
        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Spacer(modifier = Modifier.weight(1f))

                // Freeze/Unfreeze button
            IconButton(
                onClick = {
                    if (!isFrozen) {
                        frozenText = recognizedText
                        frozenImageWidth = imageWidth
                        frozenImageHeight = imageHeight
                        frozenImageRotation = imageRotation
                    }
                    isFrozen = !isFrozen
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                        .padding(bottom = 120.dp)
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFrozen) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isFrozen) "Devam Et" else "Dondur",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                }
                
                // Camera and Gallery buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery button
                    FloatingActionButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Galeri",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    // Camera capture button
                    FloatingActionButton(
                        onClick = {
                            cameraController.value?.let { capture ->
                                isProcessing = true
                                capturePhoto(context, capture) { bitmap ->
                                    processImageForText(bitmap) { text ->
                                        if (text != null) {
                                            capturedImageBitmap = bitmap
                                            capturedImageText = text
                                            showCapturedImage = true
                                        }
                                        isProcessing = false
                                    }
                                }
                            }
                        },
                        containerColor = Color.Black.copy(alpha = 0.7f),
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Fotoğraf Çek",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TextOverlay(
    text: Text,
    imageWidth: Int,
    imageHeight: Int,
    rotation: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Determine the dimensions of the image as it would appear upright
        val isRotated = rotation == 90 || rotation == 270
        val uprightImageWidth = if (isRotated) imageHeight else imageWidth
        val uprightImageHeight = if (isRotated) imageWidth else imageHeight

        // Calculate scale to fill the screen (FILL_CENTER behavior)
        val scale = max(canvasWidth / uprightImageWidth, canvasHeight / uprightImageHeight)
        
        // Calculate the offset to center the scaled image
        val scaledWidth = uprightImageWidth * scale
        val scaledHeight = uprightImageHeight * scale
        val offsetX = (canvasWidth - scaledWidth) / 2
        val offsetY = (canvasHeight - scaledHeight) / 2

        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null) {
                    // ML Kit returns coordinates relative to the upright image.
                    // So we can use them directly without manual rotation transformation.
                    
                    val left = box.left * scale + offsetX
                    val top = box.top * scale + offsetY
                    val right = box.right * scale + offsetX
                    val bottom = box.bottom * scale + offsetY
                    
                    val rectWidth = right - left
                    val rectHeight = bottom - top

                    // Draw background
                    drawRect(
                        color = Color.Black.copy(alpha = 0.8f),
                        topLeft = Offset(left, top),
                        size = ComposeSize(rectWidth, rectHeight),
                        style = androidx.compose.ui.graphics.drawscope.Fill
                    )

                    // Calculate font size with a minimum limit
                    val calculatedFontSize = with(density) { (rectHeight * 0.75f).toSp() }
                    val fontSize = if (calculatedFontSize < 14.sp) 14.sp else calculatedFontSize
                    
                    val measuredText = textMeasurer.measure(
                        text = line.text,
                        style = TextStyle(
                            fontFamily = OpenDyslexic,
                            color = Color.White,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    
                    // Center text in the box
                    drawText(
                        textLayoutResult = measuredText,
                        topLeft = Offset(
                            left + (rectWidth - measuredText.size.width) / 2, 
                            top + (rectHeight - measuredText.size.height) / 2
                        )
                    )
                }
            }
        }
    }
}

/**
 * Static image view with text overlay
 */
@Composable
private fun StaticImageTextView(
    bitmap: Bitmap,
    text: Text,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Image
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Çekilen Görüntü",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillWidth
        )
        
        // Text overlay with proper scaling
        TextOverlay(
            text = text,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            rotation = 0, // Static images are already rotated
            modifier = Modifier.fillMaxSize()
        )
        
        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Process bitmap for text recognition
 */
private fun processImageForText(bitmap: Bitmap, onResult: (Text?) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            onResult(visionText)
        }
        .addOnFailureListener { e ->
            Log.e("TextScan", "Görsel metin tanıma başarısız", e)
            onResult(null)
        }
}

/**
 * Capture photo from camera
 */
private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
        override fun onCaptureSuccess(image: ImageProxy) {
            try {
                // Use the same approach as ARScreen
                val bitmap = image.toBitmap()
                val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
                onPhotoCaptured(rotatedBitmap)
            } catch (e: Exception) {
                Log.e("TextScan", "Bitmap dönüştürme hatası", e)
            } finally {
                image.close()
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("TextScan", "Fotoğraf çekme hatası", exception)
        }
    })
}

/**
 * Extension function to convert ImageProxy to Bitmap
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun ImageProxy.toBitmap(): Bitmap {
    val mediaImage = this.image ?: throw IllegalStateException("Image is null")
    val yBuffer = mediaImage.planes[0].buffer
    val uBuffer = mediaImage.planes[1].buffer
    val vBuffer = mediaImage.planes[2].buffer
    
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    
    val yuvImage = android.graphics.YuvImage(
        nv21,
        android.graphics.ImageFormat.NV21,
        this.width,
        this.height,
        null
    )
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        android.graphics.Rect(0, 0, this.width, this.height),
        100,
        out
    )
    val imageBytes = out.toByteArray()
    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        ?: throw IllegalStateException("Failed to decode bitmap")
}

/**
 * Rotate bitmap
 */
private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Get bitmap from URI
 */
private fun Uri.getBitmap(contentResolver: ContentResolver): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.graphics.ImageDecoder.decodeBitmap(
                android.graphics.ImageDecoder.createSource(contentResolver, this)
            )
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, this)
        }
    } catch (e: Exception) {
        Log.e("TextScan", "Bitmap yükleme hatası", e)
        null
    }
}

private class TextRecognitionAnalyzer(
    private val onTextDetected: (Text, Int, Int, Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        
        val width = imageProxy.width
        val height = imageProxy.height

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onTextDetected(visionText, width, height, rotationDegrees)
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognitionAnalyzer", "Metin tanıma başarısız", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
