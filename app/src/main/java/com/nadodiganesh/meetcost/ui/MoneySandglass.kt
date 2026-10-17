package com.nadodiganesh.meetcost.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class)
@Composable
fun MoneySandglass(
    isTracking: Boolean,
    burnRate: Double, // USD per second
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    
    // Using a list that doesn't trigger recomposition for every particle update
    val particles = remember { mutableListOf<Particle>() }
    
    val infiniteTransition = rememberInfiniteTransition(label = "SandglassTransition")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AnimValue"
    )

    var lastSpawnTime by remember { mutableLongStateOf(0L) }
    val spawnIntervalMs = if (burnRate > 0) (150 / burnRate).toLong().coerceIn(50, 1500) else 2000L

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2

        // Update logic inside the draw loop or side effect
        val currentTime = System.currentTimeMillis()
        if (isTracking && burnRate > 0 && currentTime - lastSpawnTime > spawnIntervalMs) {
            particles.add(Particle(
                x = 0.5f + (Random.nextFloat() - 0.5f) * 0.03f,
                y = 0.15f,
                speed = 0.012f + Random.nextFloat() * 0.01f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 10f,
                drift = (Random.nextFloat() - 0.5f) * 0.005f
            ))
            lastSpawnTime = currentTime
        }
        
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.y += p.speed
            p.x += p.drift + (sin(p.y * 10f) * 0.002f)
            p.rotation += p.rotSpeed
            if (p.y > 0.88f) {
                iterator.remove()
            }
        }

        val glassPath = Path().apply {
            moveTo(w * 0.35f, h * 0.1f)
            lineTo(w * 0.65f, h * 0.1f)
            cubicTo(w * 0.65f, h * 0.42f, cx + 10, cy - 10, cx + 10, cy)
            cubicTo(cx + 10, cy + 10, w * 0.65f, h * 0.58f, w * 0.65f, h * 0.9f)
            lineTo(w * 0.35f, h * 0.9f)
            cubicTo(w * 0.35f, h * 0.58f, cx - 10, cy + 10, cx - 10, cy)
            cubicTo(cx - 10, cy - 10, w * 0.35f, h * 0.42f, w * 0.35f, h * 0.1f)
            close()
        }

        drawPath(path = glassPath, color = outlineColor, style = Stroke(width = 5f))
        drawRect(color = outlineColor, topLeft = Offset(w * 0.25f, h * 0.07f), size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.03f))
        drawRect(color = outlineColor, topLeft = Offset(w * 0.25f, h * 0.9f), size = androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.03f))

        clipPath(glassPath) {
            particles.forEach { p ->
                rotate(p.rotation, pivot = Offset(p.x * w, p.y * h)) {
                    drawText(
                        textMeasurer = textMeasurer,
                        text = "$",
                        topLeft = Offset(p.x * w - 10, p.y * h - 10),
                        style = TextStyle(
                            color = primaryColor.copy(alpha = 0.9f),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            shadow = Shadow(color = primaryColor.copy(alpha = 0.3f), blurRadius = 4f)
                        )
                    )
                }
            }
            
            if (isTracking || particles.isNotEmpty()) {
                val pileHeight = if (isTracking) (0.05f + (sin(animValue * 5f) * 0.01f)) else 0.05f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, h * 0.88f),
                        radius = w * 0.2f
                    ),
                    radius = w * 0.2f,
                    center = Offset(cx, h * (0.9f - pileHeight))
                )
            }
        }
    }
}

private class Particle(
    var x: Float,
    var y: Float,
    val speed: Float,
    var rotation: Float,
    val rotSpeed: Float,
    val drift: Float
)
