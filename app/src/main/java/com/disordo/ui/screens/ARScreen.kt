package com.disordo.ui.screens

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.disordo.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min
import android.graphics.Matrix

data class RecognizedTextLine(
    val text: String,
    val boundingBox: android.graphics.Rect,
    val cornerPoints: Array<android.graphics.Point>?
)

@Composable
fun ARScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    
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
        ARTextOverlayScreen()
    } else {
        ARPermissionScreen { launcher.launch(Manifest.permission.CAMERA) }
    }
}

@Composable
private fun ARPermissionScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(disordo_cream),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Kamera",
                    modifier = Modifier.size(80.dp),
                    tint = disordo_coral
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Kamera İzni Gerekli",
                    style = MaterialTheme.typography.headlineMedium,
                    color = disordo_brown,
                    fontFamily = OpenDyslexic
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "AR metin okuyucu için kamera iznine ihtiyacımız var.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = disordo_brown.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    fontFamily = OpenDyslexic
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = disordo_coral
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "İzin Ver",
                        modifier = Modifier.padding(8.dp),
                        fontFamily = OpenDyslexic
                    )
                }
            }
        }
    }
}

@Composable
private fun ARTextOverlayScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var recognizedTextLines by remember { mutableStateOf<List<RecognizedTextLine>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var showCapturedImage by remember { mutableStateOf(false) }
    var capturedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showNoTextMessage by remember { mutableStateOf(false) }
    
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val cameraController = remember { mutableStateOf<ImageCapture?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val bitmap = it.getBitmap(context.contentResolver)
                bitmap?.let { bm ->
                    isProcessing = true
                    processImageForText(bm) { lines ->
                        if (lines.isEmpty()) {
                            showNoTextMessage = true
                            isProcessing = false
                        } else {
                            capturedImageBitmap = bm
                            recognizedTextLines = lines
                            showCapturedImage = true
                            // İşlem bitince animasyonu kapat
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                delay(1500)
                                isProcessing = false
                            }
                        }
                    }
                }
            }
        }
    )

    // Metin yoksa mesajı göster
    LaunchedEffect(showNoTextMessage) {
        if (showNoTextMessage) {
            delay(3000)
            showNoTextMessage = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Kamera Preview veya Çekilen Görsel
        if (showCapturedImage && capturedImageBitmap != null) {
            // Çekilen/Seçilen görsel
            StaticImageOverlayView(
                bitmap = capturedImageBitmap!!,
                textLines = recognizedTextLines,
                onClose = {
                    showCapturedImage = false
                    capturedImageBitmap = null
                    recognizedTextLines = emptyList()
                }
            )
        } else {
            // Real-time kamera preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        // ImageAnalysis - Optimize edilmiş
                        val imageAnalyzer = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, OptimizedARTextAnalyzer { lines ->
                                    recognizedTextLines = lines
                                })
                            }
                        
                        // ImageCapture
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        
                        cameraController.value = imageCapture
                        
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalyzer,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("ARScreen", "Kamera bağlanamadı", exc)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // AR Text Overlay - Real-time
            ARTextOverlay(textLines = recognizedTextLines)
        }

        // AI Processing Animasyonu - Sadece işlem sırasında
        if (isProcessing) {
            EdgeAIAnimation()
        }

        // Metin bulunamadı mesajı
        if (showNoTextMessage) {
            NoTextFoundMessage()
        }

        // Alt Butonlar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            BottomControls(
                onCaptureClick = {
                    cameraController.value?.let { capture ->
                        isProcessing = true
                        capturePhoto(context, capture) { bitmap ->
                            processImageForText(bitmap) { lines ->
                                if (lines.isEmpty()) {
                                    showNoTextMessage = true
                                    isProcessing = false
                                } else {
                                    capturedImageBitmap = bitmap
                                    recognizedTextLines = lines
                                    showCapturedImage = true
                                    // İşlem bitince animasyonu kapat
                                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                        delay(1500)
                                        isProcessing = false
                                    }
                                }
                            }
                        }
                    }
                },
                onGalleryClick = {
                    galleryLauncher.launch("image/*")
                }
            )
        }
    }
}

@Composable
private fun NoTextFoundMessage() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = disordo_coral.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Görselde metin bulunamadı",
                modifier = Modifier.padding(24.dp),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = OpenDyslexic
            )
        }
    }
}

@Composable
private fun ARTextOverlay(textLines: List<RecognizedTextLine>) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        textLines.forEach { line ->
            val rect = line.boundingBox
            
            // Arka plan (okunabilirlik için)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.75f),
                topLeft = Offset(rect.left.toFloat() - 4f, rect.top.toFloat() - 4f),
                size = Size(rect.width().toFloat() + 8f, rect.height().toFloat() + 8f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
            )

            // Open Dyslexia font ile metin - tam hizalı
            val fontSize = with(density) { 
                min((rect.height() * 0.9f).toSp().value, 24f).sp
            }
            
            val textLayoutResult = textMeasurer.measure(
                text = line.text,
                style = TextStyle(
                    color = Color.White,
                    fontSize = fontSize,
                    fontFamily = OpenDyslexic
                )
            )

            // Metni tam ortala
            val textX = rect.left.toFloat() + (rect.width() - textLayoutResult.size.width) / 2f
            val textY = rect.top.toFloat() + (rect.height() - textLayoutResult.size.height) / 2f

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY)
            )
        }
    }
}

