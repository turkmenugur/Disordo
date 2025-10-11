package com.disordo

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.disordo.ui.theme.OpenDyslexic
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

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
        Text("Metin taramak için kamera izni gereklidir.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRequestPermission) {
            Text("İzin Ver")
        }
    }
}

@Composable
private fun TextScanCameraView(modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var recognizedText by remember { mutableStateOf("") }
    var isFrozen by remember { mutableStateOf(false) }

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
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, TextRecognitionAnalyzer {
                                if (!isFrozen) {
                                    recognizedText = it
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

        if (isFrozen) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)))
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TextDisplayPanel(
                text = recognizedText,
                isFrozen = isFrozen
            )
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(
                onClick = { isFrozen = !isFrozen },
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
private fun TextDisplayPanel(
    text: String,
    isFrozen: Boolean,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val height by animateDpAsState(
        targetValue = if (isFrozen) screenHeight * 0.6f else 150.dp,
        label = "PanelHeightAnimation"
    )

    if (text.isNotBlank()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp)
                .animateContentSize()
        ) {
            Column {
                if (isFrozen) {
                    Text(
                        text = "Tanınan Metin",
                        fontFamily = OpenDyslexic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                LazyColumn(state = lazyListState) {
                    item {
                        Text(
                            text = text,
                            fontFamily = OpenDyslexic,
                            fontSize = if (isFrozen) 18.sp else 14.sp,
                            color = Color.White,
                            lineHeight = if (isFrozen) 28.sp else 20.sp
                        )
                    }
                }
            }
        }

        LaunchedEffect(text) {
            if (lazyListState.firstVisibleItemIndex < 1 && lazyListState.firstVisibleItemScrollOffset < 100) {
                coroutineScope.launch {
                    lazyListState.animateScrollToItem(0)
                }
            }
        }
    }
}

private class TextRecognitionAnalyzer(
    private val onTextUpdated: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                onTextUpdated(visionText.text)
            }
            .addOnFailureListener { e ->
                Log.e("TextRecognitionAnalyzer", "Metin tanıma başarısız", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
