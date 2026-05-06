package com.example.misuper.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(
    isLoading: Boolean = true,
    contentAfterLoading: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (isLoading) {
        content()
    } else {
        contentAfterLoading()
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    width: androidx.compose.ui.unit.Dp? = null
) {
    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.3f),
        Color.Gray.copy(alpha = 0.1f),
        Color.Gray.copy(alpha = 0.3f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translation"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    )
}

@Composable
fun ProductSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.size(24.dp), height = 24.dp)
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(height = 16.dp, width = 120.dp)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(height = 12.dp, width = 80.dp)
            }
            ShimmerBox(height = 18.dp, width = 60.dp)
        }
    }
}

@Composable
fun TicketSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBox(modifier = Modifier.size(64.dp), height = 64.dp)
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(height = 18.dp, width = 150.dp)
                Spacer(modifier = Modifier.height(4.dp))
                ShimmerBox(height = 12.dp, width = 100.dp)
            }
            ShimmerBox(height = 18.dp, width = 80.dp)
        }
    }
}

@Composable
fun BudgetSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(160.dp), height = 160.dp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShimmerBox(height = 12.dp, width = 80.dp)
            ShimmerBox(height = 24.dp, width = 100.dp)
            ShimmerBox(height = 10.dp, width = 120.dp)
        }
    }
}

@Composable
fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        // Mode selector skeleton
        ShimmerBox(height = 64.dp)
        Spacer(modifier = Modifier.height(16.dp))
        // Budget hero skeleton
        BudgetSkeleton()
        Spacer(modifier = Modifier.height(16.dp))
        // Members skeleton
        ShimmerBox(height = 80.dp)
        Spacer(modifier = Modifier.height(16.dp))
        // Accumulated savings skeleton
        ShimmerBox(height = 100.dp)
        Spacer(modifier = Modifier.height(16.dp))
        // AI tip skeleton
        ShimmerBox(height = 80.dp)
        Spacer(modifier = Modifier.height(16.dp))
        // Last purchases title
        ShimmerBox(height = 12.dp, width = 150.dp)
        repeat(3) {
            TicketSkeleton()
        }
    }
}

@Composable
fun ListaSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Search bar skeleton
        ShimmerBox(height = 56.dp)
        // Filters skeleton
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp)
                ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp)
                ShimmerBox(modifier = Modifier.weight(1f), height = 48.dp)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Products skeleton
        repeat(6) {
            ProductSkeleton()
        }
    }
}

@Composable
fun TicketsSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // Search bar skeleton
        ShimmerBox(height = 56.dp)
        Spacer(modifier = Modifier.height(8.dp))
        // Tickets skeleton
        repeat(4) {
            TicketSkeleton()
        }
    }
}
