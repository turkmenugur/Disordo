package com.disordo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.disordo.DisordoApplication
import com.disordo.data.local.entity.UploadedImage
import com.disordo.ui.components.ImageCard
import com.disordo.ui.theme.*
import com.disordo.viewmodel.HomeViewModel
import com.disordo.viewmodel.ViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onNavigateToCamera: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf<UploadedImage?>(null) }
    val alpha = remember { Animatable(0f) }

    val context = LocalContext.current
    val application = context.applicationContext as? DisordoApplication
    val images = if (application != null) {
        val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(application))
        homeViewModel.images.collectAsState().value
    } else {
        emptyList()
    }

    LaunchedEffect(key1 = Unit) {
        alpha.animateTo(targetValue = 1f, animationSpec = tween(600))
    }

    // Animasyonlu arka plan balonları
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(disordo_cream)
    ) {
        // Animasyonlu arka plan dekorasyonları
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = (-100).dp, y = float1.dp + 80.dp)
                .alpha(0.4f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(disordo_mint, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = float2.dp - 50.dp)
                .alpha(0.3f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(disordo_peach, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .alpha(alpha.value),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Section
            HeroCard()

            // Hızlı Aksiyonlar
            ActionButtons(
                onNavigateToCamera = onNavigateToCamera,
                onNavigateToGallery = onNavigateToGallery
            )

            // Progress kartı
            ProgressCard()

            // Motivasyon kartı
            MotivationCard()
        }

        // Süper büyük FAB
        FloatingActionButton(
            onClick = onNavigateToCamera,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(28.dp)
                .size(80.dp),
            containerColor = disordo_coral,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 12.dp,
                pressedElevation = 16.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Fotoğraf Çek",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Composable
fun HeroCard() {
    val scale = remember { Animatable(0.9f) }
    val rotation = remember { Animatable(-2f) }

    LaunchedEffect(key1 = Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        rotation.animateTo(
            targetValue = 2f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        // Arka plan şekiller
        Box(
            modifier = Modifier
                .size(120.dp)
                .offset(x = 250.dp, y = 20.dp)
                .rotate(rotation.value)
                .background(disordo_mint.copy(alpha = 0.3f), RoundedCornerShape(30.dp))
        )

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 3.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(disordo_coral, disordo_peach, disordo_mint)
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                disordo_coral.copy(alpha = 0.9f),
                                disordo_peach.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Büyük ikon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                Color.White.copy(alpha = 0.3f),
                                RoundedCornerShape(24.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "Hadi Başlayalım!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            fontSize = 28.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "El yazını çek, yapay zeka analiz etsin",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.95f),
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    onNavigateToCamera: () -> Unit,
    onNavigateToGallery: () -> Unit
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = Unit) {
        delay(200)
        alpha.animateTo(targetValue = 1f, animationSpec = tween(800))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BigActionButton(
            icon = Icons.Default.CameraAlt,
            text = "Fotoğraf\nÇek",
            gradient = listOf(disordo_coral, disordo_peach),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToCamera
        )
        BigActionButton(
            icon = Icons.Default.PhotoLibrary,
            text = "Galeriden\nSeç",
            gradient = listOf(disordo_mint, disordo_mint.copy(alpha = 0.7f)),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToGallery
        )
    }
}

@Composable
fun BigActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = gradient)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 16.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun ProgressCard() {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = Unit) {
        delay(400)
        alpha.animateTo(targetValue = 1f, animationSpec = tween(800))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = disordo_coral
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "İlerleme Durumu",
                    style = MaterialTheme.typography.headlineSmall,
                    color = disordo_brown,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProgressStatItem(
                    icon = Icons.Default.CheckCircle,
                    value = "0",
                    label = "Tamamlanan\nAnaliz",
                    color = disordo_coral
                )
                ProgressStatItem(
                    icon = Icons.Default.CalendarToday,
                    value = "0",
                    label = "Bu Hafta\nYapılan",
                    color = disordo_mint
                )
                ProgressStatItem(
                    icon = Icons.Default.Stars,
                    value = "-",
                    label = "Risk\nSkoru",
                    color = disordo_peach
                )
            }
        }
    }
}

@Composable
fun ProgressStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(key1 = Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale.value)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = color,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = disordo_brown.copy(alpha = 0.8f),
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 16.sp,
            fontSize = 12.sp
        )
    }
}

@Composable
fun MotivationCard() {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.95f) }

    LaunchedEffect(key1 = Unit) {
        delay(600)
        alpha.animateTo(targetValue = 1f, animationSpec = tween(800))
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha.value)
            .scale(scale.value)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            disordo_mint.copy(alpha = 0.6f),
                            disordo_mint.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = disordo_brown
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "İlk Adımı At!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = disordo_brown,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                    fontSize = 26.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El yazını çekerek başlayalım.\nYapay zeka sana özel rapor hazırlayacak!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = disordo_brown.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 24.sp,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen()
    }
}