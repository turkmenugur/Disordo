package com.disordo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
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

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
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
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
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

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Spacer(modifier = Modifier.weight(1f))

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
                    .padding(bottom = 32.dp)
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
