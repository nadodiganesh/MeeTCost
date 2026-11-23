package com.nadodiganesh.meetcost.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoneySandglass(
    isTracking: Boolean,
    elapsedSeconds: Long,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF7844)
) {
    val frameColor = Color.White.copy(alpha = 0.60f)

    // Liquid sand levels exactly as in Lovable:
    // progress = (elapsedSeconds % 60) / 60f
    // n = isTracking ? (1 - progress) : 1f
    // r = 1 - n
    val progress = (elapsedSeconds % 60L) / 60f
    val n = if (isTracking) (1f - progress) else 1f
    val r = 1f - n

    val animatedN by animateFloatAsState(
        targetValue = n,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "sandTopRatio"
    )
    val animatedR by animateFloatAsState(
        targetValue = r,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "sandBotRatio"
    )

    // Infinite coin animation only active while tracking
    val coinTransition = rememberInfiniteTransition(label = "CoinFallTransition")
    val coinProgress by if (isTracking) {
        coinTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "CoinProgress"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    // Fixed 96x144 proportional canvas box
    Box(
        modifier = modifier.size(width = 96.dp, height = 144.dp),
        contentAlignment = Alignment.Center
    ) {
        // SVG Hourglass Canvas (Paths are remembered to avoid allocations during draws)
        var cachedSize by remember { mutableStateOf(Size.Zero) }
        var topClipPath by remember { mutableStateOf(Path()) }
        var botClipPath by remember { mutableStateOf(Path()) }
        var framePath by remember { mutableStateOf(Path()) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size != cachedSize) {
                cachedSize = size
                val sx = size.width / 96f
                val sy = size.height / 144f

                topClipPath = Path().apply {
                    moveTo(14f * sx, 10f * sy)
                    lineTo(82f * sx, 10f * sy)
                    cubicTo(82f * sx, 40f * sy, 52f * sx, 60f * sy, 50f * sx, 72f * sy)
                    lineTo(46f * sx, 72f * sy)
                    cubicTo(44f * sx, 60f * sy, 14f * sx, 40f * sy, 14f * sx, 10f * sy)
                    close()
                }

                botClipPath = Path().apply {
                    moveTo(46f * sx, 72f * sy)
                    lineTo(50f * sx, 72f * sy)
                    cubicTo(52f * sx, 84f * sy, 82f * sx, 104f * sy, 82f * sx, 134f * sy)
                    lineTo(14f * sx, 134f * sy)
                    cubicTo(14f * sx, 104f * sy, 44f * sx, 84f * sy, 46f * sx, 72f * sy)
                    close()
                }

                framePath = Path().apply {
                    moveTo(14f * sx, 10f * sy)
                    lineTo(82f * sx, 10f * sy)
                    cubicTo(82f * sx, 40f * sy, 52f * sx, 60f * sy, 50f * sx, 72f * sy)
                    cubicTo(52f * sx, 84f * sy, 82f * sx, 104f * sy, 82f * sx, 134f * sy)
                    lineTo(14f * sx, 134f * sy)
                    cubicTo(14f * sx, 104f * sy, 44f * sx, 84f * sy, 46f * sx, 72f * sy)
                    cubicTo(44f * sx, 60f * sy, 14f * sx, 40f * sy, 14f * sx, 10f * sy)
                    close()
                }
            }

            val sx = size.width / 96f
            val sy = size.height / 144f

            // Top Bulb Sand: rect y = 10 + 62 * (1 - n)
            val topY = (10f + 62f * (1f - animatedN)) * sy
            clipPath(topClipPath) {
                drawRect(
                    color = accentColor,
                    topLeft = Offset(0f, topY),
                    size = Size(size.width, (72f * sy) - topY)
                )
            }

            // Bottom Bulb Sand: rect y = 134 - 62 * max(r, 0.08)
            val botY = (134f - 62f * animatedR.coerceAtLeast(0.08f)) * sy
            clipPath(botClipPath) {
                drawRect(
                    color = accentColor,
                    topLeft = Offset(0f, botY),
                    size = Size(size.width, (134f * sy) - botY)
                )
            }

            // Glass Frame outline
            drawPath(
                path = framePath,
                color = frameColor,
                style = Stroke(width = 3f * sx)
            )

            // Top Cap (x: 6, y: 4, w: 84, h: 8, rx: 4)
            drawRoundRect(
                color = frameColor.copy(alpha = 0.80f),
                topLeft = Offset(6f * sx, 4f * sy),
                size = Size(84f * sx, 8f * sy),
                cornerRadius = CornerRadius(4f * sx, 4f * sy)
            )

            // Bottom Cap (x: 6, y: 132, w: 84, h: 8, rx: 4)
            drawRoundRect(
                color = frameColor.copy(alpha = 0.80f),
                topLeft = Offset(6f * sx, 132f * sy),
                size = Size(84f * sx, 8f * sy),
                cornerRadius = CornerRadius(4f * sx, 4f * sy)
            )
        }

        // Animated falling coins - cleanly rendered with Compose nodes (no Canvas text measurement overhead)
        if (isTracking) {
            val delays = floatArrayOf(0f, 0.35f, 0.7f)
            delays.forEach { delay ->
                val phase = (coinProgress + delay) % 1f
                val alpha = when {
                    phase < 0.15f -> (phase / 0.15f).coerceIn(0f, 1f)
                    phase > 0.85f -> ((1f - phase) / 0.15f).coerceIn(0f, 1f)
                    else -> 1f
                }
                val offsetY = 66.dp + (54.dp * phase)

                if (alpha > 0.05f) {
                    Box(
                        modifier = Modifier
                            .offset(y = offsetY - 72.dp) // center-relative offset
                            .size(12.dp)
                            .alpha(alpha)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 7.sp
                        )
                    }
                }
            }
        }
    }
}
