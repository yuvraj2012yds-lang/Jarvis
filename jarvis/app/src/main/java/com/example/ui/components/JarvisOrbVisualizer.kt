package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voice.JarvisListeningState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisOrbVisualizer(
    listeningState: JarvisListeningState,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioRmsDb: Float,
    wakePulseTriggered: Boolean,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Slow continuous outer ring rotation
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_ring"
    )

    // Inner counter-rotation (speeds up when thinking)
    val innerRotationSpeed = if (isProcessing) 2000 else 6000
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(innerRotationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_ring"
    )

    // Breathing pulse
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Shockwave pulse for wake word trigger
    val wakePulseAnim = remember { Animatable(0f) }
    LaunchedEffect(wakePulseTriggered) {
        if (wakePulseTriggered) {
            wakePulseAnim.snapTo(0f)
            wakePulseAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(700, easing = FastOutSlowInEasing)
            )
        }
    }

    // Determine state colors
    val coreColor = when {
        isProcessing -> Color(0xFF9D4EDD) // Electric Violet
        isSpeaking -> Color(0xFF007AFF) // Electric Blue
        listeningState == JarvisListeningState.WAKE_WORD_TRIGGERED || wakePulseTriggered -> Color(0xFF00F0FF) // Glowing Cyan
        listeningState == JarvisListeningState.RECORDING_COMMAND -> Color(0xFF00F59B) // Emerald Active
        else -> Color(0xFF00F0FF) // High Tech Cyan
    }

    val stateText = when {
        isProcessing -> "NEURAL CORE PROCESSING..."
        isSpeaking -> "VOCAL SYNTHESIZER ACTIVE"
        wakePulseTriggered || listeningState == JarvisListeningState.WAKE_WORD_TRIGGERED -> "WAKE WORD TRIGGERED // \"JARVIS\""
        listeningState == JarvisListeningState.RECORDING_COMMAND -> "CAPTURING DIRECTIVE..."
        listeningState == JarvisListeningState.WAITING_FOR_WAKE_WORD -> "SYSTEM ARMED // SAY \"JARVIS\""
        else -> "TAP ORB TO ACTIVATE"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOrbClick
                )
                .testTag("jarvis_orb_button"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.width * 0.38f

                // Dynamic expansion based on speech volume and breathing
                val voiceExpansion = (audioRmsDb * 1.8f).coerceIn(0f, 25f)
                val currentRadius = baseRadius * (if (isProcessing) 1.05f else breathingPulse) + voiceExpansion

                // 1. Wake word shockwave ring
                if (wakePulseAnim.value > 0f && wakePulseAnim.value < 1f) {
                    val shockwaveRadius = baseRadius + (wakePulseAnim.value * 50f)
                    val shockwaveAlpha = (1f - wakePulseAnim.value).coerceIn(0f, 1f)
                    drawCircle(
                        color = Color(0xFF00F0FF).copy(alpha = shockwaveAlpha * 0.6f),
                        radius = shockwaveRadius,
                        center = center,
                        style = Stroke(width = 3.dp.toPx() * (1f - wakePulseAnim.value))
                    )
                }

                // 2. Outermost holographic glowing halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = if (isSpeaking || isProcessing) 0.35f else 0.15f),
                            coreColor.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRadius * 1.55f
                    ),
                    radius = currentRadius * 1.55f,
                    center = center
                )

                // 3. Segmented outer HUD ring (rotating)
                rotate(outerRotation, pivot = center) {
                    // Dashed circle
                    drawCircle(
                        color = coreColor.copy(alpha = 0.5f),
                        radius = currentRadius * 1.15f,
                        center = center,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                        )
                    )

                    // 4 Quadrant Accent Arcs
                    for (i in 0 until 4) {
                        val startAngle = i * 90f + 10f
                        drawArc(
                            color = coreColor,
                            startAngle = startAngle,
                            sweepAngle = 30f,
                            useCenter = false,
                            topLeft = Offset(center.x - currentRadius * 1.25f, center.y - currentRadius * 1.25f),
                            size = androidx.compose.ui.geometry.Size(currentRadius * 2.5f, currentRadius * 2.5f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // 4. Inner counter-rotating ring with notch ticks
                rotate(innerRotation, pivot = center) {
                    drawCircle(
                        color = Color(0xFF64D2FF).copy(alpha = 0.6f),
                        radius = currentRadius * 0.88f,
                        center = center,
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    )

                    // Tick notches
                    val ticks = 12
                    for (t in 0 until ticks) {
                        val angle = Math.toRadians((t * (360f / ticks)).toDouble())
                        val r1 = currentRadius * 0.82f
                        val r2 = currentRadius * 0.94f
                        val start = Offset(
                            center.x + (r1 * cos(angle)).toFloat(),
                            center.y + (r1 * sin(angle)).toFloat()
                        )
                        val end = Offset(
                            center.x + (r2 * cos(angle)).toFloat(),
                            center.y + (r2 * sin(angle)).toFloat()
                        )
                        drawLine(
                            color = coreColor.copy(alpha = 0.8f),
                            start = start,
                            end = end,
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }

                // 5. Radiant Core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White,
                            coreColor,
                            coreColor.copy(alpha = 0.4f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = currentRadius * 0.65f
                    ),
                    radius = currentRadius * 0.65f,
                    center = center
                )

                // 6. Central AI Core Symbol (Arc Reactor Triangle)
                val triSize = 14.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, center.y - triSize * 0.8f)
                    lineTo(center.x + triSize, center.y + triSize * 0.7f)
                    lineTo(center.x - triSize, center.y + triSize * 0.7f)
                    close()
                }
                drawPath(path = path, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State indicator label
        Text(
            text = stateText,
            color = coreColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Futuristic audio waveform spectrum visualizer
        Row(
            modifier = Modifier.size(width = 140.dp, height = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barCount = 14
            for (i in 0 until barCount) {
                val factor = (sin(i.toDouble() * 0.8 + (audioRmsDb * 0.5)).toFloat().coerceAtLeast(0.15f))
                val barHeight = if (isSpeaking || listeningState == JarvisListeningState.RECORDING_COMMAND) {
                    (4.dp + (12.dp * (audioRmsDb / 10f).coerceIn(0.1f, 1f) * factor))
                } else if (isProcessing) {
                    (4.dp + (8.dp * sin((outerRotation / 15f + i).toDouble()).toFloat().coerceAtLeast(0.2f)))
                } else {
                    2.dp
                }
                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = barHeight)
                        .background(
                            color = coreColor.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}
