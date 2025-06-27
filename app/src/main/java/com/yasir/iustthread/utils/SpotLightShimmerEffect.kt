package com.yasir.iustthread.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

fun Modifier.spotlightShimmerEffect(
    overlayColor: Color = Color.Black.copy(alpha = 0.3f),
    revealColor: Color = Color.Transparent,
    durationMillis: Int = 1600,
    waveWidth: Dp = 500.dp
): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "SpotlightShimmer")
    val waveWidthPx = with(LocalDensity.current) { waveWidth.toPx() }

    val translateY by transition.animateFloat(
        initialValue = -waveWidthPx,
        targetValue = size.height.toFloat(), // End the wave completely below the container
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslateY"
    )

    val brush = Brush.verticalGradient(
        colors = listOf(
            overlayColor,
            revealColor,
            overlayColor
        ),
        // Animate the Y position of the gradient
        startY = translateY,
        endY = translateY + waveWidthPx
    )

    this
        .onSizeChanged { size = it }
        .drawWithContent {
            // 1. Draw the actual placeholder content first
            drawContent()

            // 2. Draw the dimming overlay with the transparent "spotlight" brush on top
            drawRect(brush = brush)
        }
}