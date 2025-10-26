package com.disordo.ui.screens

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.disordo.DisordoApplication
import com.disordo.ml.DyslexiaResult
import com.disordo.ui.theme.disordo_brown
import com.disordo.ui.theme.disordo_coral
import com.disordo.ui.theme.disordo_coral_light
import com.disordo.ui.theme.disordo_mint
import com.disordo.ui.theme.disordo_mint_light
import com.disordo.ui.theme.disordo_peach
import com.disordo.util.ManageCameraPermission
import com.disordo.viewmodel.CameraViewModel
import com.disordo.viewmodel.ViewModelFactory

@Composable
fun CameraScreen(
    onNavigateToResults: (Float) -> Unit = {}
) {
    val application = LocalContext.current.applicationContext as DisordoApplication
    val cameraViewModel: CameraViewModel = viewModel(factory = ViewModelFactory(application))
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                val bitmap = it.getBitmap(context.contentResolver)
                bitmap?.let { bm -> cameraViewModel.saveSelectedImage(bm) }
            }
        }
    )

    ManageCameraPermission(
        context = context,
        onPermissionGranted = { hasPermission = true },
        onPermissionDenied = { /* İzin reddedildi, kullanıcıya bilgi ver */ },
        onPermissionRevoked = { /* İzin iptal edildi, kullanıcıya bilgi ver */ }
    )

    if (hasPermission) {
        CameraPreview(
            cameraViewModel = cameraViewModel,
            onGalleryClick = { galleryLauncher.launch("image/*") },
            onNavigateToResults = onNavigateToResults
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Kamera İkonu",
                        modifier = Modifier.padding(bottom = 16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Kamera İzni Gerekli",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Fotoğraf çekmek için kamera iznine ihtiyacımız var. Lütfen ayarlardan izin verin.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    cameraViewModel: CameraViewModel,
    onGalleryClick: () -> Unit,
    onNavigateToResults: (Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    
    // State'leri collect et
    val analysisResult by cameraViewModel.analysisResult.collectAsState()
    val isAnalyzing by cameraViewModel.isAnalyzing.collectAsState()
    
    // Çekilen fotoğrafı tut
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Analiz tamamlandığında ResultsScreen'e yönlendir
    LaunchedEffect(analysisResult) {
        analysisResult?.let { result ->
            if (result.errorMessage == null) {
                // Analiz başarılı, ResultsScreen'e git
                onNavigateToResults(result.riskScore)
                cameraViewModel.clearAnalysisResult()
                capturedBitmap = null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Kamera preview - sadece fotoğraf çekilmemişse göster
        if (capturedBitmap == null) {
            AndroidView(
                factory = {
                    PreviewView(it).apply {
                        this.controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Çekilen fotoğraf önizlemesi
        AnimatedVisibility(
            visible = capturedBitmap != null && !isAnalyzing && analysisResult == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            capturedBitmap?.let { bitmap ->
                PhotoPreviewScreen(
                    bitmap = bitmap,
                    onConfirm = {
                        cameraViewModel.saveCapturedImage(bitmap)
                    },
                    onRetake = {
                        capturedBitmap = null
                    }
                )
            }
        }

        // Analiz loading ve sonuç overlay
        AnimatedVisibility(
            visible = isAnalyzing || analysisResult != null,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            if (isAnalyzing) {
                CameraAnalyzingCard()
            } else if (analysisResult != null) {
                CameraAnalysisResultCard(
                    result = analysisResult!!,
                    onDismiss = { 
                        cameraViewModel.clearAnalysisResult()
                        capturedBitmap = null // Sonuç kapatıldığında fotoğrafı da temizle
                    }
                )
            }
        }

        // Kamera kontrolleri - sadece fotoğraf çekilmemişse göster
        if (capturedBitmap == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Galeri butonu
                IconButton(
                    onClick = onGalleryClick,
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isAnalyzing) 
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            else 
                                MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary, 
                        contentDescription = "Galeriyi Aç",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Ana fotoğraf çekme butonu
                IconButton(
                    onClick = { 
                        capturePhoto(context, cameraController) { bitmap ->
                            capturedBitmap = bitmap // Önce önizleme göster
                        }
                    },
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = if (isAnalyzing)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else
                                MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.PhotoCamera, 
                        contentDescription = "Fotoğraf Çek",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Boş alan (simetri için)
                Spacer(modifier = Modifier.size(56.dp))
            }
        }
    }
}

private fun capturePhoto(context: Context, cameraController: CameraController, onPhotoCaptured: (Bitmap) -> Unit) {
    val executor = ContextCompat.getMainExecutor(context)
    cameraController.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val rotatedBitmap = image.toBitmap().rotate(image.imageInfo.rotationDegrees.toFloat())
            onPhotoCaptured(rotatedBitmap)
            image.close()
        }
    })
}

fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

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

@Composable
fun PhotoPreviewScreen(
    bitmap: Bitmap,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    val scale = remember { Animatable(0.9f) }
    
    LaunchedEffect(key1 = Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Fotoğraf önizlemesi
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Çekilen Fotoğraf",
            modifier = Modifier
                .fillMaxSize()
                .scale(scale.value),
            contentScale = ContentScale.Fit
        )
        
        // Üstte bilgi kartı
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = disordo_brown.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Fotoğrafı İncele",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Göndermek için onaylayın veya tekrar çekin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // Altta butonlar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tekrar çek butonu
            Card(
                onClick = onRetake,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = disordo_peach
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Tekrar Çek",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tekrar Çek",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Analiz et butonu
            Card(
                onClick = onConfirm,
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = disordo_coral
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Analiz Et",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analiz Et",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CameraAnalyzingCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = "Analiz ediliyor",
                    modifier = Modifier
                        .size(48.dp)
                        .rotate(rotation),
                    tint = disordo_coral
                )
                Column {
                    Text(
                        text = "Analiz Ediliyor...",
                        style = MaterialTheme.typography.titleLarge,
                        color = disordo_brown,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "Yapay zeka inceliyor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = disordo_brown.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun CameraAnalysisResultCard(
    result: DyslexiaResult,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.8f) }
    
    LaunchedEffect(key1 = Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isDyslexiaDetected) 
                disordo_peach.copy(alpha = 0.95f) 
            else 
                disordo_mint.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (result.errorMessage == null) 
                            Icons.Default.CheckCircle 
                        else 
                            Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (result.errorMessage == null) 
                            Color.White 
                        else 
                            Color.Red
                    )
                    Text(
                        text = "Sonuç",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (result.errorMessage != null) {
                Text(
                    text = result.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CameraResultItem(
                        label = "Risk",
                        value = result.getRiskLevelText()
                    )
                    CameraResultItem(
                        label = "Yüzde",
                        value = "${result.getRiskPercentage()}%"
                    )
                    CameraResultItem(
                        label = "Güven",
                        value = "${(result.confidence * 100).toInt()}%"
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (result.isDyslexiaDetected) {
                            "⚠️ Disleksi riski tespit edildi"
                        } else {
                            "✅ Normal değerler"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun CameraResultItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}