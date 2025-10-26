package com.disordo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.disordo.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ResultsScreen(
    riskScore: Float = 0.0f,
    isLoading: Boolean = false,
    onBackToHome: () -> Unit = {}
) {
    val alpha = remember { Animatable(0f) }
    val scrollState = rememberScrollState()

    // Çok dinamik arka plan animasyonları
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val float1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float1"
    )
    val float2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float2"
    )
    val rotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    LaunchedEffect(key1 = Unit) {
        alpha.animateTo(targetValue = 1f, animationSpec = tween(600))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        disordo_cream,
                        disordo_peach.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        // Dinamik arka plan şekilleri
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-120).dp, y = float1.dp + 100.dp)
                .alpha(0.4f)
                .rotate(rotate * 0.5f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(disordo_mint, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = float2.dp - 50.dp)
                .alpha(0.3f)
                .rotate(-rotate * 0.3f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(disordo_coral, Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 150.dp)
                .alpha(0.25f)
                .rotate(rotate * 0.4f)
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
                .verticalScroll(scrollState)
                .padding(20.dp)
                .alpha(alpha.value),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                LoadingCard()
            } else {
                // Büyük Hero Kart
                MegaHeroCard(riskScore)

                // Ana Risk Göstergesi
                CircularRiskCard(riskScore)

                // Renkli İstatistikler
                ColorfulStatsRow(riskScore)

                // Süper Öneriler
                ModernRecommendationsCard(riskScore)

                // Sonraki Adımlar
                FunNextStepsCard(riskScore)

                // Bilgilendirme
                CuteInfoCard()

                // Süper Buton
                FloatingHomeButton(onBackToHome)

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun LoadingCard() {
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(key1 = Unit) {
        while (true) {
            scale.animateTo(1.05f, animationSpec = tween(1000))
            scale.animateTo(0.95f, animationSpec = tween(1000))
        }
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            disordo_mint.copy(alpha = 0.7f),
                            disordo_mint.copy(alpha = 0.4f)
                        )
                    )
                )
                .padding(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(scale.value)
                        .background(Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = disordo_coral,
                        strokeWidth = 6.dp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Analiz Ediliyor",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Yapay zeka çalışıyor...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun MegaHeroCard(riskScore: Float) {
    val scale = remember { Animatable(0.85f) }
    val rotation = remember { Animatable(-3f) }

    LaunchedEffect(key1 = Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        rotation.animateTo(
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500),
                repeatMode = RepeatMode.Reverse
            )
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        // Arka plan dekoratif şekil
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 220.dp, y = 30.dp)
                .rotate(rotation.value)
                .background(disordo_peach.copy(alpha = 0.3f), RoundedCornerShape(40.dp))
        )

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(40.dp),
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale.value)
                .border(
                    width = 4.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(disordo_coral, disordo_peach, disordo_mint, disordo_coral)
                    ),
                    shape = RoundedCornerShape(40.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                disordo_coral.copy(alpha = 0.95f),
                                disordo_peach.copy(alpha = 0.9f)
                            )
                        )
                    )
                    .padding(40.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Büyük ikon
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Analiz Tamamlandı!",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sonuçların hazır, hadi bakalım!",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CircularRiskCard(riskScore: Float) {
    val animatedProgress = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    val riskPercentage = (riskScore * 100).toInt()
    val (riskColor, bgColor) = getRiskColors(riskScore)

    LaunchedEffect(riskScore) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
        delay(300)
        animatedProgress.animateTo(
            targetValue = riskScore,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(40.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            bgColor.copy(alpha = 0.15f),
                            Color.White
                        )
                    )
                )
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Circular Progress
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(220.dp)
                ) {
                    // Arka plan çember
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        riskColor.copy(alpha = 0.1f),
                                        riskColor.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )

                    CircularProgressIndicator(
                        progress = animatedProgress.value,
                        modifier = Modifier.size(180.dp),
                        color = riskColor,
                        strokeWidth = 16.dp,
                        trackColor = riskColor.copy(alpha = 0.2f)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = getRiskIcon(riskScore),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = riskColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "%$riskPercentage",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = riskColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Badge
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = riskColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = getRiskIcon(riskScore),
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = getRiskLevelText(riskScore),
                            style = MaterialTheme.typography.titleLarge,
                            color = riskColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = getRiskDescription(riskScore),
                    style = MaterialTheme.typography.bodyLarge,
                    color = disordo_brown.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ColorfulStatsRow(riskScore: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FunStatCard(
            icon = Icons.Default.TrendingUp,
            value = "${(riskScore * 100).toInt()}",
            label = "Risk\nSkoru",
            gradient = listOf(disordo_coral, disordo_coral.copy(alpha = 0.7f)),
            modifier = Modifier.weight(1f)
        )
        FunStatCard(
            icon = Icons.Default.Shield,
            value = "${((1f - riskScore) * 100).toInt()}%",
            label = "Güven\nSeviyesi",
            gradient = listOf(disordo_mint, disordo_mint.copy(alpha = 0.7f)),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FunStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    gradient: List<Color>,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(key1 = Unit) {
        delay(200)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        )
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.scale(scale.value)
    ) {
        Box(
            modifier = Modifier
                .background(brush = Brush.verticalGradient(gradient))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ModernRecommendationsCard(riskScore: Float) {
    val recommendations = getRecommendations(riskScore)
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(key1 = Unit) {
        delay(400)
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(disordo_coral, disordo_peach)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Önerilerimiz",
                    style = MaterialTheme.typography.headlineSmall,
                    color = disordo_brown,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            recommendations.forEachIndexed { index, rec ->
                CuteRecommendationItem(
                    icon = rec.icon,
                    title = rec.title,
                    description = rec.description,
                    color = rec.color
                )
                if (index < recommendations.size - 1) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun CuteRecommendationItem(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.1f))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = disordo_brown,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = disordo_brown.copy(alpha = 0.75f),
                lineHeight = 22.sp,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun FunNextStepsCard(riskScore: Float) {
    val (title, steps) = getNextSteps(riskScore)
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(key1 = Unit) {
        delay(600)
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            disordo_peach.copy(alpha = 0.4f),
                            disordo_peach.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(32.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Rocket,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = disordo_coral
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = disordo_brown,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                steps.forEachIndexed { index, step ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(disordo_coral, disordo_peach)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyLarge,
                            color = disordo_brown.copy(alpha = 0.9f),
                            modifier = Modifier.weight(1f),
                            lineHeight = 22.sp,
                            fontSize = 15.sp
                        )
                    }
                    if (index < steps.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CuteInfoCard() {
    val scale = remember { Animatable(0.9f) }

    LaunchedEffect(key1 = Unit) {
        delay(800)
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = disordo_mint.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
    ) {
        Row(
            modifier = Modifier.padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = disordo_mint
            )
            Text(
                text = "Bu analiz yapay zeka destekli bir ön değerlendirmedir. Kesin teşhis için mutlaka uzman görüşü alınmalıdır.",
                style = MaterialTheme.typography.bodyLarge,
                color = disordo_brown.copy(alpha = 0.85f),
                lineHeight = 22.sp,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun FloatingHomeButton(onClick: () -> Unit) {
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(key1 = Unit) {
        delay(1000)
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .scale(scale.value),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(disordo_coral, disordo_peach)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ana Sayfaya Dön",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper Data Class
data class Recommendation(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color
)

// Helper Functions
fun getRiskColors(riskScore: Float): Pair<Color, Color> {
    return when {
        riskScore < 0.3f -> Pair(Color(0xFF4CAF50), Color(0xFFC8E6C9))
        riskScore < 0.7f -> Pair(Color(0xFFFF9800), Color(0xFFFFE0B2))
        else -> Pair(Color(0xFFF44336), Color(0xFFFFCDD2))
    }
}

fun getRiskIcon(riskScore: Float): ImageVector {
    return when {
        riskScore < 0.3f -> Icons.Default.CheckCircle
        riskScore < 0.7f -> Icons.Default.Info
        else -> Icons.Default.Warning
    }
}

fun getRiskLevelText(riskScore: Float): String {
    return when {
        riskScore < 0.3f -> "Düşük Risk"
        riskScore < 0.7f -> "Orta Risk"
        else -> "Yüksek Risk"
    }
}

fun getRiskDescription(riskScore: Float): String {
    return when {
        riskScore < 0.3f -> "El yazısı gelişimi yaşa uygun seviyede görünmektedir. Harika bir sonuç!"
        riskScore < 0.7f -> "El yazısı gelişiminde bazı farklılıklar gözlemlenmektedir. Takip önerilir."
        else -> "El yazısı gelişiminde önemli farklılıklar tespit edilmiştir. Uzman değerlendirmesi önerilir."
    }
}

fun getRecommendations(riskScore: Float): List<Recommendation> {
    return when {
        riskScore < 0.3f -> listOf(
            Recommendation(
                Icons.Default.EmojiEvents,
                "Harika Bir Başlangıç!",
                "El yazısı gelişimi çok iyi durumda. Günlük yazı pratikleriyle bu seviyeyi koruyun.",
                Color(0xFF4CAF50)
            ),
            Recommendation(
                Icons.Default.MenuBook,
                "Okuma Alışkanlığı",
                "Düzenli kitap okuma alışkanlığını destekleyin. Sesli okuma çalışmaları da çok faydalı.",
                Color(0xFF2196F3)
            ),
            Recommendation(
                Icons.Default.Brush,
                "Yaratıcı Aktiviteler",
                "Resim, boyama gibi el-göz koordinasyonunu geliştiren aktivitelerle devam edin.",
                Color(0xFF9C27B0)
            )
        )
        riskScore < 0.7f -> listOf(
            Recommendation(
                Icons.Default.School,
                "Öğretmen Görüşmesi",
                "Sınıf öğretmeninizle görüşerek ek destek programları hakkında bilgi alın.",
                Color(0xFFFF9800)
            ),
            Recommendation(
                Icons.Default.Games,
                "Eğitici Oyunlar",
                "El yazısı gelişimini destekleyen eğitici uygulamalar ve oyunlar kullanın.",
                Color(0xFF2196F3)
            ),
            Recommendation(
                Icons.Default.CalendarMonth,
                "Düzenli Kontrol",
                "3-6 ay içinde tekrar değerlendirme yaptırmanız önerilir.",
                Color(0xFF9C27B0)
            ),
            Recommendation(
                Icons.Default.FitnessCenter,
                "El Egzersizleri",
                "Günlük 15-20 dakika el yazısı pratiği yapın. Sabır ve süreklilik çok önemli.",
                Color(0xFF00BCD4)
            )
        )
        else -> listOf(
            Recommendation(
                Icons.Default.MedicalServices,
                "Acil: Uzman Değerlendirmesi",
                "En kısa sürede bir çocuk gelişim uzmanı veya disleksi uzmanı ile görüşmeniz şiddetle önerilir.",
                Color(0xFFF44336)
            ),
            Recommendation(
                Icons.Default.Psychology,
                "Erken Müdahale Çok Önemli",
                "Erken müdahale programları çocuğunuzun gelişimini çok önemli ölçüde destekleyebilir. Vakit kaybetmeyin.",
                Color(0xFFFF5722)
            ),
            Recommendation(
                Icons.Default.LocalHospital,
                "Nöropsikolojik Test",
                "Detaylı nöropsikolojik değerlendirme ve testler yaptırmanız gerekiyor.",
                Color(0xFFE91E63)
            ),
            Recommendation(
                Icons.Default.FamilyRestroom,
                "Aile Desteği",
                "Bu süreçte sabırlı ve destekleyici olmak çok önemli. Aile danışmanlığı da alabilirsiniz.",
                Color(0xFF9C27B0)
            )
        )
    }
}

fun getNextSteps(riskScore: Float): Pair<String, List<String>> {
    return when {
        riskScore < 0.3f -> Pair(
            "Gelişimi Sürdürmek İçin",
            listOf(
                "Günlük 15-20 dakika keyifli yazı pratiği yapın",
                "Çocuğunuzun yazdıklarını takdir edin ve cesaretlendirin",
                "Yılda bir kez kontrol değerlendirmesi yaptırın",
                "Okuma ve yazmayı eğlenceli hale getirin"
            )
        )
        riskScore < 0.7f -> Pair(
            "İzlenmesi Gereken Adımlar",
            listOf(
                "Bu hafta içinde okul öğretmeni ile görüşme planlayın",
                "Heceleme ve okuma çalışmalarına ağırlık verin",
                "3-6 ay içinde mutlaka tekrar değerlendirme yaptırın",
                "El-göz koordinasyonunu geliştiren aktivitelere devam edin",
                "Özel eğitim desteği alıp alamayacağınızı araştırın"
            )
        )
        else -> Pair(
            "ACİL YAPILMASI GEREKENLER",
            listOf(
                "BU HAFTA içinde çocuk gelişim uzmanına randevu alın",
                "Okul yönetimiyle acil görüşme talep edin",
                "Detaylı nöropsikolojik değerlendirme için başvurun",
                "Özel eğitim ve rehabilitasyon merkezi araştırın",
                "Sigorta kapsamını kontrol edin, gerekirse raporları hazırlayın"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenLowRiskPreview() {
    MaterialTheme {
        ResultsScreen(riskScore = 0.2f, isLoading = false)
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenMediumRiskPreview() {
    MaterialTheme {
        ResultsScreen(riskScore = 0.5f, isLoading = false)
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenHighRiskPreview() {
    MaterialTheme {
        ResultsScreen(riskScore = 0.85f, isLoading = false)
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenLoadingPreview() {
    MaterialTheme {
        ResultsScreen(riskScore = 0.0f, isLoading = true)
    }
}