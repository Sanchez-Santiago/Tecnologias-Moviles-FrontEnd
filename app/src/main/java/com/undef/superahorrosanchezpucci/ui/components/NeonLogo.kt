package com.undef.superahorrosanchezpucci.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.undef.superahorrosanchezpucci.ui.theme.MiSuperTheme
import com.undef.superahorrosanchezpucci.ui.theme.ThemeMode

@Composable
fun NeonLogo(
    modifier: Modifier = Modifier,
    logoColor: Color = Color.White,
    glowColor: Color = Color(0xFF10B981), // Emerald500
) {
    Canvas(modifier = modifier) {
        val strokeWidthPx = 8.dp.toPx()
        val glowRadiusPx = 15.dp.toPx()

        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeCap = android.graphics.Paint.Cap.ROUND
                color = logoColor.toArgb()
                setShadowLayer(glowRadiusPx, 0f, 0f, glowColor.copy(alpha = 0.9f).toArgb())
            }

            // Outer cart path
            val path = android.graphics.Path().apply {
                // Top handle
                moveTo(size.width * 0.15f, size.height * 0.35f)
                lineTo(size.width * 0.25f, size.height * 0.35f)
                
                // Back slanted
                lineTo(size.width * 0.38f, size.height * 0.65f)
                
                // Bottom curve/line
                lineTo(size.width * 0.78f, size.height * 0.65f)
                
                // Front slanted up
                lineTo(size.width * 0.88f, size.height * 0.42f)
                
                // Top edge of the basket
                lineTo(size.width * 0.32f, size.height * 0.42f)
            }

            canvas.nativeCanvas.drawPath(path, paint)

            // Wheels
            val wheelY = size.height * 0.76f
            val wheelRadius = 8.dp.toPx()
            canvas.nativeCanvas.drawCircle(size.width * 0.42f, wheelY, wheelRadius, paint)
            canvas.nativeCanvas.drawCircle(size.width * 0.74f, wheelY, wheelRadius, paint)

            // Dollar Sign ($)
            val dollarPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = strokeWidthPx * 0.7f
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = 110.dp.toPx()
                color = logoColor.toArgb()
                setShadowLayer(glowRadiusPx, 0f, 0f, glowColor.copy(alpha = 0.9f).toArgb())
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            }
            
            // Draw $ sign
            canvas.nativeCanvas.drawText("$", size.width * 0.58f, size.height * 0.63f, dollarPaint)
        }
    }
}

@Composable
fun CheckeredBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 20.dp.toPx()
        val rows = (size.height / step).toInt() + 1
        val cols = (size.width / step).toInt() + 1
        val color1 = Color(0xFF1E293B).copy(alpha = 0.5f) // Slate 800
        val color2 = Color(0xFF0F172A).copy(alpha = 0.5f) // Slate 900
        
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (((r + c) % 2) == 0) {
                    drawRect(
                        color = color1,
                        topLeft = Offset(c * step, r * step),
                        size = Size(step, step)
                    )
                } else {
                    drawRect(
                        color = color2,
                        topLeft = Offset(c * step, r * step),
                        size = Size(step, step)
                    )
                }
            }
        }
    }
}

@Composable
fun NeonLogoScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)), // Slate 950
        contentAlignment = Alignment.Center
    ) {
        CheckeredBackground()
        NeonLogo(modifier = Modifier.size(300.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun NeonLogoPreview() {
    MiSuperTheme(themeMode = ThemeMode.DARK) {
        NeonLogoScreen()
    }
}
