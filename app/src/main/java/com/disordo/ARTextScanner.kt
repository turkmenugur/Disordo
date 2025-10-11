package com.disordo

//import android.Manifest
//import android.content.Context
//import android.graphics.PointF
//import android.graphics.RectF
//import android.util.Log
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageAnalysis
//import androidx.camera.core.ImageProxy
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.view.PreviewView
//import androidx.camera.view.TransformExperimental
//import androidx.camera.view.transform.OutputTransform
//import androidx.compose.foundation.Canvas
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.geometry.CornerRadius
//import androidx.compose.ui.geometry.Offset
//import androidx.compose.ui.geometry.Size
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.Path
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalDensity
//import androidx.compose.ui.platform.LocalLifecycleOwner
//import androidx.compose.ui.text.AnnotatedString
//import androidx.compose.ui.text.TextStyle
//import androidx.compose.ui.text.drawText
//import androidx.compose.ui.text.rememberTextMeasurer
//import androidx.compose.ui.unit.Constraints
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.compose.ui.viewinterop.AndroidView
//import androidx.core.content.ContextCompat
//import com.disordo.ui.theme.OpenDyslexic
//import com.google.accompanist.permissions.ExperimentalPermissionsApi
//import com.google.accompanist.permissions.isGranted
//import com.google.accompanist.permissions.rememberPermissionState
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.text.Text
//import com.google.mlkit.vision.text.TextRecognition
//import com.google.mlkit.vision.text.latin.TextRecognizerOptions
//import java.util.concurrent.Executors
//import kotlin.math.atan2
//import kotlin.math.max
//import kotlin.math.roundToInt
//
//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun ARTextScanner(modifier: Modifier = Modifier) {
//    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
//
//    if (cameraPermissionState.status.isGranted) {
//        CameraPreview(modifier = modifier)
//    } else {
//        LaunchedEffect(Unit) {
//            cameraPermissionState.launchPermissionRequest()
//        }
//    }
//}
//
//@OptIn(TransformExperimental::class)
//@Composable
//fun CameraPreview(modifier: Modifier = Modifier) {
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val context = LocalContext.current
//    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//    var recognizedTextLines by remember { mutableStateOf<List<RecognizedTextLine>>(emptyList()) }
//    val density = LocalDensity.current
//    val textMeasurer = rememberTextMeasurer()
//
//    Box(modifier = modifier.fillMaxSize()) {
//        AndroidView(
//            factory = { ctx ->
//                val previewView = PreviewView(ctx)
//                val executor = Executors.newSingleThreadExecutor()
//                cameraProviderFuture.addListener({
//                    val cameraProvider = cameraProviderFuture.get()
//                    val preview = Preview.Builder().build().also {
//                        it.setSurfaceProvider(previewView.surfaceProvider)
//                    }
//
//                        val imageAnalysis = ImageAnalysis.Builder()
//                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                            .build()
//                            .also {
//                                it.setAnalyzer(executor, TextRecognitionAnalyzer(context, previewView) { textLines ->
//                                    recognizedTextLines = textLines
//                                })
//                            }
//
//                    val useCaseGroup = androidx.camera.core.UseCaseGroup.Builder()
//                        .addUseCase(preview)
//                        .addUseCase(imageAnalysis)
//                        .setViewPort(previewView.viewPort!!)
//                        .build()
//
//                    cameraProvider.unbindAll()
//                    cameraProvider.bindToLifecycle(
//                        lifecycleOwner,
//                        CameraSelector.DEFAULT_BACK_CAMERA,
//                        useCaseGroup
//                    )
//                }, ContextCompat.getMainExecutor(ctx))
//                previewView
//            },
//            modifier = Modifier.fillMaxSize(),
//        )
//
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            val canvasWidth = size.width
//            val canvasHeight = size.height
//
//            recognizedTextLines.forEach { line ->
//                val rect = line.rect.coerceWithin(canvasWidth, canvasHeight)
//                if (rect.width() <= 0f || rect.height() <= 0f) return@forEach
//
//                val backgroundPath = line.cornerPath()
//                val highlightColor = Color.Black.copy(alpha = 0.35f)
//
//                if (backgroundPath != null) {
//                    drawPath(backgroundPath, color = highlightColor)
//                } else {
//                    drawRoundRect(
//                        color = highlightColor,
//                        topLeft = Offset(rect.left, rect.top),
//                        size = Size(rect.width(), rect.height()),
//                        cornerRadius = CornerRadius(10.dp.toPx())
//                    )
//                }
//
//                val fontSizePx = rect.height() * 0.75f
//                val fontSizeSp = max(14f, fontSizePx / density.density).sp
//
//                val textLayoutResult = textMeasurer.measure(
//                    text = AnnotatedString(line.text),
//                    style = TextStyle(
//                        fontFamily = OpenDyslexic,
//                        fontSize = fontSizeSp,
//                        color = Color.White
//                    ),
//                    constraints = Constraints(
//                        maxWidth = rect.width().roundToInt().coerceAtLeast(1)
//                    ),
//                    maxLines = 1
//                )
//
//                val textLeft = rect.left + 6.dp.toPx()
//                val textTop = rect.top + (rect.height() - textLayoutResult.size.height) / 2f
//
//                drawText(
//                    textLayoutResult,
//                    topLeft = Offset(textLeft, textTop)
//                )
//            }
//        }
//    }
//}
//private data class RecognizedTextLine(
//    val id: String,
//    val text: String,
//    val rect: RectF,
//    val cornerPoints: List<PointF>
//) {
//    fun cornerPath(): androidx.compose.ui.graphics.Path? {
//        if (cornerPoints.size < 4) return null
//        val path = Path()
//        val orderedPoints = orderCornerPoints(cornerPoints)
//        path.moveTo(orderedPoints[0].x, orderedPoints[0].y)
//        for (i in 1 until orderedPoints.size) {
//            path.lineTo(orderedPoints[i].x, orderedPoints[i].y)
//        }
//        path.close()
//        return path
//    }
//
//    private fun orderCornerPoints(points: List<PointF>): List<PointF> {
//        val centroidX = points.sumOf { it.x.toDouble() } / points.size
//        val centroidY = points.sumOf { it.y.toDouble() } / points.size
//        return points.sortedBy { atan2((it.y - centroidY).toDouble(), (it.x - centroidX).toDouble()) }
//    }
//}
//private fun RectF.coerceWithin(maxWidth: Float, maxHeight: Float): RectF {
//    if (maxWidth <= 0f || maxHeight <= 0f) return RectF()
//
//    val width = width().coerceAtMost(maxWidth)
//    val height = height().coerceAtMost(maxHeight)
//
//    val maxLeft = maxWidth - width
//    val maxTop = maxHeight - height
//
//    val left = left.coerceIn(0f, maxLeft)
//    val top = top.coerceIn(0f, maxTop)
//
//    return RectF(left, top, left + width, top + height)
//}
//private class TextRecognitionAnalyzer(
//    private val context: Context,
//    private val previewView: PreviewView,
//    private val onTextRecognized: (List<RecognizedTextLine>) -> Unit
//) : ImageAnalysis.Analyzer {
//    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//    private var lastAnalyzedTimestamp = 0L
//
//    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class, TransformExperimental::class)
//    override fun analyze(imageProxy: ImageProxy) {
//        val currentTimestamp = System.currentTimeMillis()
//        if (currentTimestamp - lastAnalyzedTimestamp < 250) { // Analyze every 250ms
//            imageProxy.close()
//            return
//        }
//
//        val mediaImage = imageProxy.image
//        if (mediaImage != null) {
//            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//            recognizer.process(image)
//                .addOnSuccessListener { visionText ->
//                    val source = androidx.camera.view.transform.ImageProxyTransformFactory().getOutputTransform(imageProxy)
//                    val target: OutputTransform? = previewView.outputTransform
//                    val coordinateTransform = androidx.camera.view.transform.CoordinateTransform(source, target)
//
//                    val lines = mutableListOf<RecognizedTextLine>()
//                    visionText.textBlocks.forEach { block ->
//                        block.lines.forEachIndexed { index, line ->
//                            val boundingBox = line.boundingBox ?: return@forEachIndexed
//                            val transformedRect = RectF(boundingBox)
//                            coordinateTransform.mapRect(transformedRect)
//
//                            val points = line.cornerPoints?.map { point ->
//                                val transformedPoint = android.graphics.PointF(point.x.toFloat(), point.y.toFloat())
//                                coordinateTransform.mapPoint(transformedPoint)
//                                PointF(transformedPoint.x, transformedPoint.y)
//                            } ?: emptyList()
//
//                            lines += RecognizedTextLine(
//                                id = "${block.hashCode()}_$index",
//                                text = line.text,
//                                rect = transformedRect,
//                                cornerPoints = points
//                            )
//                        }
//                    }
//
//                    onTextRecognized(lines)
//                    lastAnalyzedTimestamp = currentTimestamp
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ARTextScanner", "Text recognition failed", e)
//                }
//                .addOnCompleteListener {
//                    imageProxy.close()
//                }
//        }
//    }
//}