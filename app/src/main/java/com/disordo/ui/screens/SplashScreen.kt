package com.disordo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Renk paletiniz
val disordo_peach = Color(0xFFFFAC9E)
val disordo_coral = Color(0xFFFF7E6B)
val disordo_brown = Color(0xFF8C5E58)
val disordo_mint = Color(0xFFA9F0D1)
val disordo_cream = Color(0xFFFFF7F8)

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.3f) }
    val rotation = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    LaunchedEffect(key1 = Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(3000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        disordo_cream,
                        disordo_peach.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        // Arka plan dekoratif elementler
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = 100.dp)
                .alpha(0.3f)
                .background(disordo_mint, CircleShape)
        )

        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-30).dp)
                .alpha(0.2f)
                .background(disordo_coral, CircleShape)
        )

        // Ana içerik
        Column(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha.value)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ana logo/ikon - şık ve oyunsu
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale.value)
                    .offset(y = floatingOffset.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dış halka - gradient efekt
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(disordo_coral, disordo_peach)
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(8.dp)
                ) {
                    // İç beyaz alan
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        // D harfi - büyük ve cesur
                        Text(
                            text = "D",
                            fontSize = 72.sp,
                            color = disordo_coral,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Uygulama adı - modern ve canlı
            Text(
                text = "Disordo",
                fontSize = 42.sp,
                color = disordo_coral,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Alt başlık - daha okunabilir
            Text(
                text = "Okuma Arkadaşın",
                fontSize = 18.sp,
                color = disordo_brown,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Animasyonlu nokta göstergesi
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) { index ->
                    val dotScale = remember { Animatable(0.8f) }

                    LaunchedEffect(Unit) {
                        delay(index * 200L)
                        while (true) {
                            dotScale.animateTo(
                                targetValue = 1.2f,
                                animationSpec = tween(400)
                            )
                            dotScale.animateTo(
                                targetValue = 0.8f,
                                animationSpec = tween(400)
                            )
                            delay(600)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .scale(dotScale.value)
                            .background(
                                color = when (index) {
                                    0 -> disordo_coral
                                    1 -> disordo_mint
                                    else -> disordo_peach
                                },
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        // Alt bilgi
        Text(
            text = "Erken Tespit, Güçlü Gelecek",
            fontSize = 14.sp,
            color = disordo_brown.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(alpha.value)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreen(onSplashFinished = {})
    }
}