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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.disordo.DisordoApplication
import com.disordo.util.ManageCameraPermission
import com.disordo.viewmodel.CameraViewModel
import com.disordo.viewmodel.ViewModelFactory

@Composable
fun CameraScreen() {
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
        CameraPreview(cameraViewModel, onGalleryClick = { galleryLauncher.launch("image/*") })
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Kamera izni gerekli.")
        }
    }
}

@Composable
private fun CameraPreview(
    cameraViewModel: CameraViewModel,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PreviewView(it).apply {
                    this.controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onGalleryClick) {
                Icon(
                    Icons.Default.PhotoLibrary, 
                    contentDescription = "Galeriyi Aç",
                    modifier = Modifier.size(36.dp)
                )
            }
            
            IconButton(
                onClick = { 
                    capturePhoto(context, cameraController) { bitmap ->
                        cameraViewModel.saveCapturedImage(bitmap)
                    }
                },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera, 
                    contentDescription = "Fotoğraf Çek",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.size(36.dp))
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