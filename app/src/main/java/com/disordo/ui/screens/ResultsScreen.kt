package com.disordo.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun ResultsScreen(
    riskScore: Float = 0.0f, // 0.0 - 1.0 arası risk skoru
    isLoading: Boolean = false
) {
    val alpha = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(1500)
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .alpha(alpha.value),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Başlık
        HeaderCard()
        
        // Risk Skoru Kartı
        RiskScoreCard(riskScore = riskScore, isLoading = isLoading)
        
        // Öneriler Kartı
        RecommendationsCard(riskScore = riskScore)
        
        // Ebeveyn Bilgilendirme Kartı
        ParentInfoCard()
    }
}

@Composable
fun HeaderCard() {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = "Analiz Sonucu",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "📊 Analiz Sonuçları",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El yazısı analizi tamamlandı! 🎉\nRisk skoru ve öneriler aşağıda görüntülenmektedir.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RiskScoreCard(riskScore: Float, isLoading: Boolean) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = getRiskColor(riskScore).copy(alpha = 0.1f)
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analiz ediliyor... 🤖",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                // Risk Skoru Gösterimi
                Text(
                    text = "Risk Skoru",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Yüzde gösterimi
                Text(
                    text = "${(riskScore * 100).toInt()}%",
                    style = MaterialTheme.typography.displayLarge,
                    color = getRiskColor(riskScore),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = riskScore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = getRiskColor(riskScore),
                    trackColor = getRiskColor(riskScore).copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Risk seviyesi
                Text(
                    text = getRiskLevel(riskScore),
                    style = MaterialTheme.typography.titleMedium,
                    color = getRiskColor(riskScore),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = getRiskDescription(riskScore),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun RecommendationsCard(riskScore: Float) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Öneriler",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            when {
                riskScore < 0.3f -> {
                    RecommendationItem(
                        icon = Icons.Default.CheckCircle,
                        text = "Harika! El yazısı gelişimi normal seviyede.",
                        color = Color(0xFF4CAF50)
                    )
                }
                riskScore < 0.6f -> {
                    RecommendationItem(
                        icon = Icons.Default.Info,
                        text = "El yazısı pratiği yaparak gelişimi destekleyebilirsiniz.",
                        color = Color(0xFF2196F3)
                    )
                }
                else -> {
                    RecommendationItem(
                        icon = Icons.Default.Warning,
                        text = "Uzman desteği almanız önerilir. Uzman değerlendirmesi yapılabilir..️",
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun ParentInfoCard() {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "👨‍👩‍👧‍👦 Ebeveyn Bilgilendirmesi",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "Bu analiz sadece bir ön değerlendirmedir. Kesin teşhis için uzman görüşü alınması önerilir. Çocuğunuzun gelişimi için düzenli takip yapmanız faydalı olacaktır.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Yardımcı fonksiyonlar
fun getRiskColor(riskScore: Float): Color {
    return when {
        riskScore < 0.3f -> Color(0xFF4CAF50) // Yeşil - Düşük risk
        riskScore < 0.6f -> Color(0xFFFF9800) // Turuncu - Orta risk
        else -> Color(0xFFF44336) // Kırmızı - Yüksek risk
    }
}

fun getRiskLevel(riskScore: Float): String {
    return when {
        riskScore < 0.3f -> "🟢 Düşük Risk"
        riskScore < 0.6f -> "🟡 Orta Risk"
        else -> "🔴 Yüksek Risk"
    }
}

fun getRiskDescription(riskScore: Float): String {
    return when {
        riskScore < 0.3f -> "El yazısı gelişimi normal seviyede görünüyor."
        riskScore < 0.6f -> "El yazısı gelişiminde bazı zorluklar olabilir."
        else -> "El yazısı gelişiminde önemli zorluklar tespit edildi."
    }
}

@Preview(showBackground = true)
@Composable
fun ResultsScreenPreview() {
    MaterialTheme {
        ResultsScreen(riskScore = 0.45f, isLoading = false)
    }
}