// Ekran kenarlarından çıkan/kaybolan AI animasyonu
@Composable
private fun EdgeAIAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "edge_ai_anim")
    
    // Sol kenardan sağa akış
    val leftProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "left_flow"
    )

    // Sağ kenardan sola akış
    val rightProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "right_flow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Gradient renkleri
        val colors = listOf(
            disordo_coral,
            disordo_peach,
            disordo_mint,
            Color.Transparent
        )

        // Sol kenar - yukarıdan aşağıya
        val leftGradient = Brush.verticalGradient(
            colors = colors,
            startY = height * leftProgress - height * 0.3f,
            endY = height * leftProgress + height * 0.3f
        )
        
        drawRect(
            brush = leftGradient,
            topLeft = Offset(0f, 0f),
            size = Size(8.dp.toPx(), height)
        )

        // Sağ kenar - aşağıdan yukarıya
        val rightGradient = Brush.verticalGradient(
            colors = colors.reversed(),
            startY = height * (1 - rightProgress) - height * 0.3f,
            endY = height * (1 - rightProgress) + height * 0.3f
        )
        
        drawRect(
            brush = rightGradient,
            topLeft = Offset(width - 8.dp.toPx(), 0f),
            size = Size(8.dp.toPx(), height)
        )

        // Üst kenar - soldan sağa
        val topGradient = Brush.horizontalGradient(
            colors = colors,
            startX = width * leftProgress - width * 0.3f,
            endX = width * leftProgress + width * 0.3f
        )
        
        drawRect(
            brush = topGradient,
            topLeft = Offset(0f, 0f),
            size = Size(width, 8.dp.toPx())
        )

        // Alt kenar - sağdan sola
        val bottomGradient = Brush.horizontalGradient(
            colors = colors.reversed(),
            startX = width * (1 - rightProgress) - width * 0.3f,
            endX = width * (1 - rightProgress) + width * 0.3f
        )
        
        drawRect(
            brush = bottomGradient,
            topLeft = Offset(0f, height - 8.dp.toPx()),
            size = Size(width, 8.dp.toPx())
        )
    }
}

@Composable
private fun BottomControls(
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Galeri Butonu
        FloatingActionButton(
            onClick = onGalleryClick,
            containerColor = disordo_mint.copy(alpha = 0.9f),
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

        // Fotoğraf Çekme Butonu
        FloatingActionButton(
            onClick = onCaptureClick,
            containerColor = disordo_coral.copy(alpha = 0.9f),
            shape = CircleShape,
            modifier = Modifier.size(72.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
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

@Composable
private fun StaticImageOverlayView(
    bitmap: Bitmap,
    textLines: List<RecognizedTextLine>,
    onClose: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Görsel
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Çekilen/Seçilen görsel",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Text overlay
        ARTextOverlay(textLines = textLines)

        // Kapat butonu
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Kapat",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// Optimize edilmiş Text Recognition Analyzer - Debounce ile
private class OptimizedARTextAnalyzer(
    private val onTextDetected: (List<RecognizedTextLine>) -> Unit
) : ImageAnalysis.Analyzer {

    private val textRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    private var lastProcessTime = 0L
    private val processingInterval = 500L // 500ms aralıklarla işle
    private var isProcessing = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Çok sık işlemeyi önle
        if (isProcessing || (currentTime - lastProcessTime) < processingInterval) {
            imageProxy.close()
            return
        }

        isProcessing = true
        lastProcessTime = currentTime

        val mediaImage = imageProxy.image ?: run { 
            imageProxy.close()
            isProcessing = false
            return 
        }
        
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks.flatMap { block ->
                    block.lines.map { line ->
                        RecognizedTextLine(
                            text = line.text,
                            boundingBox = line.boundingBox ?: android.graphics.Rect(),
                            cornerPoints = line.cornerPoints
                        )
                    }
                }
                onTextDetected(lines)
            }
            .addOnFailureListener { e ->
                Log.e("ARTextAnalyzer", "Metin tanıma başarısız", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
                isProcessing = false
            }
    }
}

// Utility fonksiyonlar
private fun Uri.getBitmap(contentResolver: ContentResolver): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, this))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(contentResolver, this)
        }
    } catch (e: Exception) {
        null
    }
}

private fun processImageForText(bitmap: Bitmap, onResult: (List<RecognizedTextLine>) -> Unit) {
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val lines = visionText.textBlocks.flatMap { block ->
                block.lines.map { line ->
                    RecognizedTextLine(
                        text = line.text,
                        boundingBox = line.boundingBox ?: android.graphics.Rect(),
                        cornerPoints = line.cornerPoints
                    )
                }
            }
            onResult(lines)
        }
        .addOnFailureListener { e ->
            Log.e("ARScreen", "Görsel metin tanıma başarısız", e)
            onResult(emptyList())
        }
}

private fun capturePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val bitmap = image.toBitmap()
            val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
            onPhotoCaptured(rotatedBitmap)
            image.close()
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e("ARScreen", "Fotoğraf çekme hatası", exception)
        }
    })
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
