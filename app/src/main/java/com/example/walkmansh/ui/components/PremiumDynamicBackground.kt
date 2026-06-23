package com.example.walkmansh.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun PremiumDynamicBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_blobs")

    // Blob 1: Animates across top-left to middle-right
    val blob1X by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1X"
    )
    val blob1Y by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(34000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob1Y"
    )

    // Blob 2: Animates across bottom-right to middle-left
    val blob2X by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(31000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2X"
    )
    val blob2Y by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob2Y"
    )

    // Blob 3: Drifts in center area
    val blob3X by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(39000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3X"
    )
    val blob3Y by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(29000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob3Y"
    )

    // Colors adjusted to align with Walkman Xperia Light and Dark themes
    val baseBgColor = if (isDark) Color(0xFF0C0E14) else Color(0xFFF3F4F6)
    val blobColor1 = if (isDark) Color(0xFF3B2F7E) else Color(0xFFE5E2FF) // Soft premium purple
    val blobColor2 = if (isDark) Color(0xFF134C4E) else Color(0xFFDDF9F8) // Soft premium teal
    val blobColor3 = if (isDark) Color(0xFF5E1B46) else Color(0xFFFDE4E6) // Soft premium pink

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.85f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(60.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            val w = size.width
            val h = size.height

            // Render Blob 1
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blobColor1, Color.Transparent),
                    center = Offset(blob1X * w, blob1Y * h),
                    radius = w * 0.8f
                ),
                center = Offset(blob1X * w, blob1Y * h),
                radius = w * 0.8f
            )

            // Render Blob 2
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blobColor2, Color.Transparent),
                    center = Offset(blob2X * w, blob2Y * h),
                    radius = w * 0.7f
                ),
                center = Offset(blob2X * w, blob2Y * h),
                radius = w * 0.7f
            )

            // Render Blob 3
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blobColor3, Color.Transparent),
                    center = Offset(blob3X * w, blob3Y * h),
                    radius = w * 0.75f
                ),
                center = Offset(blob3X * w, blob3Y * h),
                radius = w * 0.75f
            )
        }
    }
}
